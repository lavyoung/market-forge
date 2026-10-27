package com.lavyoung.marketforge.api.activity.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 活动 SKU 运营响应。
 *
 * <p>SKU、活动和次数配置标识均使用字符串返回，确保浏览器能够无损展示和回传
 * 十八位业务编号。</p>
 *
 * @param sku                    SKU 业务标识
 * @param activityId             所属活动业务标识
 * @param activityCountId        次数配置业务标识
 * @param stockCount             累计配置库存总量
 * @param stockCountSurplus      当前剩余库存
 * @param consumedCount          已消费库存
 * @param creditAmount           积分兑换价格；为空表示不支持积分兑换
 * @param supportsCreditExchange 是否支持积分兑换
 * @param version                乐观锁版本
 * @param createdBy              创建人
 * @param updatedBy              最后修改人
 * @param createTime             创建时间
 * @param updateTime             最后修改时间
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/27
 */
@Schema(description = "活动 SKU 运营定义")
public record ActivitySkuDefinitionResponse(String sku, String activityId, String activityCountId,
                                            long stockCount, long stockCountSurplus, long consumedCount,
                                            Long creditAmount, boolean supportsCreditExchange, int version,
                                            String createdBy, String updatedBy, LocalDateTime createTime,
                                            LocalDateTime updateTime) implements Serializable {
}
