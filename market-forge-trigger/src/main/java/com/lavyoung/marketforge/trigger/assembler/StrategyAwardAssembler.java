package com.lavyoung.marketforge.trigger.assembler;

import com.lavyoung.marketforge.api.strategy.response.StrategyAwardResponse;
import com.lavyoung.marketforge.application.strategy.model.StrategyAwardResult;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * 策略奖品接口层装配器。
 * <p>
 * 将应用层奖品查询结果投影为稳定的 API 响应模型。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/09
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface StrategyAwardAssembler {

    /**
     * 将单个应用层奖品结果转换为 API 奖品响应。
     *
     * @param result 策略奖品应用层结果
     * @return 策略奖品 API 响应
     */
    StrategyAwardResponse toResponse(StrategyAwardResult result);

    /**
     * 批量将应用层奖品结果转换为 API 奖品响应。
     *
     * @param results 策略奖品应用层结果列表
     * @return 策略奖品 API 响应列表
     */
    List<StrategyAwardResponse> toResponses(List<StrategyAwardResult> results);
}
