package com.lavyoung.marketforge.domain.activity.service;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivityOrderEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityShopCartEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.SkuRechargeEntity;

/**
 * 活动账户额度服务端口。
 * <p>
 * 负责根据活动 SKU 或外部充值行为创建额度订单，并为用户活动账户增加可抽奖次数。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/18
 */
public interface IRaffleActivityAccountQuotaService {

    /**
     * 创建活动抽奖额度订单。
     *
     * @param activityShopCart 活动商品购物车实体
     * @return 活动订单实体
     */
    ActivityOrderEntity createRaffleActivityOrder(ActivityShopCartEntity activityShopCart);

    /**
     * 创建 sku 账户充值订单，给用户增加抽奖次数
     * <p>
     * 1. 在【打卡、签到、分享、对话、积分兑换】等行为动作下，创建出活动订单，给用户的活动账户【日、月】充值可用的抽奖次数。
     * 2. 对于用户可获得的抽奖次数，比如首次进来就有一次，则是依赖于运营配置的动作，在前端页面上。用户点击后，可以获得一次抽奖次数。
     *
     * @param skuRechargeEntity 活动商品充值实体对象
     * @return 活动ID
     */
    String createSkuRechargeOrder(SkuRechargeEntity skuRechargeEntity);
}
