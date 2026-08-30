package com.lavyoung.marketforge.infrastructure.persistent.dao;

import com.lavyoung.marketforge.infrastructure.persistent.po.StrategyAwardPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 抽奖策略奖品数据访问接口。
 *
 * @author lavyoung
 * @version 1.0.0-SNAPSHOT
 * @email lavyoung1325@outlook.com
 */
@Mapper
public interface IStrategyAwardDao {

    List<StrategyAwardPO> queryStrategyAwardList(@Param("strategyId") Long strategyId);

    Optional<StrategyAwardPO> queryStrategyAward(
            @Param("strategyId") Long strategyId,
            @Param("awardId") Long awardId);

    int insert(StrategyAwardPO strategyAward);

    int updateByStrategyIdAndAwardId(StrategyAwardPO strategyAward);

    int decrementAwardCountSurplus(
            @Param("strategyId") Long strategyId,
            @Param("awardId") Long awardId);

}
