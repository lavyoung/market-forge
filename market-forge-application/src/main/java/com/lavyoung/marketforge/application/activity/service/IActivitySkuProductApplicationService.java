package com.lavyoung.marketforge.application.activity.service;

import com.lavyoung.marketforge.application.activity.model.ActivitySkuProductResult;

import java.util.List;

/**
 * 活动 SKU 商品查询应用服务。
 *
 * <p>该服务面向积分兑换页，负责按活动标识查询可兑换 SKU 商品列表。
 * 它只做查询编排，不创建兑换订单、不扣减积分、不扣减库存。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/12
 */
public interface IActivitySkuProductApplicationService {

    /**
     * 查询指定活动下可用于积分兑换的 SKU 商品。
     *
     * @param activityId 抽奖活动标识
     * @return 活动 SKU 商品列表；没有配置 SKU 时返回空列表
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 当 SKU 关联的次数配置缺失或积分价格配置无效时抛出
     */
    List<ActivitySkuProductResult> querySkuProducts(Long activityId);
}
