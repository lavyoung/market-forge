package com.lavyoung.marketforge.domain.strategy.service.rule.impl;

import com.lavyoung.marketforge.domain.strategy.annotation.LogicStrategy;
import com.lavyoung.marketforge.domain.strategy.model.entity.RuleActionEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.RuleMatterEntity;
import com.lavyoung.marketforge.domain.strategy.model.vo.RuleLogicCheckTypeVO;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyRepository;
import com.lavyoung.marketforge.domain.strategy.service.rule.ILogicFilter;
import com.lavyoung.marketforge.types.common.Constants;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * 黑名单前置规则过滤器。
 * <p>
 * 当用户命中规则配置中的黑名单时接管抽奖流程，并返回规则指定的奖品。
 *
 * @author lavyoung
 * @version 1.0.0
 * @email lavyoung1325@outlook.com
 * @date 2026/09/05
 */
@Slf4j
@Component
@RequiredArgsConstructor
@LogicStrategy(logicModel = RuleModel.RULE_BLACKLIST)
public class RuleBlackListLogicFilter implements ILogicFilter<RuleActionEntity.RaffleBeforeEntity> {

    /**
     * 抽奖策略仓储端口。
     */
    private final IStrategyRepository repository;

    /**
     * 根据黑名单配置判断当前用户是否需要由规则接管。
     *
     * @param ruleMatterEntity 包含用户、策略及规则模型的规则物料
     * @return 命中黑名单时返回接管动作，否则返回放行动作
     * @throws IllegalArgumentException       黑名单配置中的奖品标识不是有效整数时抛出
     * @throws ArrayIndexOutOfBoundsException 黑名单配置不符合“奖品标识:用户列表”格式时抛出
     */
    @Override
    public RuleActionEntity<RuleActionEntity.RaffleBeforeEntity> filter(RuleMatterEntity ruleMatterEntity) {
        log.info("规则过滤-黑名单 userId={}, strategyId={}, ruleModel={}", ruleMatterEntity.getUserId(), ruleMatterEntity.getStrategyId(), ruleMatterEntity.getRuleModel());
        // 查询规则值
        String ruleValue = repository.queryStrategyRuleValue(ruleMatterEntity.getStrategyId(), ruleMatterEntity.getAwardId(), ruleMatterEntity.getRuleModel());
        if (StringUtils.isNotBlank(ruleValue)) {
            // 一般是这样的格式 100:user1/user002
            String[] splitRuleValue = ruleValue.split(Constants.COLON);
            long awardId = Long.parseLong(splitRuleValue[0]);
            String[] blackUserIds = splitRuleValue[1].split(Constants.SPLIT);
            for (String blackUserId : blackUserIds) {
                if (ruleMatterEntity.getUserId().equals(blackUserId)) {
                    return RuleActionEntity.<RuleActionEntity.RaffleBeforeEntity>builder()
                            .ruleModel(RuleModel.RULE_BLACKLIST.getCode())
                            .code(RuleLogicCheckTypeVO.TAKE_OVER.getCode())
                            .msg(RuleLogicCheckTypeVO.TAKE_OVER.getInfo())
                            .data(RuleActionEntity.RaffleBeforeEntity.builder()
                                    .strategyId(ruleMatterEntity.getStrategyId())
                                    .awardId(awardId)
                                    .build()
                            ).build();
                }
            }
        }
        // 直接返回 进行下一个规则过滤执行
        return RuleActionEntity.<RuleActionEntity.RaffleBeforeEntity>builder()
                .code(RuleLogicCheckTypeVO.ALLOW.getCode())
                .msg(RuleLogicCheckTypeVO.ALLOW.getInfo())
                .build();
    }
}
