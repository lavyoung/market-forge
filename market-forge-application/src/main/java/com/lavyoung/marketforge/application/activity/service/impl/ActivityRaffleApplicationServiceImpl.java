package com.lavyoung.marketforge.application.activity.service.impl;

import com.lavyoung.marketforge.application.activity.model.*;
import com.lavyoung.marketforge.application.activity.service.IActivityAccessPolicy;
import com.lavyoung.marketforge.application.activity.service.IActivityDegradeSwitch;
import com.lavyoung.marketforge.application.activity.service.IActivityRaffleApplicationService;
import com.lavyoung.marketforge.application.strategy.model.RaffleCommand;
import com.lavyoung.marketforge.application.strategy.model.RaffleResult;
import com.lavyoung.marketforge.application.strategy.service.IStrategyRaffleService;
import com.lavyoung.marketforge.domain.activity.model.entity.*;
import com.lavyoung.marketforge.domain.activity.model.vo.AwardUnlockScopeVO;
import com.lavyoung.marketforge.domain.activity.repository.IActivityRepository;
import com.lavyoung.marketforge.domain.activity.service.IRaffleActivityPartakeService;
import com.lavyoung.marketforge.domain.activity.service.armory.IActivityArmory;
import com.lavyoung.marketforge.domain.award.model.vo.AwardUnlockCountQueryVO;
import com.lavyoung.marketforge.domain.award.model.vo.AwardUnlockResultVO;
import com.lavyoung.marketforge.domain.award.service.IAwardUnlockService;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyAwardEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyRuleEntity;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyAwardRuleModelVO;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyRepository;
import com.lavyoung.marketforge.domain.strategy.service.IRaffleAward;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

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
    private final IAwardUnlockService awardUnlockService;
    private final IStrategyRaffleService strategyRaffleService;
    private final ActivityRaffleSettlementTransaction settlementTransaction;
    private final IActivityArmory activityArmory;

    private final IActivityRepository activityRepository;
    private final IRaffleAward raffleAward;
    private final IStrategyRepository strategyRepository;

    private final IActivityDegradeSwitch activityDegradeSwitch;
    private final IActivityAccessPolicy activityAccessPolicy;
    /**
     * {@inheritDoc}
     * <p>
     * 活动订单中携带活动结束时间，策略抽奖会继续向下透传该时间，
     * 供库存规则节点计算 Redis 锁租约。
     */
    @Override
    public ActivityRaffleResult raffle(ActivityRaffleCommand command) {
        if (activityDegradeSwitch.isDegraded()) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_DEGRADED);
        }
        ActivityRaffleCommand validCommand = Objects.requireNonNull(command, "command must not be null");
        if (!activityAccessPolicy.isAllowed(validCommand.userId())) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_ACCESS_DENIED, validCommand.userId());
        }
        ActivityOrderEntity activityOrder = activityPartakeService.createRaffleOrder(new PartakeRaffleActivityEntity(
                validCommand.userId(),
                validCommand.sku(),
                validCommand.activityId()
        ));
        RaffleResult raffleResult = strategyRaffleService.raffle(new RaffleCommand(
                activityOrder.userId(),
                activityOrder.strategyId(),
                activityOrder.activityId(),
                activityOrder.awardUnlockScope(),
                activityOrder.orderTime().toLocalDate(),
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
        if (activityEntity.awardUnlockScope() == null) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_CONFIG_INVALID, activityId, "awardUnlockScope");
        }

        List<StrategyAwardEntity> awardEntities = raffleAward.queryRaffleStrategyAwardList(activityEntity.strategyId());
        Map<Long, Integer> ruleLockCountMap = queryRuleLockCountMap(activityEntity.strategyId(), awardEntities);
        Map<Long, Integer> ruleWeightCountMap = queryRuleWeightCountMap(activityEntity.strategyId());
        Map<Integer, AwardUnlockResultVO> unlockResultMap = queryAwardUnlockResultMap(userId, activityEntity, ruleLockCountMap);
        long userRaffleCount = strategyRepository.queryUserRaffleCount(userId, activityEntity.strategyId());

        AwardDisplayContext context = new AwardDisplayContext(
                activityId,
                activityEntity.awardUnlockScope(),
                ruleLockCountMap,
                unlockResultMap,
                ruleWeightCountMap,
                userRaffleCount
        );
        return awardEntities.stream()
                .map(award -> toActivityAwardResult(award, context))
                .toList();
    }

    @Override
    public ActivityAccountResult queryActivityAccount(ActivityAwardListCommand command) {
        ActivityAwardListCommand validCommand = Objects.requireNonNull(command, "command must not be null");
        String userId = validCommand.userId();
        Long activityId = validCommand.activityId();

        ActivityAccountEntity totalAccount = activityRepository.queryActivityAccountByUserId(userId, activityId);
        if (totalAccount == null) {
            return ActivityAccountResult.zero();
        }

        ActivityAccountMonthEntity monthAccount = activityRepository.queryActivityAccountMonthByUserId(userId, activityId, YearMonth.now());
        ActivityAccountDayEntity dayAccount = activityRepository.queryActivityAccountDayByUserId(userId, activityId, LocalDate.now());

        return new ActivityAccountResult(
                Objects.requireNonNullElse(totalAccount.totalCount(), 0),
                Objects.requireNonNullElse(totalAccount.totalCountSurplus(), 0),
                dayAccount == null ? Objects.requireNonNullElse(totalAccount.dayCount(), 0)
                        : Objects.requireNonNullElse(dayAccount.dayCount(), 0),
                dayAccount == null ? Objects.requireNonNullElse(totalAccount.dayCountSurplus(), 0)
                        : Objects.requireNonNullElse(dayAccount.dayCountSurplus(), 0),
                monthAccount == null ? Objects.requireNonNullElse(totalAccount.monthCount(), 0)
                        : Objects.requireNonNullElse(monthAccount.monthCount(), 0),
                monthAccount == null ? Objects.requireNonNullElse(totalAccount.monthCountSurplus(), 0)
                        : Objects.requireNonNullElse(monthAccount.monthCountSurplus(), 0)
        );
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
     * 将策略奖品和用户维度判定上下文转换为活动页展示结果。
     *
     * @param award   策略奖品配置
     * @param context 当前活动和用户的奖品展示上下文
     * @return 活动奖品展示结果
     * @throws BusinessException 奖品配置了次数锁但缺少判定结果时抛出
     */
    private ActivityAwardResult toActivityAwardResult(StrategyAwardEntity award, AwardDisplayContext context) {
        Integer lockCount = context.lockCountMap().get(award.awardId());

        AwardUnlockResultVO unlockResult = null;
        if (lockCount != null) {
            unlockResult = context.unlockResultMap().get(lockCount);
            if (unlockResult == null) {
                throw BusinessException.of(BusinessResponseCode.ACTIVITY_CONFIG_INVALID, context.activityId(), award.awardId(), lockCount);
            }
        }

        boolean unlocked = unlockResult == null || unlockResult.unlocked();

        int waitUnlockCount = unlockResult == null ? 0 : unlockResult.remainingCount();

        Integer weightCount = context.weightCountMap().get(award.awardId());

        boolean weightUnlocked = weightCount == null || context.userRaffleCount() >= weightCount;

        int waitWeightCount = weightUnlocked ? 0 : Math.toIntExact(weightCount - context.userRaffleCount());

        return new ActivityAwardResult(
                award.awardId(),
                award.awardTitle(),
                award.awardSubtitle(),
                award.sort(),
                lockCount,
                context.unlockScope().getCode(),
                unlocked,
                waitUnlockCount,
                weightCount,
                weightUnlocked,
                waitWeightCount
        );
    }

    /**
     * 批量查询活动奖品列表中的次数锁门槛。
     *
     * <p>先从策略奖品配置中筛选包含 {@link RuleModel#LOCK} 的奖品，
     * 再通过策略仓储一次查询对应的奖品级规则。</p>
     *
     * @param strategyId 当前活动关联的策略标识
     * @param awards     当前策略的奖品配置
     * @return 奖品标识到解锁次数的不可变映射
     */
    private Map<Long, Integer> queryRuleLockCountMap(Long strategyId, List<StrategyAwardEntity> awards) {
        List<Long> lockAwardIds = awards.stream()
                .filter(this::containsLockRule)
                .map(StrategyAwardEntity::awardId).distinct().toList();

        if (lockAwardIds.isEmpty()) {
            return Map.of();
        }

        return strategyRepository.queryAwardRuleLockCounts(strategyId, lockAwardIds);
    }

    /**
     * 判断策略奖品是否配置次数锁规则。
     *
     * @param award 策略奖品配置
     * @return 包含 {@link RuleModel#LOCK} 时返回 {@code true}
     */
    private boolean containsLockRule(StrategyAwardEntity award) {
        return new StrategyAwardRuleModelVO(
                award.ruleModels()
        ).toModelList().contains(RuleModel.LOCK);
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

    /**
     * 查询策略权重规则，并反向整理为“奖品标识 -> 最低权重门槛”。
     * <p>
     * 权重规则是策略级规则，配置格式示例：
     * {@code 4000:100011/100012;5000:100013}。
     * 前端奖品列表需要按奖品展示门槛，所以这里会把规则反转为：
     * {@code 100011 -> 4000, 100012 -> 4000, 100013 -> 5000}。
     * 如果同一个奖品出现在多个权重档位，取最低门槛，表示用户最早达到多少次后可进入包含该奖品的权重奖池。
     *
     * @param strategyId 策略标识
     * @return 奖品标识到最低权重门槛的映射；没有权重规则时返回空 Map
     */
    private Map<Long, Integer> queryRuleWeightCountMap(Long strategyId) {
        StrategyRuleEntity weightRule = strategyRepository.getStrategyRule(strategyId, RuleModel.WEIGHT);
        if (weightRule == null) {
            return Map.of();
        }
        Map<Long, Integer> awardWeightCountMap = new HashMap<>();
        weightRule.ruleWeightValues().forEach((weightCount, awardIds) -> {
            int parsedWeightCount = Integer.parseInt(weightCount);
            awardIds.forEach(awardId -> awardWeightCountMap.merge(awardId, parsedWeightCount, Math::min));
        });

        return Map.copyOf(awardWeightCountMap);
    }

    /**
     * 批量查询当前活动奖品门槛对应的解锁结果。
     *
     * <p>先对门槛去重，再通过领域服务执行一次中奖记录统计，
     * 防止奖品数量增加后产生 N+1 数据库查询。</p>
     *
     * @param userId       用户标识
     * @param activity     当前活动配置
     * @param lockCountMap 奖品标识与解锁门槛的映射
     * @return 解锁门槛与判定结果的不可变映射
     */
    private Map<Integer, AwardUnlockResultVO> queryAwardUnlockResultMap(String userId,
                                                                        ActivityEntity activity,
                                                                        Map<Long, Integer> lockCountMap) {
        Set<Integer> requiredCounts = lockCountMap.values().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());

        if (requiredCounts.isEmpty()) {
            return Map.of();
        }

        AwardUnlockCountQueryVO queryVO = new AwardUnlockCountQueryVO(userId,
                activity.activityId(),
                activity.awardUnlockScope(),
                LocalDate.now()
        );

        return awardUnlockService.evaluateUnlocks(queryVO, requiredCounts);
    }

    /**
     * 活动奖品列表单次查询所需的共享上下文。
     *
     * <p>使用上下文对象避免结果组装方法出现过多参数，同时保证
     * 所有奖品共享同一批解锁结果和权重统计结果。</p>
     *
     * @param activityId      活动标识
     * @param unlockScope     奖品解锁次数统计范围
     * @param lockCountMap    奖品标识到解锁门槛的映射
     * @param unlockResultMap 解锁门槛到用户判定结果的映射
     * @param weightCountMap  奖品标识到权重门槛的映射
     * @param userRaffleCount 用户在当前策略下的累计抽奖次数
     */
    private record AwardDisplayContext(
            Long activityId,
            AwardUnlockScopeVO unlockScope,
            Map<Long, Integer> lockCountMap,
            Map<Integer, AwardUnlockResultVO> unlockResultMap,
            Map<Long, Integer> weightCountMap,
            long userRaffleCount
    ) {
    }
}
