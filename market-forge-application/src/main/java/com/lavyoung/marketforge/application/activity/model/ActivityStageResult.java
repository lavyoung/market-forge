package com.lavyoung.marketforge.application.activity.model;

import java.time.LocalDateTime;

/**
 * 活动展台应用结果。
 *
 * <p>该结果向外层提供稳定的字符串状态和审计信息，不泄漏领域枚举或持久化对象。</p>
 *
 * @param stageId    展台业务标识
 * @param channel    投放渠道
 * @param source     流量来源
 * @param activityId 关联活动标识
 * @param state      展台生命周期状态编码
 * @param version    乐观锁版本
 * @param createdBy  创建人标识
 * @param updatedBy  最后修改人标识
 * @param createTime 创建时间
 * @param updateTime 最后修改时间
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
public record ActivityStageResult(Long stageId, String channel, String source, Long activityId, String state,
                                  int version,
                                  String createdBy, String updatedBy, LocalDateTime createTime,
                                  LocalDateTime updateTime) {
}
