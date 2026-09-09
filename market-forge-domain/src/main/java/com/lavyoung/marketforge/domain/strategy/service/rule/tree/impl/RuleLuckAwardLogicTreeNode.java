package com.lavyoung.marketforge.domain.strategy.service.rule.tree.impl;

import com.lavyoung.marketforge.domain.strategy.model.vo.RuleLogicCheckTypeVO;
import com.lavyoung.marketforge.domain.strategy.service.rule.tree.ILogicTreeNode;
import com.lavyoung.marketforge.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import com.lavyoung.marketforge.types.common.Constants;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * 幸运奖兜底规则树节点。
 * <p>
 * 将 {@code 奖品ID:数量范围} 格式的节点配置转换为最终兜底奖品。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/06
 */
@Slf4j
@Component("rule_luck_award")
public class RuleLuckAwardLogicTreeNode implements ILogicTreeNode {

    /**
     * {@inheritDoc}
     */
    @Override
    public DefaultTreeFactory.TreeActionEntity logic(String userId, Long strategyId, Long awardId, String ruleValue) {
        if (StringUtils.isBlank(ruleValue)) {
            log.error("抽奖策略-规则树：策略绑定的幸运奖规则值为空 userId={} strategyId={} ruleModel={}",
                    userId, strategyId, ruleModel());
            throw new BusinessException(BusinessResponseCode.STRATEGY_RULE_VALUE_INVALID);
        }
        String[] luckAwardValue = ruleValue.split(Constants.COLON, -1);
        if (luckAwardValue.length != 2 || StringUtils.isAnyBlank(luckAwardValue)) {
            log.error("抽奖策略-规则树：策略绑定的幸运奖规则值格式不合法 userId={} strategyId={} ruleModel={} ruleValue={}",
                    userId, strategyId, ruleModel(), ruleValue);
            throw new BusinessException(BusinessResponseCode.STRATEGY_RULE_VALUE_INVALID);
        }
        Long luckAwardId;
        try {
            luckAwardId = Long.valueOf(luckAwardValue[0]);
        } catch (NumberFormatException exception) {
            log.error("抽奖策略-规则树：幸运奖奖品ID不合法 userId={} strategyId={} ruleValue={}",
                    userId, strategyId, ruleValue);
            throw new BusinessException(BusinessResponseCode.STRATEGY_RULE_VALUE_INVALID, exception);
        }
        log.info("抽奖策略-规则树，兜底奖品 userId={} strategyId={} ruleModel={} ruleValue={}",
                userId, strategyId, ruleModel(), ruleValue);
        return DefaultTreeFactory.TreeActionEntity
                .builder()
                .strategyAwardVO(DefaultTreeFactory.StrategyAwardVO.builder()
                        .awardId(luckAwardId)
                        .ruleModel(ruleModel())
                        .awardRuleValue(luckAwardValue[1])
                        .build())
                .ruleLogicCheckTypeVO(RuleLogicCheckTypeVO.TAKE_OVER)
                .build();
    }

    @Override
    public RuleModel ruleModel() {
        return RuleModel.LUCK_AWARD;
    }
}
