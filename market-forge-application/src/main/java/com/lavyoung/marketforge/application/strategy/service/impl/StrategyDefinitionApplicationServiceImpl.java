package com.lavyoung.marketforge.application.strategy.service.impl;

import com.lavyoung.marketforge.application.strategy.model.*;
import com.lavyoung.marketforge.application.strategy.service.IStrategyDefinitionApplicationService;
import com.lavyoung.marketforge.domain.strategy.model.aggregate.StrategyDefinitionAggregate;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyAwardEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyDefinitionStateChange;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyRuleEntity;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyDefinitionStateVO;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyDefinitionRepository;
import com.lavyoung.marketforge.domain.strategy.service.IStrategyDefinitionPublicationValidator;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.IStrategyArmory;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import com.lavyoung.marketforge.types.utils.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * 策略定义运营应用服务默认实现。
 *
 * <p>负责创建策略草稿、查询完整聚合、修改草稿、复制历史策略以及编排发布和停用。
 * 本类不处理 HTTP、Session、MyBatis PO 或 Redis 数据结构。</p>
 *
 * <p>策略发布按照“读取最新聚合、校验版本、校验生命周期、校验发布配置、
 * 预热运行时快照、短事务更新状态”的顺序执行。只有概率快照成功生成后，
 * 策略才会进入已生效状态。</p>
 *
 * <p>命令到领域聚合以及领域聚合到应用结果的转换仅服务当前用例，因此保留为
 * 有明确业务命名的私有方法，不额外引入只做字段复制的 Application Assembler。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
@Service
@RequiredArgsConstructor
public class StrategyDefinitionApplicationServiceImpl implements IStrategyDefinitionApplicationService {

    private static final int STRATEGY_ID_LENGTH = 18;

    private final IStrategyDefinitionRepository strategyDefinitionRepository;
    private final IStrategyDefinitionPublicationValidator publicationValidator;
    private final IStrategyArmory strategyArmory;
    private final StrategyDefinitionStateTransaction stateTransaction;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StrategyDefinitionResult create(CreateStrategyDefinitionCommand command) {
        CreateStrategyDefinitionCommand validCommand = Objects.requireNonNull(command, "command must not be null");
        Long strategyId = Long.valueOf(IdGenerator.nextNumericId(STRATEGY_ID_LENGTH));
        StrategyDefinitionAggregate aggregate = createAggregate(strategyId, validCommand);
        strategyDefinitionRepository.create(aggregate);
        return toResult(queryRequired(strategyId));
    }

