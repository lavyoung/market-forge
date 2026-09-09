package com.lavyoung.marketforge.api.strategy;

import com.lavyoung.marketforge.api.strategy.request.StrategyRaffleRequest;
import com.lavyoung.marketforge.api.strategy.response.StrategyAwardResponse;
import com.lavyoung.marketforge.api.strategy.response.StrategyRaffleResponse;
import com.lavyoung.marketforge.types.model.Response;
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
@RequestMapping(
        path = IStrategyRaffleApi.BASE_PATH,
        produces = MediaType.APPLICATION_JSON_VALUE
)
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
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    Response<StrategyRaffleResponse> raffle(@Valid @RequestBody StrategyRaffleRequest request);

    /**
     * 策略装配初始化
     *
     * @param strategyId 策略id
     * @return void
     */
    @PutMapping("/strategy/armory")
    Response<Void> strategyArmory(@RequestParam(value = "strategyId") Long strategyId);

    /**
     * 奖品列表
     *
     * @param strategyId 策略id
     * @return 奖品
     */
    @GetMapping("/strategy/awardList")
    Response<List<StrategyAwardResponse>> queryStrategyAwardList(@RequestParam(value = "strategyId") Long strategyId);
}
