package com.lavyoung.marketforge.domain.activity.service.quota.rule;

import lombok.extern.slf4j.Slf4j;

/**
 * 活动规则责任链抽象节点。
 * <p>
 * 提供责任链后继节点的编排能力，具体规则节点只需关注自身业务校验逻辑。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/20
 */
@Slf4j
public abstract class AbstractActionChain implements IActionChain {

    private IActionChain next;

    @Override
    public IActionChain next() {
        return next;
    }

    @Override
    public IActionChain appendNext(IActionChain next) {
        this.next = next;
        return next;
    }
}
