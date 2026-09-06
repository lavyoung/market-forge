package com.lavyoung.marketforge.domain.strategy.service.rule.chain.impl;

import com.lavyoung.marketforge.domain.strategy.repository.IStrategyRepository;
import com.lavyoung.marketforge.domain.strategy.service.rule.chain.AbstractLogicChain;
import com.lavyoung.marketforge.types.common.Constants;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
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
     * @param userId     参与抽奖的用户标识
     * @param strategyId 抽奖策略标识
     * @return 命中黑名单时返回指定奖品标识，否则返回后继节点结果
     * @throws IllegalArgumentException       黑名单奖品标识不是有效整数时抛出
     * @throws ArrayIndexOutOfBoundsException 黑名单配置不符合“奖品标识:用户列表”格式时抛出
     * @throws NullPointerException           未命中规则且未装配后继节点时抛出
     */
    @Override
    public Long logic(String userId, Long strategyId) {
        log.info("抽奖责任链-黑名单处理开始 userId={} strategyId={} ruleModel={} ", userId, strategyId, ruleModel());
        String ruleValueStr = repository.queryStrategyRuleValue(strategyId, RuleModel.RULE_BLACKLIST.getCode());
        if (StringUtils.isBlank(ruleValueStr)) {
            return next().logic(userId, strategyId);
        }
        // 一般是这样的格式 100:user1/user002
        String[] splitRuleValue = ruleValueStr.split(Constants.COLON);
        long awardId = Long.parseLong(splitRuleValue[0]);
        String[] blackUserIds = splitRuleValue[1].split(Constants.SPLIT);
        for (String blackUserId : blackUserIds) {
            if (userId.equals(blackUserId)) {
                return awardId;
            }
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
