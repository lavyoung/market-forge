package com.lavyoung.marketforge.application.activity.model;

import java.time.LocalDateTime;

/**
 * 活动次数配置应用结果。
 *
 * @param activityCountId   活动次数配置业务标识
 * @param activityCountName 配置名称
 * @param totalCount        一次入账增加的总抽奖次数
 * @param dayCount          一次入账增加的日抽奖次数
 * @param monthCount        一次入账增加的月抽奖次数
 * @param version           乐观锁版本
 * @param createdBy         创建人标识
 * @param updatedBy         最后修改人标识
 * @param createTime        创建时间
 * @param updateTime        最后修改时间
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/26
 */
public record ActivityCountDefinitionResult(Long activityCountId, String activityCountName, int totalCount,
                                            int dayCount, int monthCount, int version, String createdBy,
                                            String updatedBy, LocalDateTime createTime, LocalDateTime updateTime) {
}
