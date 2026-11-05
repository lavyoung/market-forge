package com.lavyoung.marketforge.application.activity.service.impl;

import com.lavyoung.marketforge.application.activity.model.ActivityDefinitionResult;
import com.lavyoung.marketforge.application.activity.model.ChangeActivityDefinitionStateCommand;
import com.lavyoung.marketforge.application.activity.model.CreateActivityDefinitionCommand;
import com.lavyoung.marketforge.application.activity.model.UpdateActivityDefinitionCommand;
import com.lavyoung.marketforge.application.activity.service.IActivityDefinitionApplicationService;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityDefinitionEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityDefinitionStateChange;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivityDefinitionStateVO;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivityReadinessReportVO;
import com.lavyoung.marketforge.domain.activity.model.vo.AwardUnlockScopeVO;
import com.lavyoung.marketforge.domain.activity.repository.IActivityCountDefinitionRepository;
import com.lavyoung.marketforge.domain.activity.repository.IActivityDefinitionRepository;
import com.lavyoung.marketforge.domain.activity.service.IActivityDefinitionReadinessChecker;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyDefinitionRepository;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import com.lavyoung.marketforge.types.utils.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 活动定义运营应用服务。
 *
 * <p>负责活动草稿创建、修改和生命周期流转。创建和修改草稿时，该服务会检查
 * 活动引用的次数配置和抽奖策略是否真实存在，避免把悬空的外部业务标识写入
 * 活动定义。</p>
 *
 * <p>草稿阶段只检查引用对象是否存在，不要求策略已经生效，也不要求活动已经
 * 配置 SKU。运营人员可以先建立活动草稿，再继续完成策略发布和 SKU 配置。
 * 当活动从草稿变为开放状态时，后续活动就绪检查将执行更严格的完整性校验。</p>
 *
 * <p>该服务只编排活动级配置。额度商品库存由活动 SKU 应用服务负责，奖品库存
 * 由策略奖品及库存同步链路负责。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/28
 */
@Service
@RequiredArgsConstructor
public class ActivityDefinitionApplicationServiceImpl implements IActivityDefinitionApplicationService {
    private static final int ACTIVITY_ID_LENGTH = 18;

    private final IActivityDefinitionRepository activityDefinitionRepository;
    private final IActivityCountDefinitionRepository activityCountDefinitionRepository;
    private final IStrategyDefinitionRepository strategyDefinitionRepository;
    private final IActivityDefinitionReadinessChecker activityDefinitionReadinessChecker;
    private final Clock clock;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActivityDefinitionResult create(CreateActivityDefinitionCommand command) {
        CreateActivityDefinitionCommand validCommand = Objects.requireNonNull(command, "command must not be null");

        validateReferencesExist(validCommand.activityCountId(), validCommand.strategyId());
        Long activityId = Long.valueOf(IdGenerator.nextNumericId(ACTIVITY_ID_LENGTH));

        ActivityDefinitionEntity definition = createDefinition(activityId, validCommand);
        activityDefinitionRepository.create(definition);
        return toResult(queryRequired(activityId));
    }

