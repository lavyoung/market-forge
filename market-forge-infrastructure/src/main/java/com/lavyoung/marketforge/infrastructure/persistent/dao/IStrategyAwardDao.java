package com.lavyoung.marketforge.infrastructure.persistent.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lavyoung.marketforge.infrastructure.persistent.po.StrategyAwardPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 抽奖策略奖品数据访问接口。
 * <p>
 * 在 MyBatis-Plus 通用 CRUD 能力之外，提供按策略维度访问奖品配置及原子扣减库存的方法。
 *
 * @author lavyoung
 * @version 1.0.0-SNAPSHOT
 * @email lavyoung1325@outlook.com
 */
@Mapper
public interface IStrategyAwardDao extends BaseMapper<StrategyAwardPO> {

    /**
     * 查询指定策略下按展示顺序排列的全部奖品。
     *
     * @param strategyId 策略标识
     * @return 策略奖品列表；不存在时返回空列表
     */
    List<StrategyAwardPO> queryStrategyAwardList(@Param("strategyId") Long strategyId);

    /**
     * 查询指定策略下的单个奖品配置。
     *
     * @param strategyId 策略标识
     * @param awardId    奖品标识
     * @return 策略奖品持久化对象；不存在时返回 {@link Optional#empty()}
     */
    Optional<StrategyAwardPO> queryStrategyAward(
            @Param("strategyId") Long strategyId,
            @Param("awardId") Long awardId);

    /**
     * 根据策略标识和奖品标识更新奖品配置。
     *
     * @param strategyAward 包含更新内容的策略奖品持久化对象
     * @return 受影响的记录数
     */
    int updateByStrategyIdAndAwardId(StrategyAwardPO strategyAward);

    /**
     * 原子扣减指定策略奖品的剩余库存。
     *
     * @param strategyId 策略标识
     * @param awardId    奖品标识
     * @return 成功扣减时为 {@code 1}，库存不足或记录不存在时为 {@code 0}
     */
    int decrementAwardCountSurplus(
            @Param("strategyId") Long strategyId,
            @Param("awardId") Long awardId);

}
