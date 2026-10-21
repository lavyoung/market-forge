package com.lavyoung.marketforge.trigger.controller;

import com.lavyoung.marketforge.api.strategy.IStrategyAdminApi;
import com.lavyoung.marketforge.api.strategy.response.StrategySnapshotRefreshResponse;
import com.lavyoung.marketforge.application.strategy.service.IStrategySnapshotRefreshApplicationService;
import com.lavyoung.marketforge.types.model.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

/**
 * 抽奖策略运营管理 HTTP 适配器。
 *
 * <p>该控制器负责接收运营端策略快照刷新命令，将协议参数传递给 Application Service，
 * 并把应用层调度结果转换为稳定的 API 响应。</p>
 *
 * <p>控制器不直接调用领域装配器，不管理线程池，也不访问 Redis。高频请求合并和
 * 异步调度属于 Application 层职责，概率快照构建属于 Domain 层职责。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/21
 */
@RestController
@RequiredArgsConstructor
public class StrategyAdminController implements IStrategyAdminApi {

    private final IStrategySnapshotRefreshApplicationService strategySnapshotRefreshApplicationService;

    @Override
    public Response<StrategySnapshotRefreshResponse> requestSnapshotRefresh(Long strategyId) {
        boolean newTaskScheduled = strategySnapshotRefreshApplicationService.requestRefresh(strategyId);
        StrategySnapshotRefreshResponse response = new StrategySnapshotRefreshResponse(strategyId, newTaskScheduled);
        return Response.success(response);
    }
}
