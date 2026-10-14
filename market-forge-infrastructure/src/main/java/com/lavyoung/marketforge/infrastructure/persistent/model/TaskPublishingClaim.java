package com.lavyoung.marketforge.infrastructure.persistent.model;

import java.time.LocalDateTime;

/**
 * 消息任务投递权抢占参数。
 *
 * <p>该对象封装一次任务状态抢占所需的全部条件，避免 DAO 方法携带过多参数。
 * 抢占操作仅允许以下任务进入 {@code publishing} 状态：</p>
 *
 * <ul>
 *     <li>首次等待投递的任务；</li>
 *     <li>已到达重试时间的失败任务；</li>
 *     <li>投递租约已经过期的处理中任务。</li>
 * </ul>
 *
 * @param userId                  用户标识，同时作为任务表分片键
 * @param eventId                 事件唯一标识
 * @param publishingState         抢占成功后写入的处理中状态
 * @param createState             首次等待投递状态
 * @param publishFailedState      投递失败状态
 * @param publishingExpiredState  可被租约回收的处理中状态
 * @param now                     当前时间，同时用于更新时间和重试时间判断
 * @param publishingExpiredBefore 投递租约失效边界
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/14
 */
public record TaskPublishingClaim(
        String userId,
        String eventId,
        String publishingState,
        String createState,
        String publishFailedState,
        String publishingExpiredState,
        LocalDateTime now,
        LocalDateTime publishingExpiredBefore
) {
}
