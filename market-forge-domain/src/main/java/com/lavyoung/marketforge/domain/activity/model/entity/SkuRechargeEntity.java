package com.lavyoung.marketforge.domain.activity.model.entity;

/**
 *
 * @param userId        用户id
 * @param sku           商品剋
 * @param outBusinessNo 业务幂等ID
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/20
 */
public record SkuRechargeEntity(
        String userId,
        Long sku,
        String outBusinessNo
) {

}
