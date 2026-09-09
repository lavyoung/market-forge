package com.lavyoung.marketforge.domain.strategy.service.rule.chain.factory;

import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyEntity;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyRepository;
import com.lavyoung.marketforge.domain.strategy.service.rule.chain.ILogicChain;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 默认抽奖责任链工厂。
 * <p>
 * 根据规则优先级由高到低连接策略配置的节点，并始终在链尾追加默认抽奖节点。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/06
 */
@Component
@RequiredArgsConstructor
public class DefaultChainFactory {

    /**
     * 规则模型与责任链节点实现的映射。
     */
    private final Map<RuleModel, ILogicChain> logicChainMap;

    /**
     * 查询策略规则配置的仓储端口。
     */
    private final IStrategyRepository repository;


    /**
     * 根据策略配置装配责任链并返回头节点。
     *
     * @param strategyId 策略标识
     * @return 已装配责任链的头节点；策略无规则时返回默认抽奖节点
     * @throws BusinessException 策略不存在或配置的责任链节点未注册时抛出
     */
    public ILogicChain openLogicChain(Long strategyId) {
        StrategyEntity strategyEntity = repository.queryStrategyEntityByStrategyId(strategyId);
        if (strategyEntity == null) {
            throw new BusinessException(BusinessResponseCode.STRATEGY_NOT_FOUND);
        }
        List<RuleModel> ruleModes = new ArrayList<>(strategyEntity.toRuleModes());
        if (CollectionUtils.isEmpty(ruleModes)) {
            return requireChain(RuleModel.DEFAULT);
        }
        ruleModes.sort(Comparator.comparingInt(RuleModel::getOrder).reversed());
        ILogicChain logicChain = requireChain(ruleModes.get(0));
        ILogicChain currentChain = logicChain;
        for (int i = 1; i < ruleModes.size(); i++) {
            ILogicChain chain = requireChain(ruleModes.get(i));
            currentChain = currentChain.appendNex(chain);
        }
        currentChain.appendNex(requireChain(RuleModel.DEFAULT));
        return logicChain;
    }

    /**
     * 获取已注册的责任链节点。
     *
     * @param ruleModel 规则模型
     * @return 对应责任链节点
     * @throws BusinessException 节点未注册时抛出
     */
    private ILogicChain requireChain(RuleModel ruleModel) {
        ILogicChain chain = logicChainMap.get(ruleModel);
        if (chain == null) {
            throw new BusinessException(
                    BusinessResponseCode.STRATEGY_RULE_NOT_FOUND,
                    "未注册抽奖责任链节点: " + ruleModel.getCode()
            );
        }
        return chain;
    }


    /**
     * 责任链执行结果。
     *
     * @param awardId   责任链选中的奖品标识
     * @param ruleModel 最终命中奖品的规则模型
     */
    @Builder
    public record StrategyAwardVO(
            Long awardId,
            RuleModel ruleModel
    ) {

    }
}
