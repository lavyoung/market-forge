package com.lavyoung.marketforge.application.activity.service;

import com.lavyoung.marketforge.application.activity.model.ActivityStagePublicationResult;
import com.lavyoung.marketforge.application.activity.model.PublishActivityStageCommand;

/**
 * 活动展台发布应用服务。
 *
 * <p>负责对某个展台版本执行幂等发布、失败重试和发布结果查询。具体流程包括配置检查、
 * 运行数据预热、渠道生效展台切换以及发布记录状态迁移。</p>
 *
 * <p>同一个 {@code stageId + stageVersion} 只能对应一条发布任务：</p>
 *
 * <ul>
 *     <li>首次请求创建任务并执行发布。</li>
 *     <li>重复请求遇到成功任务时直接返回成功结果。</li>
 *     <li>重复请求遇到处理中任务时返回当前进度，不重复执行副作用。</li>
 *     <li>重复请求遇到失败任务时复用原记录并增加尝试次数。</li>
 * </ul>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/06
 */
public interface IActivityStagePublicationApplicationService {

    /**
     * 幂等发布指定版本的活动展台。
     *
     * <p>发布成功前不会改变当前已生效展台。生效切换和发布任务成功状态必须位于
     * 同一个本地事务中，避免出现“展台已经生效但发布记录仍然处理中”的不一致状态。</p>
     *
     * @param command 展台发布命令
     * @return 发布任务最新结果
     * @throws NullPointerException                                       命令为空时抛出
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 展台不存在、版本冲突、
     *                                                                    配置未就绪、预热失败或切换失败时抛出
     */
    ActivityStagePublicationResult publish(PublishActivityStageCommand command);

    /**
     * 查询指定展台版本对应的发布任务。
     *
     * @param stageId      展台业务标识
     * @param stageVersion 展台乐观锁版本
     * @return 发布任务最新结果
     * @throws NullPointerException                                       展台标识为空时抛出
     * @throws IllegalArgumentException                                   展台版本小于 1 时抛出
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 发布任务不存在时抛出
     */
    ActivityStagePublicationResult query(Long stageId, int stageVersion);
}
