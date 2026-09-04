package com.lavyoung.marketforge.infrastructure.persistent.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lavyoung.marketforge.infrastructure.persistent.po.StrategyPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

/**
 * 抽奖策略数据访问接口。
 * <p>
 * 在 MyBatis-Plus 通用 CRUD 能力之外，提供按策略业务标识查询和更新的方法。
 *
 * @author lavyoung
 * @version 1.0.0-SNAPSHOT
 * @email lavyoung1325@outlook.com
 */
@Mapper
public interface IStrategyDao extends BaseMapper<StrategyPO> {

    /**
     * 根据策略标识查询抽奖策略。
     *
     * @param strategyId 策略标识
     * @return 策略持久化对象；不存在时返回 {@link Optional#empty()}
     */
    Optional<StrategyPO> queryStrategyByStrategyId(@Param("strategyId") Long strategyId);

    /**
     * 根据策略标识更新抽奖策略。
     *
     * @param strategy 包含更新内容的策略持久化对象
     * @return 受影响的记录数
     */
    int updateByStrategyId(StrategyPO strategy);

}
