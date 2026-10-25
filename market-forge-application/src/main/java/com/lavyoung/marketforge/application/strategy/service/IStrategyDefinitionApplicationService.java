package com.lavyoung.marketforge.application.strategy.service;

import com.lavyoung.marketforge.application.strategy.model.*;

/**
 * 策略定义运营应用服务。
 *
 * <p>负责策略创建、完整聚合查询、草稿修改、历史版本复制和生命周期变更。
 * 本服务只编排用例，不处理 HTTP、Session、MyBatis 或 Redis 编码细节。</p>
 *
 * <p>策略发布时必须先完成配置就绪检查和运行时概率快照预热，
 * 再使用状态与版本条件更新将策略推进为已生效状态。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
public interface IStrategyDefinitionApplicationService {

    /**
     * 创建新的草稿策略定义。
     *
     * @param command 创建策略命令
     * @return 创建后的完整策略定义
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 配置非法、策略标识重复或持久化失败时抛出
     */
    StrategyDefinitionResult create(CreateStrategyDefinitionCommand command);

    /**
     * 查询完整策略定义。
     *
     * @param strategyId 策略业务标识
     * @return 包含根配置、奖品和规则的完整策略定义
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 策略不存在时抛出
     */
    StrategyDefinitionResult queryByStrategyId(Long strategyId);

    /**
     * 修改完整草稿策略定义。
     *
     * @param command 修改草稿命令
     * @return 修改后的完整策略定义
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 策略不存在、状态不可编辑、配置非法或版本冲突时抛出
     */
    StrategyDefinitionResult updateDraft(UpdateStrategyDefinitionCommand command);

    /**
     * 将已生效或已停用策略复制为新的独立草稿。
     *
     * @param command 复制策略命令
     * @return 新生成的草稿策略定义
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 原策略不存在、原策略仍可直接编辑或持久化失败时抛出
     */
    StrategyDefinitionResult copyAsDraft(CopyStrategyDefinitionCommand command);

    /**
     * 发布或停用策略定义。
     *
     * <p>发布操作将执行奖品可用性、概率空间、规则引用和运行时预热检查；
     * 停用操作只改变策略是否允许被新活动引用，不删除历史快照。</p>
     *
     * @param command 生命周期变更命令
     * @return 状态变更后的完整策略定义
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 状态迁移非法、配置不可发布、预热失败或版本冲突时抛出
     */
    StrategyDefinitionResult changeState(ChangeStrategyDefinitionStateCommand command);
}
