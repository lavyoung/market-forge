package com.lavyoung.marketforge.api.strategy;

import com.lavyoung.marketforge.api.strategy.request.StrategyRaffleRequest;
import com.lavyoung.marketforge.api.strategy.response.StrategyAwardResponse;
import com.lavyoung.marketforge.api.strategy.response.StrategyRaffleResponse;
import com.lavyoung.marketforge.types.model.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 抽奖策略 REST API 契约。
 * <p>
 * 本接口同时约定资源路径、HTTP 方法、请求校验和统一响应结构。服务端适配器只需实现本接口，
 * HTTP 客户端也可复用该契约生成代理，避免提供方与消费方分别维护路由和数据结构。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/09
 */
@Tag(name = "策略抽奖", description = "提供策略装配、奖品查询和抽奖能力")
@RequestMapping(path = IStrategyRaffleApi.BASE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public interface IStrategyRaffleApi {

    /**
     * 抽奖资源基础路径。
     */
    String BASE_PATH = "/raffles";

    /**
     * 根据用户与策略执行一次抽奖。
     *
     * @param request 抽奖请求
     * @return 包含本次抽奖结果的统一响应
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 请求不满足业务规则时抛出
     */
    @Operation(operationId = "raffle", summary = "执行策略抽奖", description = """
            根据用户标识与策略标识执行一次真实抽奖。
            抽奖结果由服务端责任链和规则树共同决定。
            """)
    @ApiResponses({@ApiResponse(responseCode = "200", description = "抽奖成功"), @ApiResponse(responseCode = "400", description = "请求体缺失、格式错误或字段校验失败"), @ApiResponse(responseCode = "422", description = "未满足抽奖业务规则"), @ApiResponse(responseCode = "500", description = "系统内部错误")})
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    Response<StrategyRaffleResponse> raffle(@Valid @RequestBody StrategyRaffleRequest request);

    /**
     * 策略装配初始化
     *
     * @param strategyId 策略id
     * @return void
     */
    @Operation(operationId = "strategyArmory", summary = "装配抽奖策略", description = "将策略概率、权重和库存数据装配到 Redis。")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "策略装配成功"), @ApiResponse(responseCode = "400", description = "策略标识不合法"), @ApiResponse(responseCode = "422", description = "策略配置不完整或无法装配"), @ApiResponse(responseCode = "500", description = "系统内部错误")})
    @PutMapping("/strategy/armory")
    Response<Void> strategyArmory(@RequestParam(value = "strategyId") Long strategyId);

    /**
     * 奖品列表
     *
     * @param strategyId 策略id
     * @return 奖品
     */
    @Operation(operationId = "queryStrategyAwardList", summary = "查询策略奖品列表", description = "查询指定抽奖策略配置的全部奖品及库存、概率和展示顺序。")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "查询成功"), @ApiResponse(responseCode = "400", description = "策略标识不合法"), @ApiResponse(responseCode = "500", description = "系统内部错误")})
    @GetMapping("/strategy/awardList")
    Response<List<StrategyAwardResponse>> queryStrategyAwardList(@RequestParam(value = "strategyId") Long strategyId);
}
