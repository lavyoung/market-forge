package com.lavyoung.marketforge.domain.activity.service.rule;

/**
 *
 * 动作责任链装配
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/20
 */
public interface IActionChainArmory {

    IActionChain next();

    IActionChain appendNext(IActionChain next);
}
