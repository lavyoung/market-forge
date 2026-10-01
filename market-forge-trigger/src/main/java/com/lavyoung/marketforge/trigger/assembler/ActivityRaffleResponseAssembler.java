package com.lavyoung.marketforge.trigger.assembler;

import com.lavyoung.marketforge.api.activity.request.ActivityAwardListRequest;
import com.lavyoung.marketforge.api.activity.request.ActivityDrawRequest;
import com.lavyoung.marketforge.api.activity.response.ActivityAwardResponse;
import com.lavyoung.marketforge.api.activity.response.ActivityDrawResponse;
import com.lavyoung.marketforge.application.activity.model.ActivityAwardListCommand;
import com.lavyoung.marketforge.application.activity.model.ActivityAwardResult;
import com.lavyoung.marketforge.application.activity.model.ActivityRaffleCommand;
import com.lavyoung.marketforge.application.activity.model.ActivityRaffleResult;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * 活动抽奖接口层装配器。
 * <p>
 * 这个装配器只负责对象投影：
 * API 请求对象转换为 application 命令，
 * application 结果对象转换为 API 响应对象。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/30
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface ActivityRaffleResponseAssembler {

    /**
     * 将活动抽奖 API 请求转换为活动抽奖应用命令。
     *
     * @param request 活动抽奖请求
     * @return 活动抽奖命令
     */
    ActivityRaffleCommand toCommand(ActivityDrawRequest request);

    /**
     * 将活动抽奖应用结果转换为活动抽奖 API 响应。
     *
     * @param raffle 活动抽奖结果
     * @return 活动抽奖响应
     */
    ActivityDrawResponse toResponse(ActivityRaffleResult raffle);

    /**
     * 将活动奖品列表 API 请求转换为活动奖品列表应用命令。
     * <p>
     * 字段名一致：userId -> userId，activityId -> activityId，
     * 所以 MapStruct 可以自动完成映射。
     *
     * @param request 活动奖品列表查询请求
     * @return 活动奖品列表查询命令
     */
    ActivityAwardListCommand toCommand(ActivityAwardListRequest request);

    /**
     * 将活动奖品应用结果转换为活动奖品 API 响应。
     * <p>
     * 字段名一致时不需要写 @Mapping。
     *
     * @param result 活动奖品应用结果
     * @return 活动奖品 API 响应
     */
    ActivityAwardResponse toResponse(ActivityAwardResult result);

    /**
     * 批量转换活动奖品列表。
     *
     * @param results 活动奖品应用结果列表
     * @return 活动奖品 API 响应列表
     */
    List<ActivityAwardResponse> toAwardResponses(List<ActivityAwardResult> results);
}
