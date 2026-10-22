package com.lavyoung.marketforge.application.activity.service;

import com.lavyoung.marketforge.application.activity.model.ActivityStageResult;
import com.lavyoung.marketforge.application.activity.model.CreateActivityStageCommand;
import com.lavyoung.marketforge.application.activity.model.PublishActivityStageCommand;
import com.lavyoung.marketforge.application.activity.model.UpdateActivityStageCommand;

/**
 * 活动展台应用服务。
 *
 * <p>负责编排展台草稿维护、活动预热、渠道生效配置切换和用户端活动解析。
 * 生命周期合法性由领域对象判断，数据库并发安全由展台仓储和发布事务共同保证。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
public interface IActivityStageApplicationService {

    /**
     * 创建待上架活动展台。
     *
     * @param command 创建命令
     * @return 创建后的展台配置
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 配置重复或写入失败时抛出
     */
    ActivityStageResult create(CreateActivityStageCommand command);

    /**
     * 查询指定活动展台。
     *
     * @param stageId 展台业务标识
     * @return 展台配置
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 展台不存在时抛出
     */
    ActivityStageResult queryByStageId(Long stageId);

    /**
     * 修改待上架活动展台。
     *
     * @param command 修改命令
     * @return 修改后的展台配置
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 展台不存在、状态错误或版本冲突时抛出
     */
    ActivityStageResult updateDraft(UpdateActivityStageCommand command);

    /**
     * 预热并发布指定活动展台。
     *
     * <p>只有活动与策略运行时数据成功预热后，展台才会切换为生效状态。</p>
     *
     * @param command 发布命令
     * @return 发布后的展台配置
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 活动不可发布、预热失败或并发冲突时抛出
     */
    ActivityStageResult publish(PublishActivityStageCommand command);

    /**
     * 根据渠道和来源解析当前对外活动标识。
     *
     * @param channel 投放渠道
     * @param source  流量来源
     * @return 当前生效的活动标识
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 当前没有生效活动时抛出
     */
    Long queryActiveActivityId(String channel, String source);
}
