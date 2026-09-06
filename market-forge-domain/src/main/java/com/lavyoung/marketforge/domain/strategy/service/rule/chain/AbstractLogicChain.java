package com.lavyoung.marketforge.domain.strategy.service.rule.chain;

import com.lavyoung.marketforge.types.domain.strategy.RuleModel;

/**
 * 抽奖规则责任链抽象基类。
 * <p>
 * 统一维护后继节点，具体节点负责执行规则并决定返回结果或继续传递请求。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/06
 */
public abstract class AbstractLogicChain implements ILogicChain {

    /**
     * 当前节点的后继责任链节点。
     */
    private ILogicChain next;

    /**
     * {@inheritDoc}
     */
    @Override
    public ILogicChain appendNex(ILogicChain next) {
        this.next = next;
        return this.next;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ILogicChain next() {
        return this.next;
    }

    /**
     * 获取当前责任链节点处理的规则模型。
     *
     * @return 当前节点对应的规则模型
     */
    protected abstract RuleModel ruleModel();
}
