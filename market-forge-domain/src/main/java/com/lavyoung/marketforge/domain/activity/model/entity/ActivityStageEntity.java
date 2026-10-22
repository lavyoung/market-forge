package com.lavyoung.marketforge.domain.activity.model.entity;

import com.lavyoung.marketforge.domain.activity.model.vo.ActivityStageStateVO;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 活动展台领域实体。
 *
 * <p>一条展台配置表示某个渠道和流量来源准备向用户提供一个活动。
 * 只有处于 {@link ActivityStageStateVO#ACTIVE} 状态的配置才能被用户端查询。</p>
 *
 * <p>展台仅保存活动引用，不复制活动、策略或奖品配置。发布用例需要先校验活动配置并完成
 * Redis 预热，再将展台推进为生效状态，避免用户查询到尚未准备完成的活动。</p>
 *
 * @param stageId    展台业务标识
 * @param channel    投放渠道，例如 app、wechat
 * @param source     流量来源，例如 home、campaign
 * @param activityId 上架的活动标识
 * @param state      展台生命周期状态
 * @param version    乐观锁版本，从 1 开始
 * @param createdBy  创建人标识
 * @param updatedBy  最后修改人标识
 * @param createTime 创建时间；首次持久化前可以为空
 * @param updateTime 最后修改时间；首次持久化前可以为空
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
public record ActivityStageEntity(
        Long stageId,
        String channel,
        String source,
        Long activityId,
        ActivityStageStateVO state,
        int version,
        String createdBy,
        String updatedBy,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {

    private static final Pattern SC_CODE_PATTERN = Pattern.compile("[a-z][a-z0-9_-]{0,31}");

    /**
     * 创建并校验活动展台实体。
     *
     * @throws NullPointerException     必填字段为空时抛出
     * @throws IllegalArgumentException 标识、渠道、来源、版本或操作人不合法时抛出
     */
    public ActivityStageEntity {
        Objects.requireNonNull(stageId, "stageId must not be null");
        Objects.requireNonNull(activityId, "activityId must not be null");
        Objects.requireNonNull(state, "state must not be null");

        if (stageId <= 0) {
            throw new IllegalArgumentException("stageId must be positive");
        }
        if (activityId <= 0) {
            throw new IllegalArgumentException("activityId must be positive");
        }
        validateCode(channel, "channel");
        validateCode(source, "source");
        validateOperator(createdBy, "createdBy");
        validateOperator(updatedBy, "updatedBy");

        if (version < 1) {
            throw new IllegalArgumentException("version must be greater than zero");
        }
    }

    /**
     * 判断当前展台是否允许修改关联活动。
     *
     * @return 展台尚未上架时返回 {@code true}
     */
    public boolean canEdit() {
        return state == ActivityStageStateVO.CREATE;
    }

    /**
     * 判断当前展台是否正在对外提供活动。
     *
     * @return 展台已经生效时返回 {@code true}
     */
    public boolean isActive() {
        return state == ActivityStageStateVO.ACTIVE;
    }

    private static void validateCode(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (!SC_CODE_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(fieldName + " format is invalid");
        }
    }

    private static void validateOperator(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank() || value.length() > 64) {
            throw new IllegalArgumentException(fieldName + " must contain 1 to 64 characters");
        }
    }
}
