package com.lavyoung.marketforge.application.activity.service;

import com.lavyoung.marketforge.application.activity.model.ActivityReadinessResult;

/**
 * 活动运行就绪检查应用服务。
 *
 * <p>负责根据活动标识读取活动定义，并调用领域检查器生成就绪报告。
 * 该报告同时服务活动开放和展台发布场景。本接口只暴露 Application
 * 模型，不向 Trigger 泄漏领域实体和值对象。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/05
 */
public interface IActivityReadinessApplicationService {

    /**
     * 检查指定活动配置是否满足开放或展台发布条件。
     *
     * @param activityId 活动业务标识
     * @return 活动运行就绪检查结果
     * @throws NullPointerException                                       活动标识为空时抛出
     * @throws IllegalArgumentException                                   活动标识不是正数时抛出
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 活动不存在时抛出
     * @throws RuntimeException                                           检查依赖发生系统故障时抛出
     */
    ActivityReadinessResult check(Long activityId);
}
