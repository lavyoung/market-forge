package com.lavyoung.marketforge.application.credit.service;

import com.lavyoung.marketforge.application.credit.model.ExchangeCreditForSkuCommand;

/**
 * 积分兑换应用服务。
 *
 * <p>该服务负责编排跨领域兑换用例。活动账户领域负责创建积分支付类型的待支付 SKU 订单，
 * 积分账户领域负责扣减用户积分并写入支付成功可靠消息；后续消息消费者再完成活动订单发货入账。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/11
 */
public interface ICreditExchangeApplicationService {

    /**
     * 使用积分兑换活动 SKU 对应的抽奖次数。
     *
     * <p>流程包括：
     * 先使用同一个外部业务幂等号创建待支付活动 SKU 订单，再根据命令扣减用户积分。
     * 积分扣减成功后由可靠消息触发异步发货入账。
     * 调用方必须保证同一次兑换请求的 {@code outBusinessNo} 稳定不变。</p>
     *
     * @param command 积分兑换活动 SKU 命令
     * @return 活动 SKU 订单号
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 参数非法、积分不足、SKU 不可用或充值失败时抛出
     */
    String exchangeCreditForSku(ExchangeCreditForSkuCommand command);
}
