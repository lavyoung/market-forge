package com.lavyoung.marketforge.domain.award.model.entity;

import com.lavyoung.marketforge.domain.award.model.vo.AwardClaimModeVO;
import com.lavyoung.marketforge.domain.award.model.vo.AwardConfigSnapshot;
import com.lavyoung.marketforge.domain.award.model.vo.AwardDefinitionStateVO;
import com.lavyoung.marketforge.domain.award.model.vo.AwardTypeVO;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 奖品定义领域实体。
 *
 * <p>该实体描述运营人员配置的一种具体奖品，包括业务编码、发奖类型、领取方式、
 * 履约供应商和发奖参数。抽奖策略只通过 {@code awardId} 引用奖品定义，
 * 不直接拥有这些履约配置。</p>
 *
 * <p>奖品启用后，其核心发奖配置不允许原地修改。配置变化应复制为新的奖品版本，
 * 防止已经产生的中奖记录因为配置变化而改变履约行为。</p>
 *
 * @param awardId      奖品业务标识，由服务端生成
 * @param awardCode    运营可识别的奖品业务编码
 * @param awardName    奖品名称
 * @param awardType    发奖处理器类型
 * @param claimMode    领取模式
 * @param providerCode 履约供应商编码；内部奖品可以为空
 * @param awardConfig  结构化发奖配置快照
 * @param awardDesc    奖品说明；可以为空
 * @param state        奖品定义生命周期状态
 * @param version      乐观锁版本，从 1 开始
 * @param createdBy    创建人标识
 * @param updatedBy    最后修改人标识
 * @param createTime   创建时间；首次持久化前可以为空
 * @param updateTime   最后修改时间；首次持久化前可以为空
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/21
 */
public record AwardDefinitionEntity(
        Long awardId,
        String awardCode,
        String awardName,
        AwardTypeVO awardType,
        AwardClaimModeVO claimMode,
        String providerCode,
        AwardConfigSnapshot awardConfig,
        String awardDesc,
        AwardDefinitionStateVO state,
        int version,
        String createdBy,
        String updatedBy,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
    private static final Pattern AWARD_CODE_PATTERN = Pattern.compile("[a-z][a-z0-9_-]{1,63}");
    private static final Pattern PROVIDER_CODE_PATTERN = Pattern.compile("[a-z][a-z0-9_-]{1,31}");

    /**
     * 创建并校验奖品定义。
     *
     * <p>这里只校验所有奖品共有的结构约束。积分数量、活动 SKU、券模板等类型专属参数，
     * 后续由对应的 {@code AwardGrantHandler} 校验。</p>
     *
     * @throws NullPointerException     必填对象为空时抛出
     * @throws IllegalArgumentException 标识、编码、名称、供应商编码或版本不合法时抛出
     */
    public AwardDefinitionEntity {
        Objects.requireNonNull(awardId, "awardId must not be null");
        Objects.requireNonNull(awardType, "awardType must not be null");
        Objects.requireNonNull(claimMode, "claimMode must not be null");
        Objects.requireNonNull(awardConfig, "awardConfig must not be null");
        Objects.requireNonNull(state, "state must not be null");

        if (awardId <= 0) {
            throw new IllegalArgumentException("awardId must be positive");
        }
        requireCode(awardCode, "awardCode", AWARD_CODE_PATTERN);
        requireText(awardName, "awardName", 128);
        requireText(createdBy, "createdBy", 64);
        requireText(updatedBy, "updatedBy", 64);

        if (providerCode != null) {
            requireCode(providerCode, "providerCode", PROVIDER_CODE_PATTERN);
        }
        if (awardDesc != null && awardDesc.length() > 256) {
            throw new IllegalArgumentException("awardDesc length must not exceed 256");
        }
        if (version < 1) {
            throw new IllegalArgumentException("version must be greater than zero");
        }
    }

    /**
     * 判断奖品定义是否允许直接编辑。
     *
     * @return 当前奖品仍为草稿时返回 {@code true}
     */
    public boolean canEdit() {
        return state.canEdit();
    }

    /**
     * 判断奖品定义是否允许被新策略引用。
     *
     * @return 当前奖品已启用时返回 {@code true}
     */
    public boolean canReference() {
        return state.canReference();
    }

    /**
     * 判断中奖后是否需要等待用户提交领取信息。
     *
     * @return 领取模式为人工领取时返回 {@code true}
     */
    public boolean requiresManualClaim() {
        return claimMode == AwardClaimModeVO.MANUAL;
    }

    /**
     * 判断奖品是否配置了外部履约供应商。
     *
     * @return 存在供应商编码时返回 {@code true}
     */
    public boolean hasProvider() {
        return providerCode != null;
    }

    private static void requireCode(String value, String fieldName, Pattern pattern) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (!pattern.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    fieldName + " must start with a lowercase letter and contain only lowercase letters, numbers, underscores or hyphens"
            );
        }
    }

    private static void requireText(String value, String fieldName, int maximumLength) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        if (value.length() > maximumLength) {
            throw new IllegalArgumentException(fieldName + " length must not exceed " + maximumLength);
        }
    }
}
