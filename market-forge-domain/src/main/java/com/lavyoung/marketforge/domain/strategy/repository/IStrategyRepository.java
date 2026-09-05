package com.lavyoung.marketforge.domain.strategy.repository;

import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyAwardEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyRuleEntity;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;

import java.util.List;
import java.util.Map;

/**
 * 抽奖策略仓储端口。
 * <p>
 * 为策略领域服务提供策略、规则和奖品配置的读取能力，并负责持久化装配后的
 * 概率范围及奖品查找表。具体的数据来源和缓存策略由基础设施层实现。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
public interface IStrategyRepository {

    /**
     * 查询指定抽奖策略下参与抽奖的全部奖品配置。
     *
     * @param strategyId 策略标识
     * @return 策略奖品配置列表；不存在配置时返回空列表，不返回 {@code null}
     */
    List<StrategyAwardEntity> queryStrategyAwardList(Long strategyId);

    /**
     * 保存装配完成的概率范围和奖品查找表。
     * <p>
     * {@code key} 可以是普通策略标识，也可以是包含权重规则值的策略装配键；
     * 查找表的键为从 {@code 0} 到 {@code rateRange - 1} 的概率下标，值为奖品标识。
     *
     * @param key                              策略装配键
     * @param rateRange                        随机取值范围，即查找表的有效下标数量
     * @param shuffleStrategyAwardSearchTables 概率下标到奖品标识的乱序映射
     */
    void storeStrategyAwardSearchTables(String key, Integer rateRange, Map<Integer, Long> shuffleStrategyAwardSearchTables);

    /**
     * 获取指定策略装配后的随机取值范围。
     *
     * @param strategyId 策略标识
     * @return 查找表的有效下标数量；策略尚未装配时返回 {@code 0}
     */
    int getRateRange(Long strategyId);

    /**
     * 从指定策略装配键对应的概率查找表中获取奖品标识。
     * <p>
     * 装配键既可以表示普通策略，也可以表示某个权重门槛下的策略。
     *
     * @param key     策略装配键
     * @param rateKey 概率查找表下标
     * @return 奖品标识；查找表中不存在对应下标时返回 {@code 0}
     */
    long getStrategyAwardAssemble(String key, int rateKey);

    /**
     * 查询指定策略的基础配置。
     *
     * @param strategyId 策略标识
     * @return 策略实体；策略不存在时返回 {@code null}
     */
    StrategyEntity queryStrategyEntityByStrategyId(Long strategyId);

    /**
     * 查询指定策略下某一规则模型的规则配置。
     *
     * @param strategyId 策略标识
     * @param ruleModel  需要查询的规则模型
     * @return 策略规则实体；规则不存在时返回 {@code null}
     */
    StrategyRuleEntity getStrategyRule(Long strategyId, RuleModel ruleModel);

    /**
     * 查询策略级或奖品级规则的配置值。
     * <p>
     * 当 {@code awardId} 为空时查询策略级规则，否则同时按奖品标识过滤。
     *
     * @param strategyId 策略标识
     * @param awardId    奖品标识；查询策略级规则时可为空
     * @param ruleModel  规则模型编码
     * @return 规则配置值；未找到匹配规则时返回 {@code null}
     */
    String queryStrategyRuleValue(Long strategyId, Long awardId, String ruleModel);
}
