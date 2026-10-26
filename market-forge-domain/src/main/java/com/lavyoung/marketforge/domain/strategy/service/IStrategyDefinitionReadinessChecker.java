package com.lavyoung.marketforge.domain.strategy.service;

import com.lavyoung.marketforge.domain.strategy.model.aggregate.StrategyDefinitionAggregate;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyReadinessReportVO;

/**
 * 策略定义发布就绪检查器。
 *
 * <p>负责以只读方式检查完整策略定义，将阻断发布的问题、运营风险、
 * 配置建议以及正式抽奖概率预览统一组织成检查报告。</p>
 *
 * <p>该服务不修改策略状态、不写入数据库、不刷新 Redis 概率快照。
 * 检查通过只表示当前配置满足发布前置条件，真正发布仍由应用服务负责
 * 编排概率快照刷新和状态条件更新。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
public interface IStrategyDefinitionReadinessChecker {

    /**
     * 检查策略定义是否满足发布条件。
     *
     * <p>配置类业务异常会转换成报告中的错误项，不直接向调用方抛出。
     * 数据库或其他基础设施不可用属于系统故障，仍然向上抛出，避免将
     * 环境故障错误展示成运营配置问题。</p>
     *
     * @param aggregate 待检查的完整策略定义聚合
     * @return 包含错误、警告、建议和概率预览的检查报告
     * @throws NullPointerException 待检查聚合为空时抛出
     * @throws RuntimeException     奖品定义仓储等依赖发生系统故障时抛出
     */
    StrategyReadinessReportVO check(StrategyDefinitionAggregate aggregate);
}
