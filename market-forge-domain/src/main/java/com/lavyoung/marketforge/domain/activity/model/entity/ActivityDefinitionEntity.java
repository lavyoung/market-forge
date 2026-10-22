package com.lavyoung.marketforge.domain.activity.model.entity;

import com.lavyoung.marketforge.domain.activity.model.vo.ActivityDefinitionStateVO;
import com.lavyoung.marketforge.domain.activity.model.vo.AwardUnlockScopeVO;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 运营活动定义领域实体。
 *
 * <p>该实体维护活动核心配置及生命周期不变量。活动创建时库存剩余量必须等于库存总量；
 * 活动开放后核心规则不可直接修改，只能关闭后新建活动。</p>
 *
 * @param activityId        活动业务标识
 * @param activityName      活动名称
 * @param activityDesc      活动说明
 * @param beginDateTime     开始时间
 * @param endDateTime       结束时间
 * @param stockCount        库存总量
 * @param stockCountSurplus 剩余库存
 * @param activityCountId   次数配置标识
 * @param strategyId        抽奖策略标识
 * @param awardUnlockScope  奖品解锁统计范围
 * @param state             生命周期状态
 * @param version           乐观锁版本
 * @param createdBy         创建人
 * @param updatedBy         修改人
 * @param createTime        创建时间
 * @param updateTime        修改时间
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
public record ActivityDefinitionEntity(Long activityId, String activityName, String activityDesc,
                                       LocalDateTime beginDateTime, LocalDateTime endDateTime,
                                       int stockCount, int stockCountSurplus, Long activityCountId,
                                       Long strategyId, AwardUnlockScopeVO awardUnlockScope,
                                       ActivityDefinitionStateVO state, int version, String createdBy,
                                       String updatedBy, LocalDateTime createTime, LocalDateTime updateTime) {

    /**
     * 创建并校验活动定义。
     *
     * @throws NullPointerException     必填字段为空时抛出
     * @throws IllegalArgumentException 标识、名称、时间、库存、版本或审计字段非法时抛出
     */
    public ActivityDefinitionEntity {
        Objects.requireNonNull(activityId, "activityId must not be null");
        Objects.requireNonNull(beginDateTime, "beginDateTime must not be null");
        Objects.requireNonNull(endDateTime, "endDateTime must not be null");
        Objects.requireNonNull(activityCountId, "activityCountId must not be null");
        Objects.requireNonNull(strategyId, "strategyId must not be null");
        Objects.requireNonNull(awardUnlockScope, "awardUnlockScope must not be null");
        Objects.requireNonNull(state, "state must not be null");
        requireText(activityName, "activityName", 64);
        requireText(activityDesc, "activityDesc", 256);
        requireText(createdBy, "createdBy", 64);
        requireText(updatedBy, "updatedBy", 64);
        if (activityId <= 0 || activityCountId <= 0 || strategyId <= 0) {
            throw new IllegalArgumentException("activity relation identifiers must be positive");
        }
        if (!beginDateTime.isBefore(endDateTime)) {
            throw new IllegalArgumentException("beginDateTime must be before endDateTime");
        }
        if (stockCount <= 0 || stockCountSurplus < 0 || stockCountSurplus > stockCount) {
            throw new IllegalArgumentException("activity stock is invalid");
        }
        if (version < 1) {
            throw new IllegalArgumentException("version must be greater than zero");
        }
    }

    /**
     * 判断当前定义是否允许编辑。
     *
     * @return 草稿状态返回 {@code true}
     */
    public boolean canEdit() {
        return state.canEdit();
    }

    private static void requireText(String value, String fieldName, int maximumLength) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank() || value.length() > maximumLength) {
            throw new IllegalArgumentException(fieldName + " is blank or too long");
        }
    }
}
