package com.lavyoung.marketforge.domain.strategy.service.rule.chain.impl;

import com.lavyoung.marketforge.domain.strategy.model.vo.BlacklistRuleValueVO;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyRepository;
import com.lavyoung.marketforge.domain.strategy.service.rule.chain.AbstractLogicChain;
import com.lavyoung.marketforge.domain.strategy.service.rule.chain.factory.DefaultChainFactory;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * 黑名单抽奖责任链节点。
 * <p>
 * 命中黑名单时直接返回规则指定奖品；未命中或未配置规则时传递给后继节点。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BlackListLogicChain extends AbstractLogicChain {

    /**
     * 查询黑名单规则配置的策略仓储端口。
     */
    private final IStrategyRepository repository;

    /**
     * 根据黑名单规则判断是否直接返回指定奖品。
     *
     * <p>规则值为空时继续执行后继节点；规则值存在时使用
     * {@link BlacklistRuleValueVO} 完成统一解析。数据库中出现非法规则值时，
     * 转换为稳定的业务异常，避免数组越界或数字转换异常泄漏到调用方。</p>
     *
     * @param userId     参与抽奖的用户标识
     * @param strategyId 抽奖策略标识
     * @return 命中黑名单时返回兜底奖品，否则返回后继节点的处理结果
     * @throws BusinessException    黑名单规则值格式非法时抛出
     * @throws NullPointerException 未命中规则且没有后继节点时抛出
     */
    @Override
    public DefaultChainFactory.StrategyAwardVO logic(String userId, Long strategyId) {
        log.info("抽奖责任链-黑名单处理开始 userId={} strategyId={} ruleModel={}", userId, strategyId, ruleModel());
        String ruleValueStr = repository.queryStrategyRuleValue(strategyId, RuleModel.RULE_BLACKLIST.getCode());
        if (StringUtils.isBlank(ruleValueStr)) {
            return next().logic(userId, strategyId);
        }
        BlacklistRuleValueVO ruleValue;
        try {
            ruleValue = BlacklistRuleValueVO.parse(ruleValueStr);
        } catch (IllegalArgumentException exception) {
            throw BusinessException.of(BusinessResponseCode.STRATEGY_RULE_VALUE_INVALID,
                    exception,
                    strategyId,
                    RuleModel.RULE_BLACKLIST.getCode(),
                    ruleValueStr);
        }
        if (ruleValue.contains(userId)) {
            return DefaultChainFactory.StrategyAwardVO.builder()
                    .awardId(ruleValue.fallbackAwardId())
                    .ruleModel(ruleModel())
                    .build();
        }
        log.info("抽奖责任链-黑名单结束放行 userId={} strategyId={} ruleModel={} ", userId, strategyId, ruleModel());
        return next().logic(userId, strategyId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected RuleModel ruleModel() {
        return RuleModel.RULE_BLACKLIST;
    }
}
