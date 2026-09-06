package com.lavyoung.marketforge.domain.strategy.service.rule.chain;

/**
 * 抽奖责任链装配接口。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/06
 */
public interface ILogicChainArmory {

    /**
     * 将指定节点追加为当前节点的直接后继。
     *
     * @param next 待追加的责任链节点
     * @return 已追加的后继节点，便于继续链式装配
     */
    ILogicChain appendNex(ILogicChain next);

    /**
     * 获取当前节点的直接后继。
     *
     * @return 后继节点；尚未装配时返回 {@code null}
     */
    ILogicChain next();
}
