package com.lavyoung.marketforge.domain.credit.repository;

import com.lavyoung.marketforge.domain.credit.model.aggregate.CreditAccountAdjustAggregate;
import com.lavyoung.marketforge.domain.credit.model.entity.CreditAccountEntity;

import java.util.Optional;

/**
 * 用户积分账户仓储端口。
 *
 * <p>负责积分账户订单和积分账户余额的持久化。领域层只表达账户查询和调额意图，
 * 具体的唯一索引幂等、账户不存在时创建、账户存在时累计余额由基础设施实现。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/11
 */
public interface ICreditAccountRepository {

    /**
     * 查询用户积分账户。
     *
     * <p>查询结果使用 {@link Optional} 表达账户是否存在，调用方必须显式处理空结果，
     * 不允许以 {@code null} 代表“未开户”，避免积分查询链路出现空指针。</p>
     *
     * @param userId 用户标识
     * @return 用户积分账户；账户不存在时返回 {@link Optional#empty()}
     */
    Optional<CreditAccountEntity> queryCreditAccountByUserId(String userId);

    /**
     * 保存积分账户调整聚合。
     *
     * <p>实现方必须保证积分订单创建和账户余额更新处于同一事务中。
     * 如果 {@code bizId} 已经存在，应视为幂等重复，不能重复调整账户余额。</p>
     *
     * @param aggregate 积分账户调整聚合
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 积分账户调整失败时抛出
     */
    void saveCreditAccountAdjust(CreditAccountAdjustAggregate aggregate);
}
