package com.lavyoung.marketforge.domain.strategy.repository;

import com.lavyoung.marketforge.domain.strategy.model.aggregate.StrategyDefinitionAggregate;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyDefinitionStateChange;

import java.util.Optional;

/**
 * 策略定义运营仓储端口。
 *
 * <p>本仓储负责策略运营聚合的读取和持久化，管理 {@code strategy}、
 * {@code strategy_award} 和 {@code strategy_rule} 三张表。具体 MyBatis DAO、
 * PO、批量写入和事务实现由基础设施层负责。</p>
 *
 * <p>抽奖运行时的概率快照、Redis 库存和规则读取继续由
 * {@link IStrategyRepository} 提供，避免配置写入职责进入高频抽奖链路。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
public interface IStrategyDefinitionRepository {

    /**
     * 查询完整策略定义聚合。
     *
     * <p>实现方应分别批量查询策略根配置、策略奖品和策略规则，再组装为完整聚合。
     * 禁止在遍历奖品时逐条查询规则，避免产生 N+1 查询。</p>
     *
     * @param strategyId 策略业务标识
     * @return 完整策略定义；策略不存在时返回空
     */
    Optional<StrategyDefinitionAggregate> queryByStrategyId(Long strategyId);

    /**
     * 创建策略定义。
     *
     * <p>创建普通草稿时，奖品和规则集合可以为空；复制历史策略时可能同时包含
     * 根配置、奖品和规则。实现方必须在同一个数据库事务中完成三张表的写入。</p>
     *
     * @param aggregate 待创建的策略定义聚合
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 策略标识重复或持久化失败时抛出
     */
    void create(StrategyDefinitionAggregate aggregate);

    /**
     * 使用草稿状态和乐观锁版本更新完整策略配置。
     *
     * <p>实现方必须先执行策略根记录的条件更新：</p>
     *
     * <pre>
     * WHERE strategy_id = ?
     *   AND state = 'draft'
     *   AND version = ?
     * </pre>
     *
     * <p>只有根记录更新成功后，才能批量替换对应的策略奖品和策略规则。
     * 三张表的修改必须处于同一个事务；任意子配置写入失败时整体回滚。</p>
     *
     * @param aggregate 包含最新基础信息、奖品、规则和期望版本的草稿聚合
     * @return 更新成功返回 {@code true}；状态或版本不匹配返回 {@code false}
     */
    boolean updateDraft(StrategyDefinitionAggregate aggregate);

    /**
     * 使用当前状态和乐观锁版本推进策略生命周期。
     *
     * <p>该方法只负责状态条件更新，不在状态变更时删除历史配置或概率快照。</p>
     *
     * @param stateChange 状态变更请求
     * @return 状态更新成功返回 {@code true}；当前状态或版本不匹配返回 {@code false}
     */
    boolean changeState(StrategyDefinitionStateChange stateChange);
}
