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

    List<StrategyRulePO> queryStrategyRuleList(@Param("strategyId") Long strategyId);

    Optional<StrategyRulePO> queryStrategyRule(
            @Param("strategyId") Long strategyId,
            @Param("awardId") Long awardId,
            @Param("ruleModel") String ruleModel);

    int insert(StrategyRulePO strategyRule);

    int updateById(StrategyRulePO strategyRule);

}
