package com.lavyoung.marketforge.domain.strategy.service.rule.tree;

import com.lavyoung.marketforge.domain.strategy.model.entity.RaffleFactorEntity;
import com.lavyoung.marketforge.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import com.lavyoung.marketforge.types.exception.BusinessException;

/**
 * 规则树节点执行契约。
 *
 * <p>所有规则树节点共享同一个抽奖因子，使次数锁、库存锁和后续扩展规则
 * 能够读取本次抽奖对应的用户、活动、策略、奖品及统计范围。</p>
 *
 * <p>节点只能读取抽奖上下文并返回决策结果，不应修改输入对象。
 * {@link RaffleFactorEntity} 使用不可变 record，避免节点之间相互污染状态。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/06
 */
public interface ILogicTreeNode {

    /**
     * 执行当前规则节点。
     *
     * @param raffleFactorEntity 本次抽奖的完整领域上下文，包含用户、活动、
     *                           策略、候选奖品和奖品解锁统计范围
     * @param ruleValue          当前规则树节点配置值；具体格式由节点实现解释
     * @return 当前节点的放行或接管结果，以及节点最终选择的奖品
     * @throws NullPointerException 抽奖因子为空时抛出
     * @throws BusinessException    规则配置非法或节点无法完成业务判断时抛出
     */
    DefaultTreeFactory.TreeActionEntity logic(RaffleFactorEntity raffleFactorEntity, String ruleValue);

    /**
     * 获取当前节点对应的规则模型。
     *
     * @return 当前节点唯一对应的规则模型
     */
    RuleModel ruleModel();
}
