package com.lavyoung.marketforge.trigger.controller;

import com.lavyoung.marketforge.api.strategy.IStrategyRaffleApi;
import com.lavyoung.marketforge.api.strategy.request.StrategyRaffleRequest;
import com.lavyoung.marketforge.api.strategy.response.StrategyAwardResponse;
import com.lavyoung.marketforge.api.strategy.response.StrategyRaffleResponse;
import com.lavyoung.marketforge.application.strategy.model.RaffleCommand;
import com.lavyoung.marketforge.application.strategy.model.RaffleResult;
import com.lavyoung.marketforge.application.strategy.service.IStrategyRaffleService;
import com.lavyoung.marketforge.trigger.assembler.StrategyAwardAssembler;
import com.lavyoung.marketforge.trigger.assembler.StrategyRaffleAssembler;
import com.lavyoung.marketforge.types.model.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/**
 * 策略抽奖 HTTP 适配器。
 * <p>
 * 实现公开 REST 契约，负责协议参数到应用命令的转换，以及 HTTP 统一响应包装。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/09
 */
@RestController
@RequiredArgsConstructor
public class StrategyRaffleController implements IStrategyRaffleApi {

    private final IStrategyRaffleService strategyRaffleService;
    private final StrategyRaffleAssembler strategyRaffleAssembler;
    private final StrategyAwardAssembler strategyAwardAssembler;

    @Override
    public Response<StrategyRaffleResponse> raffle(StrategyRaffleRequest request) {
        StrategyRaffleRequest validRequest = Objects.requireNonNull(request, "request must not be null");
        RaffleCommand command = strategyRaffleAssembler.toCommand(validRequest);
        RaffleResult result = strategyRaffleService.raffle(command);
        return Response.success(strategyRaffleAssembler.toResponse(result));
    }

    @Override
    public Response<Void> strategyArmory(Long strategyId) {
        strategyRaffleService.initStrategyRaffle(strategyId);
        return Response.success();
    }

    @Override
    public Response<List<StrategyAwardResponse>> queryStrategyAwardList(Long strategyId) {
        return Response.success(strategyAwardAssembler.toResponses(
                strategyRaffleService.queryRaffleStrategyAwardList(strategyId)));
    }

}
