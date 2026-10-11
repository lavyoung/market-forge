package com.lavyoung.marketforge.domain.credit.model.entity;

import com.lavyoung.marketforge.domain.credit.model.vo.CreditAccountBusinessTypeVO;
import com.lavyoung.marketforge.domain.credit.model.vo.CreditAccountOrderStateVO;
import com.lavyoung.marketforge.domain.credit.model.vo.CreditAccountTradeTypeVO;

import java.time.LocalDateTime;

/**
 * 用户积分账户订单领域实体。
 * <p>
 * 每一次积分增加或扣减都必须生成一笔账户订单。订单的 {@code bizId}
 * 是下游业务幂等号，例如抽奖订单号或签到返利单号，用于防止重复发奖、重复返利。
 *
 * @param userId       用户标识
 * @param orderId      积分账户订单号
 * @param bizId        业务幂等号
 * @param tradeType    积分变动方向，例如收入或支出
 * @param businessType 积分变动业务类型，例如中奖发奖、签到返利或积分兑换
 * @param amount       积分变动数量，必须为正数
 * @param state        积分账户订单状态
 * @param orderTime    订单创建时间
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/11
 */
public record CreditAccountOrderEntity(
        String userId,
        String orderId,
        String bizId,
        CreditAccountTradeTypeVO tradeType,
        CreditAccountBusinessTypeVO businessType,
        Long amount,
        CreditAccountOrderStateVO state,
        LocalDateTime orderTime
) {
}
