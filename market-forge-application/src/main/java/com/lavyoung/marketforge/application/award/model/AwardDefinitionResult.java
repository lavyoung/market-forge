package com.lavyoung.marketforge.application.award.model;

import java.time.LocalDateTime;

/**
 * 奖品定义应用结果。
 *
 * <p>该结果用于 Application 向 Trigger 返回完整奖品定义，
 * 不向外层暴露领域值对象或基础设施持久化对象。</p>
 *
 * @param awardId       奖品业务标识
 * @param awardCode     奖品业务编码
 * @param awardName     奖品名称
 * @param awardType     奖品发放类型编码
 * @param claimMode     领取模式编码
 * @param providerCode  履约供应商编码
 * @param awardConfig   奖品发放配置快照内容
 * @param configVersion 配置结构版本
 * @param awardDesc     奖品说明
 * @param state         生命周期状态编码
 * @param version       乐观锁版本
 * @param createdBy     创建人标识
 * @param updatedBy     最后修改人标识
 * @param createTime    创建时间
 * @param updateTime    最后修改时间
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/21
 */
public record AwardDefinitionResult(
        Long awardId,
        String awardCode,
        String awardName,
        String awardType,
        String claimMode,
        String providerCode,
        String awardConfig,
        int configVersion,
        String awardDesc,
        String state,
        int version,
        String createdBy,
        String updatedBy,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
