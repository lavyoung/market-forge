package com.lavyoung.marketforge.api.strategy;

import com.lavyoung.marketforge.api.strategy.response.StrategySnapshotRefreshResponse;
import com.lavyoung.marketforge.types.model.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 抽奖策略运营管理 REST API 契约。
 *
 * <p>该接口面向已经登录的运营人员，用于触发策略运行时数据维护。
 * 接口不提供用户抽奖能力，也不直接暴露 Redis、线程池和快照版本等基础设施细节。</p>
 *
 * <p>所有接口均位于 {@code /admin/**} 路径下，由 Spring Security
 * 统一执行 Session 身份认证和 CSRF 校验。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/21
 */
@Tag(name = "运营后台策略管理", description = "提供抽奖策略运行时快照刷新能力")
@RequestMapping(path = IStrategyAdminApi.BASE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public interface IStrategyAdminApi {

    /**
     * 策略运营管理基础路径。
     */
    String BASE_PATH = "/admin/strategies";

    /**
     * 请求异步刷新指定策略的概率快照。
     *
     * <p>接口返回 {@code 202 Accepted} 只代表请求已受理。若该策略已有刷新任务，
     * 本次请求会合并到原任务，由工作线程在当前轮次结束后再读取一次数据库最新配置。</p>
     *
     * @param strategyId 需要刷新概率快照的策略标识
     * @return 包含任务创建或请求合并状态的统一响应
     * @throws IllegalArgumentException                        策略标识不是正数时抛出
     * @throws java.util.concurrent.RejectedExecutionException 刷新线程池拒绝任务时抛出
     */
    @Operation(operationId = "requestStrategySnapshotRefresh", summary = "异步刷新策略概率快照", description = "受理策略概率快照刷新请求，并合并同一策略的高频重复请求。")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "刷新请求已受理"),
            @ApiResponse(responseCode = "400", description = "策略标识非法"),
            @ApiResponse(responseCode = "401", description = "运营人员尚未登录"),
            @ApiResponse(responseCode = "403", description = "运营人员没有操作权限"),
            @ApiResponse(responseCode = "503", description = "刷新线程池容量耗尽，可在退避后重试")
    })
    @PostMapping("/{strategyId}/snapshot-refresh")
    @ResponseStatus(HttpStatus.ACCEPTED)
    Response<StrategySnapshotRefreshResponse> requestSnapshotRefresh(@Positive(message = "策略标识必须为正数") @PathVariable("strategyId") Long strategyId);
}