    @Override
    @Transactional(readOnly = true)
    public StrategyDefinitionResult queryByStrategyId(Long strategyId) {
        return toResult(queryRequired(strategyId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StrategyDefinitionResult updateDraft(UpdateStrategyDefinitionCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        StrategyDefinitionAggregate current = queryRequired(command.strategyId());
        if (!current.canEdit()) {
            throw BusinessException.of(BusinessResponseCode.STRATEGY_STATE_ERROR,
                    current.strategyId(),
                    current.state().getCode()
            );
        }
        ensureExpectedVersion(current, command.expectedVersion());
        StrategyDefinitionAggregate updated = updateAggregate(current, command);
        if (!strategyDefinitionRepository.updateDraft(updated)) {
            throw conflict(command.strategyId(), command.expectedVersion());
        }
        return toResult(queryRequired(current.strategyId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StrategyDefinitionResult copyAsDraft(CopyStrategyDefinitionCommand command) {
        CopyStrategyDefinitionCommand validCommand = Objects.requireNonNull(command, "command must not be null");
        StrategyDefinitionAggregate source = queryRequired(validCommand.sourceStrategyId());
        Long newStrategyId = Long.valueOf(IdGenerator.nextNumericId(STRATEGY_ID_LENGTH));
        try {
            StrategyDefinitionAggregate copied = source.copyAsDraft(newStrategyId, validCommand.operator());
            strategyDefinitionRepository.create(copied);
            return toResult(queryRequired(newStrategyId));
        } catch (NullPointerException | IllegalArgumentException | IllegalStateException exception) {
            throw BusinessException.of(BusinessResponseCode.STRATEGY_CONFIG_INVALID,
                    exception,
                    source.strategyId(),
                    newStrategyId
            );
        }
    }

    @Override
    public StrategyDefinitionResult changeState(ChangeStrategyDefinitionStateCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        StrategyDefinitionAggregate current = queryRequired(command.strategyId());
        ensureExpectedVersion(current, command.expectedVersion());
        StrategyDefinitionStateVO targetState = parseState(command.targetState());
        if (!current.state().canTransitionTo(targetState)) {
            throw BusinessException.of(BusinessResponseCode.STRATEGY_STATE_ERROR,
                    current.strategyId(),
                    current.state().getCode(),
                    targetState.getCode()
            );
        }
        if (targetState == StrategyDefinitionStateVO.ACTIVE) {
            prepareRuntimeStrategy(current);
        }
        StrategyDefinitionStateChange stateChange = new StrategyDefinitionStateChange(
                current.strategyId(),
                current.state(),
                targetState,
                command.expectedVersion(),
                command.operator()
        );
        stateTransaction.changeState(stateChange);
        return toResult(queryRequired(current.strategyId()));
    }

    /**
     * 创建初始草稿策略聚合。
     *
     * <p>策略标识、初始状态和版本由服务端控制。子配置的策略标识统一使用新生成的
     * {@code strategyId}，剩余库存初始化为库存总量。</p>
     *
     * @param strategyId 服务端生成的策略业务标识
     * @param command    创建策略命令
     * @return 初始版本为 1 的草稿策略聚合
     * @throws BusinessException 命令内容无法建立合法聚合时抛出
     */
    private StrategyDefinitionAggregate createAggregate(Long strategyId, CreateStrategyDefinitionCommand command) {
        try {
            return new StrategyDefinitionAggregate(
                    strategyId,
                    command.strategyDesc(),
                    command.ruleModels(),
                    StrategyDefinitionStateVO.DRAFT,
                    1,
                    command.operator(),
                    command.operator(),
                    null,
                    null,
                    toAwardEntities(strategyId, command.awards()),
                    toRuleEntities(strategyId, command.rules())
            );
        } catch (NullPointerException | IllegalArgumentException exception) {
            throw BusinessException.of(BusinessResponseCode.STRATEGY_CONFIG_INVALID, exception,
                    strategyId,
                    command.strategyDesc()
            );
        }
    }


    /**
     * 根据修改命令重建完整草稿聚合。
     *
     * <p>更新时保留原始创建人和创建时间，使用调用方持有的版本作为数据库乐观锁条件。
     * 草稿尚未进入运行时抽奖，因此重新提交奖品配置时，剩余库存重置为库存总量。</p>
     *
     * @param current 当前数据库策略定义
     * @param command 修改草稿命令
     * @return 包含最新根配置和完整子配置的草稿聚合
     * @throws BusinessException 命令内容无法建立合法聚合时抛出
     */
    private StrategyDefinitionAggregate updateAggregate(StrategyDefinitionAggregate current, UpdateStrategyDefinitionCommand command) {
        try {
            return new StrategyDefinitionAggregate(
                    current.strategyId(),
                    command.strategyDesc(),
                    command.ruleModels(),
                    current.state(),
                    command.expectedVersion(),
                    current.createdBy(),
                    command.operator(),
                    current.createTime(),
                    current.updateTime(),
                    toAwardEntities(current.strategyId(), command.awards()),
                    toRuleEntities(current.strategyId(), command.rules())
            );
        } catch (NullPointerException | IllegalArgumentException exception) {
            throw BusinessException.of(BusinessResponseCode.STRATEGY_CONFIG_INVALID, exception, current.strategyId());
        }
    }

    /**
     * 将奖品命令项转换为策略奖品领域实体。
     *
     * <p>命令不允许指定策略标识和剩余库存。所有子实体统一归属于父策略，
     * 剩余库存由库存总量初始化。</p>
     *
     * @param strategyId 父策略业务标识
     * @param awards     奖品配置命令集合
     * @return 策略奖品领域实体集合
     * @throws NullPointerException 集合或集合元素为空时抛出
     */
    private List<StrategyAwardEntity> toAwardEntities(Long strategyId, List<StrategyAwardConfigCommand> awards) {
        return Objects.requireNonNull(awards, "awards must not be null").stream()
                .map(award -> {
                    Objects.requireNonNull(award, "award must not be null");
                    return new StrategyAwardEntity(
                            strategyId,
                            award.awardId(),
                            award.awardTitle(),
                            award.awardSubtitle(),
                            award.awardCount(),
                            award.awardCount(),
                            award.awardRate(),
                            award.ruleModels(),
                            award.sort(),
                            null,
                            null
                    );
                })
                .toList();
    }

    /**
     * 将规则命令项转换为策略规则领域实体。
     *
     * @param strategyId 父策略业务标识
     * @param rules      规则配置命令集合
     * @return 策略规则领域实体集合
     * @throws NullPointerException 集合或集合元素为空时抛出
     */
    private List<StrategyRuleEntity> toRuleEntities(Long strategyId, List<StrategyRuleConfigCommand> rules) {
        return Objects.requireNonNull(rules, "rules must not be null").stream()
                .map(rule -> {
                    Objects.requireNonNull(rule, "rule must not be null");
                    return new StrategyRuleEntity(
                            strategyId,
                            rule.awardId(),
                            rule.ruleType(),
                            rule.ruleModel(),
                            rule.ruleValue(),
                            rule.ruleDesc(),
                            null,
                            null
                    );
                })
                .toList();
    }

    /**
     * 校验策略配置并生成运行时概率快照。
     *
     * <p>发布就绪校验必须在预热之前执行，防止非法配置污染 Redis。
     * 装配返回 {@code false} 时不允许继续更新数据库状态。</p>
     *
     * @param aggregate 待发布策略定义
     * @throws BusinessException 配置校验失败或概率快照预热失败时抛出
     */
    private void prepareRuntimeStrategy(StrategyDefinitionAggregate aggregate) {
        publicationValidator.validate(aggregate);
        boolean assembled = strategyArmory.assembleLotteryStrategy(aggregate.strategyId());
        if (!assembled) {
            throw BusinessException.of(BusinessResponseCode.STRATEGY_ASSEMBLY_FAILED, aggregate.strategyId());
        }
    }

    /**
     * 查询必须存在的完整策略定义。
     *
     * @param strategyId 策略业务标识
     * @return 完整策略定义聚合
     * @throws NullPointerException 策略标识为空时抛出
     * @throws BusinessException    策略不存在时抛出
     */
    private StrategyDefinitionAggregate queryRequired(Long strategyId) {
        Objects.requireNonNull(strategyId, "strategyId must not be null");
        return strategyDefinitionRepository.queryByStrategyId(strategyId)
                .orElseThrow(() -> BusinessException.of(BusinessResponseCode.STRATEGY_NOT_FOUND, strategyId));
    }

    /**
     * 校验调用方持有的乐观锁版本。
     *
     * <p>应用层检查用于尽早返回明确错误，数据库条件更新仍是最终并发防线。</p>
     *
     * @param current         当前数据库策略定义
     * @param expectedVersion 调用方持有的版本
     * @throws BusinessException 版本不一致时抛出
     */
    private void ensureExpectedVersion(StrategyDefinitionAggregate current, int expectedVersion) {
        if (current.version() != expectedVersion) {
            throw conflict(current.strategyId(), expectedVersion);
        }
    }

    /**
     * 解析策略生命周期状态。
     *
     * @param stateCode 目标状态编码
     * @return 策略生命周期状态
     * @throws BusinessException 状态编码为空或无法识别时抛出
     */
    private StrategyDefinitionStateVO parseState(String stateCode) {
        return StrategyDefinitionStateVO.of(stateCode)
                .orElseThrow(() -> BusinessException.of(BusinessResponseCode.STRATEGY_STATE_ERROR, stateCode));
    }

    /**
     * 创建策略并发冲突异常。
     *
     * @param strategyId      策略业务标识
     * @param expectedVersion 调用方持有的版本
     * @return 策略定义并发冲突异常
     */
    private BusinessException conflict(Long strategyId, int expectedVersion) {
        return BusinessException.of(BusinessResponseCode.STRATEGY_DEFINITION_CONFLICT, strategyId, expectedVersion);
    }

    /**
     * 将领域聚合转换为应用结果。
     *
     * @param aggregate 完整策略定义聚合
     * @return 不暴露领域对象的应用结果
     */
    private StrategyDefinitionResult toResult(StrategyDefinitionAggregate aggregate) {
        List<StrategyAwardConfigResult> awards = aggregate.awards().stream()
                .map(this::toAwardResult)
                .toList();

        List<StrategyRuleConfigResult> rules = aggregate.rules().stream()
                .map(this::toRuleResult)
                .toList();

        return new StrategyDefinitionResult(
                aggregate.strategyId(),
                aggregate.strategyDesc(),
                aggregate.ruleModels(),
                aggregate.state().getCode(),
                aggregate.version(),
                aggregate.createdBy(),
                aggregate.updatedBy(),
                aggregate.createTime(),
                aggregate.updateTime(),
                awards,
                rules
        );
    }

    /**
     * 将策略奖品领域实体转换为应用结果。
     *
     * @param award 策略奖品领域实体
     * @return 策略奖品应用结果
     */
    private StrategyAwardConfigResult toAwardResult(StrategyAwardEntity award) {
        return new StrategyAwardConfigResult(
                award.awardId(),
                award.awardTitle(),
                award.awardSubtitle(),
                award.awardCount(),
                award.awardCountSurplus(),
                award.awardRate(),
                award.ruleModels(),
                award.sort(),
                award.createTime(),
                award.updateTime()
        );
    }

    /**
     * 将策略规则领域实体转换为应用结果。
     *
     * @param rule 策略规则领域实体
     * @return 策略规则应用结果
     */
    private StrategyRuleConfigResult toRuleResult(StrategyRuleEntity rule) {
        return new StrategyRuleConfigResult(
                rule.awardId(),
                rule.ruleType(),
                rule.ruleModel(),
                rule.ruleValue(),
                rule.ruleDesc(),
                rule.createTime(),
                rule.updateTime()
        );
    }
}
