package com.lavyoung.marketforge.api.activity.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 活动展台运营响应。
 *
 * <p>业务标识均以字符串返回，确保浏览器能够无损展示和回传十八位编号。</p>
 *
 * @param stageId    展台业务标识
 * @param channel    投放渠道
 * @param source     流量来源
 * @param activityId 关联活动标识
 * @param state      生命周期状态
 * @param version    乐观锁版本
 * @param createdBy  创建人
 * @param updatedBy  最后修改人
 * @param createTime 创建时间
 * @param updateTime 最后修改时间
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
@Schema(description = "活动展台")
public record ActivityStageResponse(String stageId, String channel, String source, String activityId, String state,
                                    int version,
                                    String createdBy, String updatedBy, LocalDateTime createTime,
                                    LocalDateTime updateTime) implements Serializable {
}
