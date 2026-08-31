package com.lavyoung.marketforge.domain.strategy.service.armorcy;

import com.lavyoung.marketforge.domain.strategy.model.StrategyAwardEntity;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 *
 * 策略装配库 负责初始化策略计算
 *
 * @author lavyoung
 * @version 1.0.0
 * @email lavyoung1325@outlook.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StrategyArmory implements IStrategyArmory {

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
    public void assembleLotteryStrategy(Long strategyId) {
        // 1. 查询策略配置
        List<StrategyAwardEntity> strategyAwardEntities = repository.queryStrategyAwardList(strategyId);

        // 2. 获取最小概率值
        BigDecimal minAwardRate = strategyAwardEntities.stream().map(StrategyAwardEntity::getAwardRate).min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        // 3. 获取概率值的总和
        BigDecimal totalAwardRate = strategyAwardEntities.stream().map(StrategyAwardEntity::getAwardRate)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 4. 获取概率范围
        BigDecimal rateRange = totalAwardRate.divide(minAwardRate, 0, RoundingMode.CEILING);

        // 5. 生成值的范围 概率查询表
        ArrayList<Long> strategyAwardSearchTables = new ArrayList<>(rateRange.intValue());
        for (StrategyAwardEntity strategyAwardEntity : strategyAwardEntities) {
            Long awardId = strategyAwardEntity.getAwardId();
            BigDecimal awardRate = strategyAwardEntity.getAwardRate();
            // 计算概率值需要存储表的数量 存储对应的奖品id
            for (int i = 0; i < rateRange.multiply(awardRate).setScale(0, RoundingMode.CEILING).intValue(); i++) {
                strategyAwardSearchTables.add(awardId);
            }
        }

        // 6. 乱序
        Collections.shuffle(strategyAwardSearchTables);

        // 7.
        HashMap<Integer, Long> shuffleStrategyAwardSearchTables = new HashMap<>();
        for (int i = 0; i < strategyAwardSearchTables.size(); i++) {
            shuffleStrategyAwardSearchTables.put(i, strategyAwardSearchTables.get(i));
        }

        // 8. 存储到缓存
        repository.storeStrategyAwardSearchTables(strategyId, shuffleStrategyAwardSearchTables.size(), shuffleStrategyAwardSearchTables);

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
        return repository.getStrategyAwardAssemble(strategyId, new SecureRandom().nextInt(repository.getRateRange(strategyId)));
    }
}
