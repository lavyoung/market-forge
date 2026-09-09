package com.lavyoung.marketforge.application.strategy.service.impl;

import com.lavyoung.marketforge.application.strategy.model.RaffleCommand;
import com.lavyoung.marketforge.application.strategy.model.RaffleResult;
import com.lavyoung.marketforge.application.strategy.model.StrategyAwardResult;
import com.lavyoung.marketforge.application.strategy.service.IStrategyRaffleService;
import com.lavyoung.marketforge.domain.strategy.model.entity.RaffleAwardEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.RaffleFactorEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyAwardEntity;
import com.lavyoung.marketforge.domain.strategy.service.IRaffleAward;
import com.lavyoung.marketforge.domain.strategy.service.IRaffleStrategy;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.IStrategyArmory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * 抽奖应用用例的默认实现。
 * <p>
 * 负责将应用命令转换为领域入参、调用领域服务，并将领域结果转换为应用结果。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StrategyStrategyRaffleServiceImpl implements IStrategyRaffleService {

    private final IRaffleStrategy raffleStrategy;
    private final IStrategyArmory strategyArmory;
    private final IRaffleAward raffleAward;

    /**
     * {@inheritDoc}
     */
    @Override
    public RaffleResult raffle(RaffleCommand command) {
        RaffleCommand validCommand = Objects.requireNonNull(command, "command must not be null");
        RaffleAwardEntity award = Objects.requireNonNull(raffleStrategy.performRaffle(
                RaffleFactorEntity.builder()
                        .userId(validCommand.userId())
                        .strategyId(validCommand.strategyId())
                        .build()), "raffle result must not be null");
        return new RaffleResult(
                award.strategyId(),
                award.awardId(),
                award.awardKey(),
                award.awardConfig(),
                award.awardDesc()
        );
    }

    @Override
    public void initStrategyRaffle(Long strategyId) {
        strategyArmory.assembleLotteryStrategy(strategyId);
    }

    @Override
    public List<StrategyAwardResult> queryRaffleStrategyAwardList(Long strategyId) {
        return raffleAward.queryRaffleStrategyAwardList(strategyId).stream()
                .map(this::toStrategyAwardResult)
                .toList();
    }

    /**
     * 将领域奖品实体投影为应用层查询结果，避免接口适配层依赖领域内部模型。
     *
     * @param entity 策略奖品领域实体
     * @return 策略奖品应用层查询结果
     */
    private StrategyAwardResult toStrategyAwardResult(StrategyAwardEntity entity) {
        return new StrategyAwardResult(
                entity.strategyId(),
                entity.awardId(),
                entity.awardTitle(),
                entity.awardCount(),
                entity.awardCountSurplus(),
                entity.awardRate(),
                entity.sort()
        );
    }
}
