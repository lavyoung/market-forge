package com.lavyoung.marketforge.application.activity.service;

import com.lavyoung.marketforge.application.activity.model.ActivityStageResult;
import com.lavyoung.marketforge.application.activity.model.CreateActivityStageCommand;
import com.lavyoung.marketforge.application.activity.model.PublishActivityStageCommand;
import com.lavyoung.marketforge.application.activity.model.UpdateActivityStageCommand;

/**
 * 活动展台应用服务。
 *
 * <p>负责展台草稿的创建、修改、查询和当前生效活动解析。展台发布已经迁移至
 * {@link IActivityStagePublicationApplicationService}，旧发布入口仅用于兼容尚未完成迁移的调用方。</p>
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
     * 使用兼容协议发布指定活动展台。
     *
     * <p>该方法将发布命令委托给 {@link IActivityStagePublicationApplicationService}，但仍返回旧版展台结果，
     * 无法完整表达处理中、失败阶段和重试次数。新的 HTTP 接口应直接调用发布应用服务并返回发布任务结果。</p>
     *
     * @param command 发布命令
     * @return 发布请求执行后的最新展台配置
     * @throws NullPointerException 命令为空时抛出
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 发布任务创建、检查、预热或切换失败时抛出
     * @deprecated 使用 {@link IActivityStagePublicationApplicationService#publish(PublishActivityStageCommand)}
     */
    @Deprecated(since = "1.0.0", forRemoval = false)
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
