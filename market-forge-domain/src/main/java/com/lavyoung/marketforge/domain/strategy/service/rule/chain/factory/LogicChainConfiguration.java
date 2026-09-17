package com.lavyoung.marketforge.domain.strategy.service.rule.chain.factory;

import com.lavyoung.marketforge.domain.strategy.service.rule.chain.ILogicChain;
import com.lavyoung.marketforge.domain.strategy.service.rule.chain.impl.BlackListLogicChain;
import com.lavyoung.marketforge.domain.strategy.service.rule.chain.impl.DefaultRuleChain;
import com.lavyoung.marketforge.domain.strategy.service.rule.chain.impl.WeightLogicChain;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 *
 * 抽奖前置责任链节点注册配置。
 *
 * <p>将 Spring 管理的责任链节点按照领域规则模型注册，
 * 供 {@link DefaultChainFactory} 根据策略配置完成责任链编排。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/17
 */
@Configuration(proxyBeanMethods = false)
public class LogicChainConfiguration {

    /**
     * 注册当前系统支持的责任链节点。
     *
     * @param blackListLogicChain 黑名单责任链节点
     * @param weightLogicChain    权重责任链节点
     * @param defaultRuleChain    默认抽奖责任链节点
     * @return 规则模型与责任链节点之间的不可变映射
     */
    @Bean
    public Map<RuleModel, ILogicChain> logicChainMap(BlackListLogicChain blackListLogicChain, WeightLogicChain weightLogicChain,
                                                     DefaultRuleChain defaultRuleChain) {
        return Map.of(RuleModel.RULE_BLACKLIST, blackListLogicChain, RuleModel.WEIGHT, weightLogicChain, RuleModel.DEFAULT, defaultRuleChain);
    }
}
