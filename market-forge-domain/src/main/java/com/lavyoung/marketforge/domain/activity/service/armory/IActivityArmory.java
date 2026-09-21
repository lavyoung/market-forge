package com.lavyoung.marketforge.domain.activity.service.armory;

/**
 * 库存装配处置
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/21
 */
public interface IActivityArmory {

    /**
     * 装配活动sku
     *
     * @param sku sku商品id
     * @return true 成功
     */
    boolean assembleActivitySku(Long sku);
}
