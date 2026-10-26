package com.lavyoung.marketforge.api.activity.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 活动次数配置运营响应。
 *
 * <p>业务标识使用字符串返回，确保浏览器能够无损展示和回传十八位编号。</p>
 *
 * @param activityCountId   活动次数配置业务标识
 * @param activityCountName 配置名称
 * @param totalCount        总入账次数
 * @param dayCount          日入账次数
 * @param monthCount        月入账次数
 * @param version           乐观锁版本
 * @param createdBy         创建人
 * @param updatedBy         最后修改人
 * @param createTime        创建时间
 * @param updateTime        最后修改时间
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/26
 */
@Schema(description = "活动次数配置")
public record ActivityCountDefinitionResponse(String activityCountId, String activityCountName, int totalCount,
                                              int dayCount, int monthCount, int version, String createdBy,
                                              String updatedBy, LocalDateTime createTime,
                                              LocalDateTime updateTime) implements Serializable {
}
