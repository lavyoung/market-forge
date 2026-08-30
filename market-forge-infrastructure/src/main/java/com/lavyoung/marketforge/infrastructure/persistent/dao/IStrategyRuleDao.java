package com.lavyoung.marketforge.infrastructure.persistent.dao;

import com.lavyoung.marketforge.infrastructure.persistent.po.StrategyRulePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 抽奖策略规则数据访问接口。
 *
 * @author lavyoung
 * @version 1.0.0-SNAPSHOT
 * @email lavyoung1325@outlook.com
 */
@Mapper
public interface IStrategyRuleDao {

    /**
     * 查询指定策略配置的全部规则。
     *
     * @param strategyId 策略标识
     * @return 策略规则列表；不存在时返回空列表
     */
    List<StrategyRulePO> queryStrategyRuleList(@Param("strategyId") Long strategyId);

    /**
     * 根据策略、奖品和规则模型查询规则。
     *
     * @param strategyId 策略标识
     * @param awardId 奖品标识；查询策略级规则时为 {@code null}
     * @param ruleModel 规则模型标识
     * @return 策略规则持久化对象；不存在时返回空
     */
    Optional<StrategyRulePO> queryStrategyRule(
            @Param("strategyId") Long strategyId,
            @Param("awardId") Long awardId,
            @Param("ruleModel") String ruleModel);

    /**
     * 新增策略规则。
     *
     * @param strategyRule 策略规则持久化对象
     * @return 受影响的记录数
     */
    int insert(StrategyRulePO strategyRule);

    /**
     * 根据自增标识更新策略规则。
     *
     * @param strategyRule 包含更新内容的策略规则持久化对象
     * @return 受影响的记录数
     */
    int updateById(StrategyRulePO strategyRule);

}