    @Override
    @Transactional(readOnly = true)
    public ActivityDefinitionResult queryByActivityId(Long activityId) {
        return toResult(queryRequired(activityId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActivityDefinitionResult updateDraft(UpdateActivityDefinitionCommand command) {
        UpdateActivityDefinitionCommand validCommand = Objects.requireNonNull(command, "command must not be null");
        ActivityDefinitionEntity current = queryRequired(validCommand.activityId());
        if (!current.canEdit()) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_DEFINITION_STATE_ERROR,
                    current.activityId(), current.state().getCode());
        }
        ensureExpectedVersion(current, validCommand.expectedVersion());
        validateReferencesExist(command.activityCountId(), command.strategyId());

        ActivityDefinitionEntity updated = updateDefinition(current, validCommand);
        if (!activityDefinitionRepository.updateDraft(updated)) {
            throw conflict(current.activityId(), validCommand.expectedVersion());
        }
        return toResult(queryRequired(current.activityId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActivityDefinitionResult changeState(ChangeActivityDefinitionStateCommand command) {
        ChangeActivityDefinitionStateCommand validCommand = Objects.requireNonNull(command, "command must not be null");
        ActivityDefinitionStateVO targetState = ActivityDefinitionStateVO.of(validCommand.targetState())
                .orElseThrow(() -> BusinessException.of(BusinessResponseCode.ACTIVITY_DEFINITION_STATE_ERROR,
                        validCommand.targetState()));
        ActivityDefinitionEntity current = targetState == ActivityDefinitionStateVO.OPEN
                ? lockRequired(validCommand.activityId())
                : queryRequired(validCommand.activityId());
        ensureExpectedVersion(current, validCommand.expectedVersion());

        if (!current.state().canTransitionTo(targetState)) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_DEFINITION_STATE_ERROR,
                    current.activityId(), current.state().getCode(), targetState.getCode());
        }
        if (targetState == ActivityDefinitionStateVO.OPEN) {
            ensureReadyToOpen(current);
        }
        ActivityDefinitionStateChange stateChange = new ActivityDefinitionStateChange(current.activityId(),
                current.state(), targetState, validCommand.expectedVersion(), validCommand.operator());

        if (!activityDefinitionRepository.changeState(stateChange)) {
            throw conflict(current.activityId(), validCommand.expectedVersion());
        }
        return toResult(queryRequired(current.activityId()));
    }

    /**
     * 在活动状态变更事务内确认活动满足开放条件。
     *
     * <p>先锁定活动引用的次数配置，再调用领域就绪检查器读取 SKU、策略及概率规则。
     * 活动行已经由 {@link #lockRequired(Long)} 锁定，因此并发的 SKU 修改会等待本次
     * 开放事务结束；次数配置行锁则避免检查期间发生额度配置变更。</p>
     *
     * <p>就绪报告中的警告和建议不会阻止开放，只有错误集合非空时才抛出稳定的
     * 业务异常。异常参数保留错误数量和首个问题编码，便于服务端日志定位，完整的
     * 问题列表仍通过独立的就绪检查接口提供给运营页面。</p>
     *
     * @param current 已加排他锁的草稿活动定义
     * @throws BusinessException 活动次数配置不存在或活动存在阻断错误时抛出
     * @throws RuntimeException  就绪检查依赖的数据库或外部资源不可用时抛出
     */
    private void ensureReadyToOpen(ActivityDefinitionEntity current) {
        activityCountDefinitionRepository.lockByActivityCountId(current.activityCountId())
                .orElseThrow(() -> BusinessException.of(BusinessResponseCode.ACTIVITY_COUNT_NOT_FOUND,
                        current.activityCountId()));

        ActivityReadinessReportVO report = activityDefinitionReadinessChecker.check(current, LocalDateTime.now(clock));
        if (!report.ready()) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_DEFINITION_NOT_READY,
                    current.activityId(), report.errors().size(), report.errors().get(0).code());
        }
    }

    /**
     * 校验活动草稿引用的次数配置和策略是否存在。
     *
     * <p>该校验用于尽早阻止悬空引用进入数据库。这里不检查策略是否已经生效，
     * 因为草稿活动允许引用仍在配置过程中的策略。策略状态、策略奖品、活动 SKU
     * 和概率空间将在活动开放前由就绪检查统一验证。</p>
     *
     * <p>这属于跨聚合存在性校验，需要访问 Repository，因此由 Application
     * Service 负责，不放入只描述活动自身不变量的
     * {@link ActivityDefinitionEntity}。</p>
     *
     * @param activityCountId 活动次数配置标识
     * @param strategyId      抽奖策略标识
     * @throws BusinessException 次数配置或抽奖策略不存在时抛出
     */
    private void validateReferencesExist(Long activityCountId, Long strategyId) {
        activityCountDefinitionRepository.queryByActivityCountId(activityCountId)
                .orElseThrow(() -> BusinessException.of(BusinessResponseCode.ACTIVITY_COUNT_NOT_FOUND, activityCountId));
        strategyDefinitionRepository.queryByStrategyId(strategyId)
                .orElseThrow(() -> BusinessException.of(BusinessResponseCode.STRATEGY_NOT_FOUND, strategyId));
    }

