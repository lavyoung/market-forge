package com.lavyoung.marketforge.trigger.assembler;

import com.lavyoung.marketforge.api.strategy.request.StrategyRaffleRequest;
import com.lavyoung.marketforge.api.strategy.response.StrategyRaffleResponse;
import com.lavyoung.marketforge.application.strategy.model.RaffleCommand;
import com.lavyoung.marketforge.application.strategy.model.RaffleResult;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * 策略抽奖接口层装配器。
 * <p>
 * 隔离外部 API 契约与应用层模型，并在编译期生成无反射的字段转换代码。
 * 对应字段的名称和类型必须保持一致；新增未映射的目标字段时编译将失败，避免契约静默丢失。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/09
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface StrategyRaffleAssembler {

    /**
     * 将抽奖 API 请求转换为应用层命令。
     *
     * @param request 已通过协议层校验的抽奖请求
     * @return 应用层抽奖命令
     */
    RaffleCommand toCommand(StrategyRaffleRequest request);

    /**
     * 将应用层抽奖结果转换为 API 响应。
     *
     * @param result 应用层抽奖结果
     * @return 对外抽奖响应
     */
    StrategyRaffleResponse toResponse(RaffleResult result);
}
