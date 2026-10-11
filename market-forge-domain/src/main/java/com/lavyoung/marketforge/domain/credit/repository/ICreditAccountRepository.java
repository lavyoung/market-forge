package com.lavyoung.marketforge.domain.credit.repository;

import com.lavyoung.marketforge.domain.credit.model.aggregate.CreditAccountAdjustAggregate;

/**
 * 用户积分账户仓储端口。
 * <p>
 * 负责积分账户订单和积分账户余额的持久化。领域层只表达“需要入账多少积分”，
 * 具体的唯一索引幂等、账户不存在时创建、账户存在时累计余额由基础设施实现。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/11
 */
public interface ICreditAccountRepository {

    /**
     * 保存积分入账聚合。
     * <p>
     * 实现方必须保证积分订单创建和账户余额更新处于同一事务中。
     * 如果 {@code bizId} 已经存在，应视为幂等重复，不能重复增加账户余额。
     *
     * @param aggregate 积分账户调整聚合
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 积分入账失败时抛出
     */
    void saveCreditAccountAdjust(CreditAccountAdjustAggregate aggregate);
}
