package com.lavyoung.marketforge.domain.credit.model.entity;

import com.lavyoung.marketforge.domain.credit.model.vo.CreditAccountBusinessTypeVO;
import com.lavyoung.marketforge.domain.credit.model.vo.CreditAccountTradeTypeVO;

/**
 * 积分账户调额请求实体。
 *
 * <p>用于表达一次积分账户余额调整命令。它只描述“谁、因为什么业务、按什么方向、
 * 调整多少积分”，不负责数据库落库和余额计算。</p>
 *
 * @param userId       用户标识
 * @param bizId        业务幂等号
 * @param amount       调整积分数量，必须为正数
 * @param tradeType    交易方向，收入或支出
 * @param businessType 业务类型，说明积分变化来源
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/11
 */
public record CreditAdjustEntity(
        String userId,
        String bizId,
        Long amount,
        CreditAccountTradeTypeVO tradeType,
        CreditAccountBusinessTypeVO businessType
) {
}
