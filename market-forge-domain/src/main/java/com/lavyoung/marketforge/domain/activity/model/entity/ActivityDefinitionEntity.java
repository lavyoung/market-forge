package com.lavyoung.marketforge.domain.activity.model.entity;

import com.lavyoung.marketforge.domain.activity.model.vo.ActivityDefinitionStateVO;
import com.lavyoung.marketforge.domain.activity.model.vo.AwardUnlockScopeVO;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 运营活动定义领域实体。
 *
 * <p>该实体维护活动名称、有效期、次数配置、抽奖策略和生命周期状态等活动级
 * 配置。活动定义不直接维护库存，额度商品库存由活动 SKU 聚合负责，奖品库存
 * 由策略奖品聚合负责。</p>
 *
 * <p>活动开放后，核心规则不可直接修改。如果运营需要更换次数配置或抽奖策略，
 * 应先关闭当前活动，再通过新的活动定义完成配置，避免已经参与活动的用户受到
 * 运行中规则变化影响。</p>
 *
 * @param activityId       活动业务标识
 * @param activityName     活动名称
 * @param activityDesc     活动说明
 * @param beginDateTime    活动开始时间
 * @param endDateTime      活动结束时间
 * @param activityCountId  次数配置标识
 * @param strategyId       抽奖策略标识
 * @param awardUnlockScope 奖品解锁统计范围
 * @param state            生命周期状态
 * @param version          乐观锁版本
 * @param createdBy        创建人
 * @param updatedBy        修改人
 * @param createTime       创建时间
 * @param updateTime       修改时间
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/27
 */
public record ActivityDefinitionEntity(
        Long activityId,
        String activityName,
        String activityDesc,
        LocalDateTime beginDateTime,
        LocalDateTime endDateTime,
        Long activityCountId,
        Long strategyId,
        AwardUnlockScopeVO awardUnlockScope,
        ActivityDefinitionStateVO state,
        int version,
        String createdBy,
        String updatedBy,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {

    /**
     * 创建并校验活动定义。
     *
     * <p>校验按以下顺序执行：</p>
     *
     * <ol>
     *     <li>检查活动、次数配置和策略标识是否存在。</li>
     *     <li>检查活动名称、说明及审计人员是否有效。</li>
     *     <li>检查所有业务标识是否为正数。</li>
     *     <li>检查活动开始时间必须早于结束时间。</li>
     *     <li>检查乐观锁版本必须大于零。</li>
     * </ol>
     *
     * <p>这里不再校验活动库存。活动 SKU 和策略奖品会分别在自己的领域模型中
     * 校验库存总量、剩余量以及调额边界。</p>
     *
     * @throws NullPointerException     必填字段为空时抛出
     * @throws IllegalArgumentException 标识、名称、时间、版本或审计字段非法时抛出
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
        if (version < 1) {
            throw new IllegalArgumentException("version must be greater than zero");
        }
    }

    /**
     * 判断当前活动定义是否允许编辑。
     *
     * <p>只有草稿状态允许编辑核心活动配置，避免运行中的活动被直接修改。</p>
     *
     * @return 草稿状态返回 {@code true}，其他状态返回 {@code false}
     */
    public boolean canEdit() {
        return state.canEdit();
    }

    /**
     * 校验必填文本内容。
     *
     * @param value         待校验文本
     * @param fieldName     字段名称，用于异常信息
     * @param maximumLength 允许的最大字符数
     * @throws NullPointerException     文本为空时抛出
     * @throws IllegalArgumentException 文本为空白或超过最大长度时抛出
     */
    private static void requireText(String value, String fieldName, int maximumLength) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank() || value.length() > maximumLength) {
            throw new IllegalArgumentException(fieldName + " is blank or too long");
        }
    }
}