    /**
     * 根据创建命令构造活动草稿实体。
     *
     * @param activityId 新生成的活动业务标识
     * @param command    已通过引用检查的创建命令
     * @return 初始状态为草稿的活动定义
     * @throws BusinessException 活动自身字段不符合领域约束时抛出
     */
    private ActivityDefinitionEntity createDefinition(Long activityId, CreateActivityDefinitionCommand command) {
        try {
            AwardUnlockScopeVO scope = parseScope(command.awardUnlockScope());
            return new ActivityDefinitionEntity(activityId, command.activityName(), command.activityDesc(),
                    command.beginDateTime(), command.endDateTime(), command.activityCountId(), command.strategyId(),
                    scope, ActivityDefinitionStateVO.DRAFT, 1, command.operator(), command.operator(), null, null);
        } catch (NullPointerException | IllegalArgumentException exception) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_CONFIG_INVALID, exception, command.activityName());
        }
    }

    /**
     * 根据修改命令构造新的活动草稿实体。
     *
     * @param current 当前活动定义
     * @param command 活动修改命令
     * @return 保留活动标识和创建信息的新活动定义
     * @throws BusinessException 修改后的活动字段不符合领域约束时抛出
     */
    private ActivityDefinitionEntity updateDefinition(ActivityDefinitionEntity current, UpdateActivityDefinitionCommand command) {
        try {
            return new ActivityDefinitionEntity(current.activityId(), command.activityName(), command.activityDesc(),
                    command.beginDateTime(), command.endDateTime(), command.activityCountId(), command.strategyId(),
                    parseScope(command.awardUnlockScope()), current.state(), command.expectedVersion(),
                    current.createdBy(), command.operator(), current.createTime(), current.updateTime());
        } catch (NullPointerException | IllegalArgumentException exception) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_CONFIG_INVALID, exception, current.activityId());
        }
    }

    /**
     * 将协议层传入的奖品解锁范围转换为领域值对象。
     *
     * @param code 奖品解锁范围编码
     * @return 奖品解锁范围值对象
     * @throws IllegalArgumentException 编码为空或不受支持时抛出
     */
    private AwardUnlockScopeVO parseScope(String code) {
        return AwardUnlockScopeVO.fromCode(code)
                .orElseThrow(() -> new IllegalArgumentException("unknown award unlock scope: " + code));
    }

    /**
     * 查询必然存在的活动定义。
     *
     * @param activityId 活动业务标识
     * @return 活动定义
     * @throws NullPointerException 活动标识为空时抛出
     * @throws BusinessException    活动不存在时抛出
     */
    private ActivityDefinitionEntity queryRequired(Long activityId) {
        Long validActivityId = Objects.requireNonNull(activityId, "activityId must not be null");
        return activityDefinitionRepository.queryByActivityId(validActivityId)
                .orElseThrow(() -> BusinessException.of(BusinessResponseCode.LOTTERY_ACTIVITY_NOT_FOUND, validActivityId));
    }

    /**
     * 使用排他锁查询准备开放的活动定义。
     *
     * <p>开放活动和修改活动 SKU 都锁定同一条活动记录，使“检查关联配置并开放”与
     * “修改关联配置”串行执行，避免活动通过检查后、状态更新前被并发改坏。</p>
     *
     * @param activityId 活动业务标识
     * @return 已在当前事务中加排他锁的活动定义
     * @throws NullPointerException 活动标识为空时抛出
     * @throws BusinessException    活动不存在时抛出
     */
    private ActivityDefinitionEntity lockRequired(Long activityId) {
        Long validActivityId = Objects.requireNonNull(activityId, "activityId must not be null");
        return activityDefinitionRepository.lockByActivityId(validActivityId)
                .orElseThrow(() -> BusinessException.of(BusinessResponseCode.LOTTERY_ACTIVITY_NOT_FOUND, validActivityId));
    }

    /**
     * 校验页面提交的乐观锁版本。
     *
     * @param current         当前活动定义
     * @param expectedVersion 页面读取到的期望版本
     * @throws BusinessException 版本已经发生变化时抛出
     */
    private void ensureExpectedVersion(ActivityDefinitionEntity current, int expectedVersion) {
        if (current.version() != expectedVersion) {
            throw conflict(current.activityId(), expectedVersion);
        }
    }

    /**
     * 创建活动定义并发冲突异常。
     *
     * @param activityId      活动业务标识
     * @param expectedVersion 页面读取到的期望版本
     * @return 稳定的活动定义并发冲突异常
     */
    private BusinessException conflict(Long activityId, int expectedVersion) {
        return BusinessException.of(BusinessResponseCode.ACTIVITY_DEFINITION_CONFLICT, activityId, expectedVersion);
    }

    /**
     * 将活动领域实体转换为应用查询结果。
     *
     * @param entity 活动领域实体
     * @return 不向外层暴露领域类型的应用结果
     */
    private ActivityDefinitionResult toResult(ActivityDefinitionEntity entity) {
        return new ActivityDefinitionResult(entity.activityId(), entity.activityName(), entity.activityDesc(),
                entity.beginDateTime(), entity.endDateTime(), entity.activityCountId(), entity.strategyId(),
                entity.awardUnlockScope().getCode(), entity.state().getCode(), entity.version(), entity.createdBy(),
                entity.updatedBy(), entity.createTime(), entity.updateTime());
    }
}
