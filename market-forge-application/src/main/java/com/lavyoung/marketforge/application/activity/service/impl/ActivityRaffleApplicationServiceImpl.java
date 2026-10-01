package com.lavyoung.marketforge.application.activity.service.impl;

import com.lavyoung.marketforge.application.activity.model.ActivityAwardListCommand;
import com.lavyoung.marketforge.application.activity.model.ActivityAwardResult;
import com.lavyoung.marketforge.application.activity.model.ActivityRaffleCommand;
import com.lavyoung.marketforge.application.activity.model.ActivityRaffleResult;
import com.lavyoung.marketforge.application.activity.service.IActivityRaffleApplicationService;
import com.lavyoung.marketforge.application.strategy.model.RaffleCommand;
import com.lavyoung.marketforge.application.strategy.model.RaffleResult;
import com.lavyoung.marketforge.application.strategy.service.IStrategyRaffleService;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityAccountDayEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityOrderEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.PartakeRaffleActivityEntity;
import com.lavyoung.marketforge.domain.activity.repository.IActivityRepository;
import com.lavyoung.marketforge.domain.activity.service.IRaffleActivityPartakeService;
import com.lavyoung.marketforge.domain.activity.service.armory.IActivityArmory;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyAwardEntity;
import com.lavyoung.marketforge.domain.strategy.repository.IRuleTreeRepository;
import com.lavyoung.marketforge.domain.strategy.service.IRaffleAward;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 活动抽奖应用服务默认实现。
 * <p>
 * 本类只做跨域用例编排：活动域负责资格订单和额度，策略域负责命中奖品，
 * 用户中奖域负责中奖记录和可靠消息任务落库。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
public class ActivityRaffleApplicationServiceImpl implements IActivityRaffleApplicationService {

    private final IRaffleActivityPartakeService activityPartakeService;
    private final IStrategyRaffleService strategyRaffleService;
    private final ActivityRaffleSettlementTransaction settlementTransaction;
    private final IActivityArmory activityArmory;

    private final IActivityRepository activityRepository;
    private final IRaffleAward raffleAward;
    private final IRuleTreeRepository ruleTreeRepository;

    /**
     * {@inheritDoc}
     * <p>
     * 活动订单中携带活动结束时间，策略抽奖会继续向下透传该时间，
     * 供库存规则节点计算 Redis 锁租约。
     */
    @Override
    public ActivityRaffleResult raffle(ActivityRaffleCommand command) {
        ActivityRaffleCommand validCommand = Objects.requireNonNull(command, "command must not be null");
        ActivityOrderEntity activityOrder = activityPartakeService.createRaffleOrder(new PartakeRaffleActivityEntity(
                validCommand.userId(),
                validCommand.sku(),
                validCommand.activityId()
        ));
        RaffleResult raffleResult = strategyRaffleService.raffle(new RaffleCommand(
                activityOrder.userId(),
                activityOrder.strategyId(),
                activityOrder.endDateTime()
        ));

        settlementTransaction.settle(activityOrder, raffleResult);
        return new ActivityRaffleResult(
                activityOrder.orderId(),
                activityOrder.activityId(),
                raffleResult.strategyId(),
                raffleResult.awardId(),
                raffleResult.awardKey(),
                raffleResult.awardConfig(),
                raffleResult.awardTitle(),
                raffleResult.awardDesc()
        );
    }

    /**
     * {@inheritDoc}
     * <p>
     * 查询过程只聚合展示信息，不创建活动订单、不扣减活动额度，也不扣减奖品库存。
     */
    @Override
    public List<ActivityAwardResult> queryAwardList(ActivityAwardListCommand command) {
        ActivityAwardListCommand validCommand = Objects.requireNonNull(command, "command must not be null");
        Long activityId = validCommand.activityId();
        String userId = validCommand.userId();

        ActivityEntity activityEntity = activityRepository.getActivityEntityByIdActivityId(activityId);
        if (activityEntity == null) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_NOT_FOUND);
        }
        if (activityEntity.strategyId() == null) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_STRATEGY_NOT_CONFIGURED, activityId);
        }

        List<StrategyAwardEntity> awardEntities = raffleAward.queryRaffleStrategyAwardList(activityEntity.strategyId());
        Map<String, Integer> ruleLockCountMap = queryRuleLockCountMap(awardEntities);
        int dayPartakeCount = queryDayPartakeCount(userId, activityId);
        return awardEntities.stream()
                .map(award -> toActivityAwardResult(award, ruleLockCountMap, dayPartakeCount))
                .toList();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void armory(Long activityId) {
        boolean assembled = activityArmory.assembleActivitySkuByActivityId(activityId);
        if (!assembled) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_ASSEMBLY_FAILED, activityId);
        }
        strategyRaffleService.initStrategyRaffleByActivityId(activityId);
    }

    /**
     * 从奖品列表中提取规则树标识。
     * <p>
     * 只有配置了规则模型的奖品才需要查询规则锁。
     * 没有配置 ruleModels 的奖品，默认不受次数锁限制。
     *
     * @param awards 策略奖品列表
     * @return 去重后的规则树标识列表
     */
    private List<String> extractRuleModelTreeIds(List<StrategyAwardEntity> awards) {
        return awards.stream()
                .map(StrategyAwardEntity::ruleModels)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
    }

    /**
     * 将策略奖品配置转换为活动页奖品展示结果。
     *
     * @param award            策略奖品配置
     * @param ruleLockCountMap 规则树标识到解锁次数的映射
     * @param dayPartakeCount  用户当天已参与次数
     * @return 活动奖品展示结果
     */
    private ActivityAwardResult toActivityAwardResult(StrategyAwardEntity award, Map<String, Integer> ruleLockCountMap, int dayPartakeCount) {
        Integer awardRuleLockCount = StringUtils.isBlank(award.ruleModels())
                ? null
                : ruleLockCountMap.get(award.ruleModels());
        boolean unlocked = awardRuleLockCount == null || dayPartakeCount >= awardRuleLockCount;

        int waitUnlockCount = unlocked ? 0 : awardRuleLockCount - dayPartakeCount;
        return new ActivityAwardResult(
                award.awardId(),
                award.awardTitle(),
                award.awardSubtitle(),
                award.sort(),
                awardRuleLockCount,
                unlocked,
                waitUnlockCount
        );
    }

    /**
     * 查询奖品次数锁规则。
     * <p>
     * 返回结构示例：
     * tree_lock_1 -> 1
     * tree_lock_2 -> 2
     *
     * @param awards 策略奖品列表
     * @return 规则树标识到解锁次数的映射
     */
    private Map<String, Integer> queryRuleLockCountMap(List<StrategyAwardEntity> awards) {
        List<String> treeIds = extractRuleModelTreeIds(awards);
        if (treeIds.isEmpty()) {
            return Map.of();
        }
        return ruleTreeRepository.queryAwardRuleLockCount(treeIds);
    }

    /**
     * 查询用户当天已参与活动抽奖次数。
     * <p>
     * 用户日账户保存的是当天总次数和当天剩余次数。
     * 已参与次数 = dayCount - dayCountSurplus。
     *
     * @param userId     用户标识
     * @param activityId 活动标识
     * @return 用户当天已参与次数；没有日账户时返回 0
     */
    private int queryDayPartakeCount(String userId, Long activityId) {
        ActivityAccountDayEntity dayAccount = activityRepository.queryActivityAccountDayByUserId(userId, activityId, LocalDate.now());
        if (dayAccount == null) {
            return 0;
        }
        return Math.max(0, dayAccount.dayCount() - dayAccount.dayCountSurplus());
    }
}
