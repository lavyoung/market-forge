package com.lavyoung.marketforge.domain.strategy.service.rule.filter;

import com.lavyoung.marketforge.domain.strategy.model.entity.RuleActionEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.RuleMatterEntity;

/**
 * 旧版抽奖规则过滤器契约。
 * <p>
 * 当前抽奖前置规则使用责任链表达执行顺序，奖品命中后的规则使用规则树表达分支决策。
 * 本接口仅为已有调用方保留兼容能力，不再承载新规则。
 *
 * @param <T> 规则执行阶段对应的结果数据类型
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/04
 * @deprecated 新规则应根据执行阶段实现责任链节点或规则树节点
 */
@Deprecated(since = "1.0.0", forRemoval = false)
public interface ILogicFilter<T extends RuleActionEntity.RaffleEntity> {

    /**
     * 根据规则物料执行过滤判断。
     *
     * @param ruleMatterEntity 规则判断所需的用户、策略、奖品和规则信息
     * @return 规则动作结果
     */
    RuleActionEntity<T> filter(RuleMatterEntity ruleMatterEntity);
}
