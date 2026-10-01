package com.lavyoung.marketforge.application.strategy.service.impl;

import com.lavyoung.marketforge.application.strategy.model.RaffleCommand;
import com.lavyoung.marketforge.application.strategy.model.RaffleResult;
import com.lavyoung.marketforge.application.strategy.model.StrategyAwardResult;
import com.lavyoung.marketforge.application.strategy.service.IStrategyRaffleService;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityEntity;
import com.lavyoung.marketforge.domain.activity.repository.IActivityRepository;
import com.lavyoung.marketforge.domain.strategy.model.entity.RaffleAwardEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.RaffleFactorEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyAwardEntity;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyRepository;
import com.lavyoung.marketforge.domain.strategy.service.IRaffleAward;
import com.lavyoung.marketforge.domain.strategy.service.IRaffleStrategy;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.IStrategyArmory;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import com.lavyoung.marketforge.types.utils.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
    private final IActivityRepository activityRepository;
    private final IStrategyRepository strategyRepository;
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
                        .endDateTime(validCommand.endDateTime())
                        .build()), "raffle result must not be null");
        // 查询具体策略奖品配置信息
        StrategyAwardEntity strategyAward = strategyRepository
                .getStrategyAwardEntity(award.strategyId(), award.awardId())
                .orElseThrow(() -> BusinessException.of(BusinessResponseCode.STRATEGY_NOT_FOUND, award.strategyId(), award.awardId()));
        return new RaffleResult(
                award.strategyId(),
                award.awardId(),
                award.awardKey(),
                award.awardConfig(),
                strategyAward.awardTitle(),
                award.awardDesc()
        );
    }

    @Override
    public void initStrategyRaffle(Long strategyId) {
        strategyArmory.assembleLotteryStrategy(strategyId);
    }

    @Override
    public void initStrategyRaffleByActivityId(Long activityId) {
        // 校验活动
        ActivityEntity activity = activityRepository.getActivityEntityByIdActivityId(activityId);
        if (activity == null) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_NOT_FOUND, activityId);
        }
        if (activity.strategyId() == null) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_STRATEGY_NOT_CONFIGURED, activityId);
        }
        LocalDateTime now = DateUtil.now();
        if (now.isBefore(activity.beginDateTime()) || now.isAfter(activity.endDateTime())) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_TIME_RANGE_ERROR, activityId, activity.beginDateTime(), activity.endDateTime());
        }
        boolean assembled = strategyArmory.assembleLotteryStrategy(activity.strategyId());
        if (!assembled) {
            throw BusinessException.of(BusinessResponseCode.STRATEGY_ASSEMBLY_FAILED, activity.strategyId());
        }
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
