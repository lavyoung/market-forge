package com.lavyoung.marketforge.domain.strategy.service.armorcy.impl;

import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyAwardEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyRuleEntity;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyRepository;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.IStrategyArmory;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.IStrategyDispatch;
import com.lavyoung.marketforge.types.common.Constants;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.*;

/**
 * 抽奖策略装配与调度服务。
 * <p>
 * 根据奖品概率生成随机下标查找表，并支持从普通策略或权重策略查找表中随机选择奖品。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StrategyArmoryDispatch implements IStrategyArmory, IStrategyDispatch {

    private final IStrategyRepository repository;

    /**
     * {@inheritDoc}
     * <p>
     * 该实现按最小中奖概率计算查找表范围，将奖品标识按概率填充并乱序后写入仓储。
     *
     * @param strategyId 策略标识
     * @throws ArithmeticException 奖品概率无法形成有效概率范围时抛出
     */
    @Override
    public boolean assembleLotteryStrategy(Long strategyId) {
        // 1. 查询策略配置
        List<StrategyAwardEntity> strategyAwardEntities = repository.queryStrategyAwardList(strategyId);

        // 2. 组装策略查询表
        assembleLotteryStrategy(String.valueOf(strategyId), strategyAwardEntities);

        // 3. 权重策略配置
        StrategyEntity strategyEntity = repository.queryStrategyEntityByStrategyId(strategyId);
        if (strategyEntity == null || !strategyEntity.toRuleModes().contains(RuleModel.WEIGHT)) {
            return true;
        }
        StrategyRuleEntity strategyRule = repository.getStrategyRule(strategyId, RuleModel.WEIGHT);
        if (strategyRule == null) {
            throw new BusinessException(BusinessResponseCode.STRATEGY_RULE_VALUE_INVALID);
        }
        // 4. 设置权重范围查询表
        Map<String, List<Long>> ruleWeightValueMap = strategyRule.ruleWeightValues();
        for (Map.Entry<String, List<Long>> entry : ruleWeightValueMap.entrySet()) {
            List<Long> ruleWeightValues = entry.getValue();
            // 移除后重新存储
            List<StrategyAwardEntity> awardEntities = new ArrayList<>(strategyAwardEntities);
            awardEntities.removeIf(e -> !ruleWeightValues.contains(e.awardId()));
            // 存储对应的权重值
            assembleLotteryStrategy(String.valueOf(strategyId).concat(Constants.UNDERLINE) + entry.getKey(), awardEntities);
        }

        return true;
    }

    /**
     * 根据一组奖品概率生成乱序查找表，并以指定装配键写入仓储。
     *
     * @param key                   策略装配键
     * @param strategyAwardEntities 参与本次装配的奖品配置
     * @throws ArithmeticException 奖品概率无法形成有效概率范围时抛出
     */
    private void assembleLotteryStrategy(String key, List<StrategyAwardEntity> strategyAwardEntities) {
        // 1. 获取最小概率值
        BigDecimal minAwardRate = strategyAwardEntities.stream().map(StrategyAwardEntity::awardRate).min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        // 2. 获取概率值的总和
        BigDecimal totalAwardRate = strategyAwardEntities.stream().map(StrategyAwardEntity::awardRate)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. 获取概率范围
        ArrayList<Long> strategyAwardSearchTables = getStrategyAwardSearchTables(strategyAwardEntities, totalAwardRate, minAwardRate);

        // 5. 乱序
        Collections.shuffle(strategyAwardSearchTables);

        // 6.
        HashMap<Integer, Long> shuffleStrategyAwardSearchTables = new HashMap<>();
        for (int i = 0; i < strategyAwardSearchTables.size(); i++) {
            shuffleStrategyAwardSearchTables.put(i, strategyAwardSearchTables.get(i));
        }

        // 7. 存储到缓存
        repository.storeStrategyAwardSearchTables(key, shuffleStrategyAwardSearchTables.size(), shuffleStrategyAwardSearchTables);

    }

    /**
     * 按奖品概率生成未乱序的奖品查找表。
     *
     * @param strategyAwardEntities 参与装配的奖品配置
     * @param totalAwardRate        奖品概率总和
     * @param minAwardRate          最小奖品概率
     * @return 按概率重复填充奖品标识的查找表
     * @throws ArithmeticException 最小概率为零或概率无法整除时抛出
     */
    private static ArrayList<Long> getStrategyAwardSearchTables(List<StrategyAwardEntity> strategyAwardEntities, BigDecimal totalAwardRate, BigDecimal minAwardRate) {
        BigDecimal rateRange = totalAwardRate.divide(minAwardRate, 0, RoundingMode.CEILING);

        //  生成值的范围 概率查询表
        ArrayList<Long> strategyAwardSearchTables = new ArrayList<>(rateRange.intValue());
        for (StrategyAwardEntity strategyAwardEntity : strategyAwardEntities) {
            Long awardId = strategyAwardEntity.awardId();
            BigDecimal awardRate = strategyAwardEntity.awardRate();
            // 计算概率值需要存储表的数量 存储对应的奖品id
            for (int i = 0; i < rateRange.multiply(awardRate).setScale(0, RoundingMode.CEILING).intValue(); i++) {
                strategyAwardSearchTables.add(awardId);
            }
        }
        return strategyAwardSearchTables;
    }

    /**
     * {@inheritDoc}
     *
     * @param strategyId 策略标识
     * @return 随机选中的奖品标识
     * @throws IllegalArgumentException 策略尚未装配或随机数范围无效时抛出
     */
    @Override
    public long getRandomAwardId(Long strategyId) {
        return repository.getStrategyAwardAssemble(String.valueOf(strategyId), new SecureRandom().nextInt(repository.getRateRange(strategyId)));
    }

    /**
     * {@inheritDoc}
     * <p>
     * 使用“策略标识_权重规则值”作为装配键，从相应的权重概率表中查询奖品。
     *
     * @param strategyId      策略标识
     * @param ruleWeightValue 当前命中的权重规则值
     * @return 随机选中的奖品标识
     * @throws IllegalArgumentException 策略尚未装配或随机数范围无效时抛出
     */
    @Override
    public long getRandomAwardIdAndWeight(Long strategyId, String ruleWeightValue) {
        return repository.getStrategyAwardAssemble(strategyId + Constants.UNDERLINE + ruleWeightValue, new SecureRandom().nextInt(repository.getRateRange(strategyId)));
    }
}
