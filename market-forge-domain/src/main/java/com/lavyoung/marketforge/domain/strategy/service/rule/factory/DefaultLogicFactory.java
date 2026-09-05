package com.lavyoung.marketforge.domain.strategy.service.rule.factory;

import com.lavyoung.marketforge.domain.strategy.annotation.LogicStrategy;
import com.lavyoung.marketforge.domain.strategy.model.entity.RuleActionEntity;
import com.lavyoung.marketforge.domain.strategy.service.rule.ILogicFilter;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认规则过滤器工厂。
 * <p>
 * 收集 Spring 容器中的规则过滤器，并依据 {@link LogicStrategy} 声明的规则模型建立索引。
 *
 * @author lavyoung
 * @version 1.0.0
 * @email lavyoung1325@outlook.com
 * @date 2026/09/04
 */
@Service
public class DefaultLogicFactory {

    /**
     * 规则模型与过滤器实例的映射。
     */
    public Map<RuleModel, ILogicFilter<?>> logicFilterMap = new ConcurrentHashMap<>();

    /**
     * 创建规则过滤器工厂并完成过滤器注册。
     *
     * @param logicFilters Spring 容器中发现的规则过滤器列表
     */
    public DefaultLogicFactory(List<ILogicFilter<?>> logicFilters) {
        logicFilters.forEach(logicFilter -> {
            LogicStrategy annotation = AnnotationUtils.findAnnotation(logicFilter.getClass(), LogicStrategy.class);
            if (null != annotation) {
                logicFilterMap.put(annotation.logicModel(), logicFilter);
            }
        });
    }

    /**
     * 获取指定执行阶段对应的规则过滤器映射。
     * <p>
     * 调用方应确保请求的结果数据类型与已注册过滤器的实际类型一致。
     *
     * @param <T> 规则执行阶段对应的结果数据类型
     * @return 规则模型与过滤器的映射
     */
    public <T extends RuleActionEntity.RaffleEntity> Map<RuleModel, ILogicFilter<T>> openLogicFilter() {
        return (Map<RuleModel, ILogicFilter<T>>) (Map<?, ?>) logicFilterMap;
    }

}
