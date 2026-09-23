package com.lavyoung.marketforge.domain.activity.service;

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
     * 创建活动 SKU 账户充值订单。
     * <p>
     * 该方法用于“抽奖次数入账”场景，例如签到、分享、积分兑换、购买 SKU 等行为触发用户获得抽奖次数。
     * 实现方应完成 SKU、活动、次数配置和库存规则校验，创建额度订单，并为用户活动账户增加总、日、月
     * 可用次数。返回的订单号用于追踪本次入账结果。
     *
     * @param skuRechargeEntity 活动 SKU 充值请求，包含用户、SKU 和外部业务幂等号
     * @return 活动额度充值订单号
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 当参数非法、配置缺失、活动不可用、库存不足或入账失败时抛出
     */
    String createSkuRechargeOrder(SkuRechargeEntity skuRechargeEntity);
}
