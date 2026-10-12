package com.lavyoung.marketforge.application.credit.model;

/**
 * 用户积分账户查询结果。
 *
 * <p>该对象是 application 层输出给 trigger 层的查询结果，
 * 用于表达用户当前可用于兑换商品的积分余额。它不直接暴露领域层
 * {@code CreditAccountEntity}，避免 HTTP 层依赖领域模型。</p>
 *
 * <p>当前查询场景只需要展示可用积分余额，因此不返回累计积分、
 * 积分流水或账户调整原因。积分是否足够兑换，仍由兑换用例在扣减时
 * 以最新账户余额做原子校验。</p>
 *
 * @param userId          用户标识
 * @param availableAmount 当前可用积分余额
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/12
 */
public record CreditAccountResult(
        String userId,
        Long availableAmount
) {
}
