package com.lavyoung.marketforge.application.strategy.service;

import com.lavyoung.marketforge.application.strategy.model.RaffleCommand;
import com.lavyoung.marketforge.application.strategy.model.RaffleResult;
import com.lavyoung.marketforge.application.strategy.model.StrategyAwardResult;

import java.util.List;

/**
 * 抽奖应用用例入口。
 */
public interface IStrategyRaffleService {

    /**
     * 编排并执行一次抽奖。
     *
     * @param command 抽奖命令
     * @return 抽奖结果
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 违反抽奖业务规则时抛出
     */
    RaffleResult raffle(RaffleCommand command);


    /**
     * 初始化抽奖策略
     *
     * @param strategyId 策略id
     */
    void initStrategyRaffle(Long strategyId);


    /**
     * 查询指定策略的可抽取奖品列表。
     *
     * @param strategyId 抽奖策略标识
     * @return 隔离领域内部字段后的策略奖品查询结果
     */
    List<StrategyAwardResult> queryRaffleStrategyAwardList(Long strategyId);
}
