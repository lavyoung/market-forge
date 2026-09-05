package com.lavyoung.marketforge.domain.strategy.service.raffle.impl;

import com.lavyoung.marketforge.domain.strategy.model.entity.RaffleFactorEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.RuleActionEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.RuleMatterEntity;
import com.lavyoung.marketforge.domain.strategy.model.vo.RuleLogicCheckTypeVO;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyRepository;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.IStrategyDispatch;
import com.lavyoung.marketforge.domain.strategy.service.raffle.AbstractRaffleStrategy;
import com.lavyoung.marketforge.domain.strategy.service.rule.ILogicFilter;
import com.lavyoung.marketforge.domain.strategy.service.rule.factory.DefaultLogicFactory;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 默认抽奖策略实现。
 * <p>
 * 优先执行黑名单规则，再依次执行其余前置规则，并在规则接管时立即返回规则结果。
 *
 * @author lavyoung
 * @version 1.0.0
 * @email lavyoung1325@outlook.com
 * @date 2026/09/05
 */
@Slf4j
@Component
public class DefaultRaffleStrategy extends AbstractRaffleStrategy {

    /**
     * 规则模型与过滤器的注册工厂。
     */
    private final DefaultLogicFactory defaultLogicFactory;

    /**
     * 创建默认抽奖策略。
     *
     * @param repository          抽奖策略仓储端口
     * @param strategyDispatch    已装配策略的随机调度服务
     * @param defaultLogicFactory 规则模型与过滤器的注册工厂
     */
    public DefaultRaffleStrategy(
            IStrategyRepository repository,
            IStrategyDispatch strategyDispatch,
            DefaultLogicFactory defaultLogicFactory) {
        super(repository, strategyDispatch);
        this.defaultLogicFactory = defaultLogicFactory;
    }

    /**
     * {@inheritDoc}
     * <p>
     * 黑名单规则拥有最高优先级；黑名单放行后，再按配置顺序执行剩余规则。
     *
     * @param factorEntity 抽奖因子
     * @param logics       待执行的前置规则模型列表
     * @return 首个接管流程的规则动作；全部规则放行时返回 {@code null}
     */
    @Override
    protected RuleActionEntity<RuleActionEntity.RaffleBeforeEntity> doCheckRaffleBeforeLogic(RaffleFactorEntity factorEntity, List<RuleModel> logics) {
        Map<RuleModel, ILogicFilter<RuleActionEntity.RaffleBeforeEntity>> logicFilterMap = defaultLogicFactory.openLogicFilter();

        // 前置
        RuleModel model = logics.stream().filter(logic -> logic.getCode().equals(RuleModel.RULE_BLACKLIST.getCode()))
                .findFirst().orElse(null);

        if (model != null) {
            ILogicFilter<RuleActionEntity.RaffleBeforeEntity> logicFilter = logicFilterMap.get(model);
            RuleMatterEntity ruleMatterEntity = RuleMatterEntity.builder()
                    .userId(factorEntity.getUserId())
                    .awardId(null) // 无奖品ID
                    .strategyId(factorEntity.getStrategyId())
                    .ruleModel(model.getCode()).build();
            RuleActionEntity<RuleActionEntity.RaffleBeforeEntity> ruleAction = logicFilter.filter(ruleMatterEntity);
            if (!RuleLogicCheckTypeVO.ALLOW.getCode().equals(ruleAction.getCode())) {
                return ruleAction;
            }
        }

        // 处理剩余的
        List<RuleModel> ruleModelList = logics.stream().filter(logic -> !RuleModel.RULE_BLACKLIST.equals(logic))
                .toList();
        for (RuleModel ruleModel : ruleModelList) {
            ILogicFilter<RuleActionEntity.RaffleBeforeEntity> logicFilter = logicFilterMap.get(ruleModel);
            RuleMatterEntity ruleMatterEntity = RuleMatterEntity.builder()
                    .userId(factorEntity.getUserId())
                    .strategyId(factorEntity.getStrategyId())
                    .awardId(null)
                    .ruleModel(ruleModel.getCode())
                    .build();
            RuleActionEntity<RuleActionEntity.RaffleBeforeEntity> ruleAction = logicFilter.filter(ruleMatterEntity);
            log.info("抽奖前规则过滤 userId={} ruleModel={} code={} info={}", factorEntity.getUserId(), ruleModel, ruleAction.getCode(), ruleAction.getMsg());
            if (!RuleLogicCheckTypeVO.ALLOW.getCode().equals(ruleAction.getCode())) {
                return ruleAction;
            }
        }
        return null;
    }
}
