package com.lavyoung.marketforge.domain.activity.service.quota.rule;

/**
 * 活动规则责任链装配接口。
 * <p>
 * 定义责任链节点之间的后继关系，供工厂按业务顺序组装规则节点。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/20
 */
public interface IActionChainArmory {

    /**
     * 获取当前节点的后继节点。
     *
     * @return 后继规则节点；不存在时返回 null
     */
    IActionChain next();

    /**
     * 追加后继规则节点。
     *
     * @param next 后继规则节点
     * @return 被追加的后继规则节点
     */
    IActionChain appendNext(IActionChain next);
}
