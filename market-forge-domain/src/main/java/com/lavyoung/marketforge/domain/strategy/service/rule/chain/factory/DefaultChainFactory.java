package com.lavyoung.marketforge.domain.strategy.service.rule.chain.factory;

import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyEntity;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyRepository;
import com.lavyoung.marketforge.domain.strategy.service.rule.chain.ILogicChain;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
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
     * 规则模型与责任链节点的映射。这里需要处理 todo
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
     * @throws BusinessException    策略不存在时抛出
     * @throws NullPointerException 规则节点或默认节点缺失时抛出
     */
    public ILogicChain openLogicChain(Long strategyId) {
        StrategyEntity strategyEntity = repository.queryStrategyEntityByStrategyId(strategyId);
        if (strategyEntity == null) {
            throw new BusinessException(BusinessResponseCode.STRATEGY_NOT_FOUND);
        }
        List<RuleModel> ruleModes = new ArrayList<>(strategyEntity.toRuleModes());
        if (CollectionUtils.isEmpty(ruleModes)) {
            return logicChainMap.get(RuleModel.DEFAULT);
        }
        // 排序
        ruleModes.sort(Comparator.comparingInt(RuleModel::getOrder).reversed());
        ILogicChain logicChain = logicChainMap.get(ruleModes.get(0));
        ILogicChain currentChain = logicChain;
        for (int i = 1; i < ruleModes.size(); i++) {
            ILogicChain chain = logicChainMap.get(ruleModes.get(i));
            currentChain = currentChain.appendNex(chain);
        }
        currentChain.appendNex(logicChainMap.get(RuleModel.DEFAULT));
        return logicChain;
    }
}
