package com.lavyoung.marketforge.domain.strategy.service.rule;

import com.lavyoung.marketforge.domain.strategy.model.entity.RuleActionEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.RuleMatterEntity;

/**
 * 抽奖规则过滤器。
 *
 * @param <T> 规则执行阶段对应的结果数据类型
 * @author lavyoung
 * @version 1.0.0
 * @email lavyoung1325@outlook.com
 * @date 2026/09/04
 */
public interface ILogicFilter<T extends RuleActionEntity.RaffleEntity> {

    /**
     * 根据规则物料执行过滤判断。
     *
     * @param ruleMatterEntity 规则判断所需的用户、策略、奖品和规则信息
     * @return 规则动作结果
     */
    RuleActionEntity<T> filter(RuleMatterEntity ruleMatterEntity);
}
