package com.lavyoung.marketforge.domain.strategy.repository;

import com.lavyoung.marketforge.domain.strategy.model.StrategyAwardEntity;
import com.lavyoung.marketforge.domain.strategy.model.StrategyEntity;
import com.lavyoung.marketforge.domain.strategy.model.StrategyRuleEntity;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;

import java.util.List;
import java.util.Map;

/**
 * 策略仓储接口
 *
 * @author lavyoung
 * @version 1.0.0
 * @email lavyoung1325@outlook.com
 */
public interface IStrategyRepository {

    /**
     * 查询指定策略下配置的全部奖品。
     *
     * @param strategyId 策略标识
     * @return 策略奖品列表；不存在配置时返回空列表
     */
    List<StrategyAwardEntity> queryStrategyAwardList(Long strategyId);

    /**
     * 将策略概率范围及乱序后的奖品查找表存储到 Redis。
     *
     * @param key                              策略标识
     * @param rateRange                        概率查找范围
     * @param shuffleStrategyAwardSearchTables 下标到奖品标识的乱序查找表
     */
    void storeStrategyAwardSearchTables(String key, Integer rateRange, Map<Integer, Long> shuffleStrategyAwardSearchTables);

    /**
     * 获取指定策略装配后的随机数范围。
     *
     * @param strategyId 策略标识
     * @return 随机数范围；策略尚未装配时返回 {@code 0}
     */
    int getRateRange(Long strategyId);

    /**
     * 根据概率查找表下标获取对应的奖品标识。
     *
     * @param strategyId 策略标识
     * @param rateKey    概率查找表下标
     * @return 奖品标识；查找表中不存在对应下标时返回 {@code 0}
     */
    long getStrategyAwardAssemble(Long strategyId, int rateKey);

    /**
     * 策略
     *
     * @param strategyId 策略id
     * @return 策略基本信息
     */
    StrategyEntity queryStrategyEntityByStrategyId(Long strategyId);

    /**
     * 查询策略规则详情
     *
     * @param strategyId 规则id
     * @param ruleModel  规则模型
     * @return 规则详情
     */
    StrategyRuleEntity getStrategyRule(Long strategyId, RuleModel ruleModel);
}
