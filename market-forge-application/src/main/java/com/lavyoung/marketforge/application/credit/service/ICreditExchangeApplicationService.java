package com.lavyoung.marketforge.application.credit.service;

import com.lavyoung.marketforge.application.credit.model.ExchangeCreditForSkuCommand;

/**
 * 积分兑换应用服务。
 *
 * <p>该服务负责编排跨领域兑换用例。积分账户领域只负责扣减积分，
 * 活动账户领域只负责创建 SKU 充值订单；本应用服务负责把两个领域动作串成一次完整兑换流程。</p>
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
     * 先根据命令扣减用户积分，再使用同一个外部业务幂等号创建活动 SKU 充值订单。
     * 调用方必须保证同一次兑换请求的 {@code outBusinessNo} 稳定不变。</p>
     *
     * @param command 积分兑换活动 SKU 命令
     * @return 活动 SKU 充值订单号
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 参数非法、积分不足、SKU 不可用或充值失败时抛出
     */
    String exchangeCreditForSku(ExchangeCreditForSkuCommand command);
}
