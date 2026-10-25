package com.lavyoung.marketforge.infrastructure.persistent.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lavyoung.marketforge.infrastructure.persistent.model.StrategyDefinitionStateUpdateParam;
import com.lavyoung.marketforge.infrastructure.persistent.po.StrategyPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

/**
 * 抽奖策略根配置数据访问接口。
 *
 * <p>当前保留按业务标识查询能力。草稿条件更新、状态迁移和乐观锁 SQL
 * 将随策略定义 Repository 实现一起增加，避免继续使用无状态条件的更新方法。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
@Mapper
public interface IStrategyDao extends BaseMapper<StrategyPO> {

    /**
     * 根据策略业务标识查询策略根配置。
     *
     * @param strategyId 策略标识
     * @return 策略持久化对象；不存在时返回 {@link Optional#empty()}
     */
    Optional<StrategyPO> queryStrategyByStrategyId(@Param("strategyId") Long strategyId);

    /**
     * 使用草稿状态和乐观锁版本更新策略基础配置。
     *
     * @param strategy 包含最新基础配置和期望版本的策略持久化对象
     * @return 受影响记录数；零表示策略不是草稿或版本已经变化
     */
    int updateDraft(StrategyPO strategy);

    /**
     * 使用当前状态和乐观锁版本推进策略生命周期。
     *
     * @param stateUpdate 状态条件更新参数
     * @return 受影响记录数；零表示状态或版本不匹配
     */
    int changeState(StrategyDefinitionStateUpdateParam stateUpdate);
}
