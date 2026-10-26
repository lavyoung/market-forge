package com.lavyoung.marketforge.application.strategy.service;

import com.lavyoung.marketforge.application.strategy.model.StrategyReadinessResult;

/**
 * 策略发布就绪检查应用服务。
 *
 * <p>负责根据策略标识读取完整策略定义，并调用领域检查器生成检查报告。
 * 本接口只暴露 Application 模型，不向 Trigger 泄漏领域聚合或值对象。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/26
 */
public interface IStrategyReadinessApplicationService {

    /**
     * 检查指定策略是否满足发布条件。
     *
     * @param strategyId 策略业务标识
     * @return 策略就绪检查结果
     * @throws NullPointerException                                       策略标识为空时抛出
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 策略不存在时抛出
     * @throws RuntimeException                                           检查依赖发生系统故障时抛出
     */
    StrategyReadinessResult check(Long strategyId);
}
