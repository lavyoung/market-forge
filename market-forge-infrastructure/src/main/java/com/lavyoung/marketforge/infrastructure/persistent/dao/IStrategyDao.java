package com.lavyoung.marketforge.infrastructure.persistent.dao;

import com.lavyoung.marketforge.infrastructure.persistent.po.StrategyPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

/**
 * 抽奖策略数据访问接口。
 *
 * @author lavyoung
 * @version 1.0.0-SNAPSHOT
 * @email lavyoung1325@outlook.com
 */
@Mapper
public interface IStrategyDao {

    Optional<StrategyPO> queryStrategyByStrategyId(@Param("strategyId") Long strategyId);

    int insert(StrategyPO strategy);

    int updateByStrategyId(StrategyPO strategy);

}
