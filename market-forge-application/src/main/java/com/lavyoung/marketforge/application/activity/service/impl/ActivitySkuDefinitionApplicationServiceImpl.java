package com.lavyoung.marketforge.application.activity.service.impl;

import com.lavyoung.marketforge.application.activity.model.ActivitySkuDefinitionResult;
import com.lavyoung.marketforge.application.activity.model.AdjustActivitySkuStockCommand;
import com.lavyoung.marketforge.application.activity.model.CreateActivitySkuDefinitionCommand;
import com.lavyoung.marketforge.application.activity.model.UpdateActivitySkuDefinitionCommand;
import com.lavyoung.marketforge.application.activity.service.IActivitySkuDefinitionApplicationService;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityDefinitionEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivitySkuDefinitionEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivitySkuStockAdjustmentEntity;
import com.lavyoung.marketforge.domain.activity.repository.IActivityCountDefinitionRepository;
import com.lavyoung.marketforge.domain.activity.repository.IActivityDefinitionRepository;
import com.lavyoung.marketforge.domain.activity.repository.IActivitySkuDefinitionRepository;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import com.lavyoung.marketforge.types.utils.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * 活动 SKU 运营应用服务默认实现。
 *
 * <p>本服务负责活动 SKU 创建、普通配置修改和库存调整的用例编排。应用层不直接执行
 * SQL，而是负责协调活动生命周期、次数配置引用、SKU 领域模型和仓储端口。</p>
 *
 * <p>所有写操作都必须锁定所属活动记录，并在同一个数据库事务中确认活动仍处于草稿
 * 状态。活动开放操作也会更新并锁定同一条活动记录，因此 SKU 配置修改与活动开放能够
 * 通过数据库行锁串行执行，避免活动开放后仍有配置写入。</p>
 *
 * <p>普通配置更新和库存调整使用 SKU 乐观锁版本防止运营人员的并发操作相互覆盖。
 * Repository 返回更新失败时，统一转换为稳定的业务冲突异常。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/27
 */
@Service
@RequiredArgsConstructor
public class ActivitySkuDefinitionApplicationServiceImpl implements IActivitySkuDefinitionApplicationService {

    private static final int SKU_ID_LENGTH = 18;
    private static final int ADJUSTMENT_ID_LENGTH = 18;

    private final IActivitySkuDefinitionRepository activitySkuDefinitionRepository;
    private final IActivityDefinitionRepository activityDefinitionRepository;
    private final IActivityCountDefinitionRepository activityCountDefinitionRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActivitySkuDefinitionResult create(CreateActivitySkuDefinitionCommand command) {
        CreateActivitySkuDefinitionCommand validCommand = Objects.requireNonNull(command, "command must not be null");
        ActivityDefinitionEntity activity = lockDraftActivity(validCommand.activityId());
        validateActivityCountRelation(activity, validCommand.activityCountId());

        Long sku = Long.valueOf(IdGenerator.nextNumericId(SKU_ID_LENGTH));
        ActivitySkuDefinitionEntity definition = createDefinition(sku, validCommand);
        activitySkuDefinitionRepository.create(definition);
        return toResult(queryRequired(sku));
    }

