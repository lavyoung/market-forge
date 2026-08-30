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

    /**
     * 根据策略标识查询抽奖策略。
     *
     * @param strategyId 策略标识
     * @return 策略持久化对象；不存在时返回空
     */
    Optional<StrategyPO> queryStrategyByStrategyId(@Param("strategyId") Long strategyId);

    /**
     * 新增抽奖策略。
     *
     * @param strategy 策略持久化对象
     * @return 受影响的记录数
     */
    int insert(StrategyPO strategy);

    /**
     * 根据策略标识更新抽奖策略。
     *
     * @param strategy 包含更新内容的策略持久化对象
     * @return 受影响的记录数
     */
    int updateByStrategyId(StrategyPO strategy);

}