    @Override
    @Transactional(readOnly = true)
    public ActivitySkuDefinitionResult queryBySku(Long sku) {
        return toResult(queryRequired(sku));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActivitySkuDefinitionResult updateConfiguration(UpdateActivitySkuDefinitionCommand command) {
        UpdateActivitySkuDefinitionCommand validCommand = Objects.requireNonNull(command, "command must not be null");
        ActivitySkuDefinitionEntity current = queryRequired(validCommand.sku());
        ensureExpectedVersion(current, validCommand.expectedVersion());
        ActivityDefinitionEntity targetActivity = lockAffectedDraftActivities(current.activityId(), validCommand.activityId());
        validateActivityCountRelation(targetActivity, validCommand.activityCountId());

        ActivitySkuDefinitionEntity updated = updateDefinition(current, validCommand);
        if (!activitySkuDefinitionRepository.updateConfiguration(updated)) {
            throw conflict(current.sku(), validCommand.expectedVersion());
        }
        return toResult(queryRequired(current.sku()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActivitySkuDefinitionResult adjustStock(AdjustActivitySkuStockCommand command) {
        AdjustActivitySkuStockCommand validCommand = Objects.requireNonNull(command, "command must not be null");
        ActivitySkuDefinitionEntity current = queryRequired(validCommand.sku());
        ensureExpectedVersion(current, validCommand.expectedVersion());
        lockDraftActivity(current.activityId());

        Long adjustmentId = Long.valueOf(IdGenerator.nextNumericId(ADJUSTMENT_ID_LENGTH));
        ActivitySkuStockAdjustmentEntity adjustment = createAdjustment(adjustmentId, current, validCommand);
        if (!activitySkuDefinitionRepository.adjustStockAndRecord(adjustment)) {
            throw conflict(current.sku(), validCommand.expectedVersion());
        }
        return toResult(queryRequired(current.sku()));
    }

    /**
     * 根据创建命令构造 SKU 领域实体。
     *
     * <p>首次创建时库存总量和剩余库存必须相等，版本固定为 1。客户端不能直接指定
     * SKU 标识、剩余库存或版本，避免通过协议参数绕过领域约束。</p>
     *
     * @param sku     应用层生成的 SKU 业务标识
     * @param command 创建命令
     * @return 经过领域模型校验的 SKU 定义
     * @throws BusinessException 创建参数违反 SKU 领域约束时抛出
     */
    private ActivitySkuDefinitionEntity createDefinition(Long sku, CreateActivitySkuDefinitionCommand command) {
        try {
            return new ActivitySkuDefinitionEntity(sku, command.activityId(), command.activityCountId(),
                    command.stockCount(), command.stockCount(), command.creditAmount(), 1, command.operator(),
                    command.operator(), null, null);
        } catch (NullPointerException | IllegalArgumentException exception) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_SKU_CONFIG_INVALID, exception, sku);
        }
    }

    /**
     * 使用当前 SKU 快照和修改命令生成新的领域实体。
     *
     * <p>领域模型会保留库存、创建人和创建时间。若 SKU 已经发生库存消费，领域模型
     * 将阻止修改活动或次数配置，但仍允许调整未来订单使用的积分价格。</p>
     *
     * @param current 当前 SKU 定义
     * @param command 修改命令
     * @return 修改后的 SKU 定义
     * @throws BusinessException 参数非法或已使用 SKU 修改业务归属时抛出
     */
    private ActivitySkuDefinitionEntity updateDefinition(ActivitySkuDefinitionEntity current, UpdateActivitySkuDefinitionCommand command) {
        try {
            return current.updateConfiguration(command.activityId(), command.activityCountId(), command.creditAmount(), command.operator());
        } catch (IllegalStateException exception) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_SKU_RELATION_CHANGE_NOT_ALLOWED, exception, current.sku());
        } catch (NullPointerException | IllegalArgumentException exception) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_SKU_CONFIG_INVALID, exception, current.sku());
        }
    }

    /**
     * 根据当前库存快照生成库存调整领域实体。
     *
     * <p>领域模型负责计算调整后的总库存和剩余库存，并保证二者增加或减少相同数量，
     * 因此库存调整不会改变已经消费的库存数量。</p>
     *
     * @param adjustmentId 库存调整流水业务标识
     * @param current      当前 SKU 定义
     * @param command      库存调整命令
     * @return 包含调整前后库存快照的领域实体
     * @throws BusinessException 调整量为零、库存不足或计算溢出时抛出
     */
    private ActivitySkuStockAdjustmentEntity createAdjustment(Long adjustmentId, ActivitySkuDefinitionEntity current,
                                                              AdjustActivitySkuStockCommand command) {
        try {
            return ActivitySkuStockAdjustmentEntity.create(adjustmentId, current, command.quantityDelta(),
                    command.reason(), command.operator());
        } catch (NullPointerException | IllegalArgumentException | ArithmeticException exception) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_SKU_STOCK_ADJUSTMENT_FAILED, exception, current.sku());
        }
    }

    /**
     * 按确定顺序锁定 SKU 修改涉及的活动。
     *
     * <p>SKU 从一个活动迁移到另一个活动时，必须同时锁定原活动和目标活动。两个活动
     * 按业务标识从小到大加锁，避免并发迁移请求以相反顺序获取锁而形成数据库死锁。</p>
     *
     * @param currentActivityId 当前所属活动标识
     * @param targetActivityId  修改后的目标活动标识
     * @return 已锁定且处于草稿状态的目标活动
     * @throws BusinessException 活动不存在或不是草稿状态时抛出
     */
    private ActivityDefinitionEntity lockAffectedDraftActivities(Long currentActivityId, Long targetActivityId) {
        if (currentActivityId.equals(targetActivityId)) {
            return lockDraftActivity(currentActivityId);
        }
        if (currentActivityId.compareTo(targetActivityId) < 0) {
            lockDraftActivity(currentActivityId);
            return lockDraftActivity(targetActivityId);
        }
        ActivityDefinitionEntity targetActivity = lockDraftActivity(targetActivityId);
        lockDraftActivity(currentActivityId);
        return targetActivity;
    }

    /**
     * 使用数据库排他锁读取活动并确认活动仍允许编辑。
     *
     * <p>该方法必须在带事务的写用例中调用。若脱离事务，SQL 执行结束后行锁会立即
     * 释放，无法与活动开放操作形成有效的并发互斥。</p>
     *
     * @param activityId 活动业务标识
     * @return 已经锁定且处于草稿状态的活动定义
     * @throws BusinessException 活动不存在或当前状态不允许修改 SKU 时抛出
     */
    private ActivityDefinitionEntity lockDraftActivity(Long activityId) {
        Long validActivityId = Objects.requireNonNull(activityId, "activityId must not be null");
        ActivityDefinitionEntity activity = activityDefinitionRepository.lockByActivityId(validActivityId)
                .orElseThrow(() -> BusinessException.of(BusinessResponseCode.LOTTERY_ACTIVITY_NOT_FOUND, validActivityId));
        if (!activity.canEdit()) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_SKU_ACTIVITY_STATE_ERROR,
                    activity.activityId(), activity.state().getCode());
        }
        return activity;
    }

    /**
     * 校验 SKU 次数配置与所属活动保持一致。
     *
     * <p>活动运行时使用活动本身的次数配置，SKU 兑换入账则读取 SKU 上的次数配置。
     * 两者如果不一致，同一个活动可能在抽奖和积分兑换场景下获得不同额度。</p>
     *
     * @param activity        SKU 所属活动
     * @param activityCountId SKU 请求配置的次数标识
     * @throws BusinessException 次数配置不一致或配置不存在时抛出
     */
    private void validateActivityCountRelation(ActivityDefinitionEntity activity, Long activityCountId) {
        Long validActivityCountId = Objects.requireNonNull(activityCountId, "activityCountId must not be null");
        if (!activity.activityCountId().equals(validActivityCountId)) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_SKU_ACTIVITY_COUNT_MISMATCH,
                    activity.activityId(), activity.activityCountId(), validActivityCountId);
        }
        activityCountDefinitionRepository.queryByActivityCountId(validActivityCountId)
                .orElseThrow(() -> BusinessException.of(BusinessResponseCode.ACTIVITY_COUNT_NOT_FOUND, validActivityCountId));
    }

    private ActivitySkuDefinitionEntity queryRequired(Long sku) {
        Long validSku = Objects.requireNonNull(sku, "sku must not be null");
        return activitySkuDefinitionRepository.queryBySku(validSku)
                .orElseThrow(() -> BusinessException.of(BusinessResponseCode.ACTIVITY_SKU_NOT_FOUND, validSku));
    }

    private void ensureExpectedVersion(ActivitySkuDefinitionEntity current, int expectedVersion) {
        if (current.version() != expectedVersion) {
            throw conflict(current.sku(), expectedVersion);
        }
    }

    private BusinessException conflict(Long sku, int expectedVersion) {
        return BusinessException.of(BusinessResponseCode.ACTIVITY_SKU_DEFINITION_CONFLICT, sku, expectedVersion);
    }

    private ActivitySkuDefinitionResult toResult(ActivitySkuDefinitionEntity entity) {
        return new ActivitySkuDefinitionResult(entity.sku(), entity.activityId(), entity.activityCountId(),
                entity.stockCount(), entity.stockCountSurplus(), entity.stockCount() - entity.stockCountSurplus(),
                entity.creditAmount(), entity.version(), entity.createdBy(), entity.updatedBy(),
                entity.createTime(), entity.updateTime());
    }
}
