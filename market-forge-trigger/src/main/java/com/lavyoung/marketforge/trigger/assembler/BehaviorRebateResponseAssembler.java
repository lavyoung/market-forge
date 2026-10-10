package com.lavyoung.marketforge.trigger.assembler;

import com.lavyoung.marketforge.api.behavior.request.BehaviorRebateRequest;
import com.lavyoung.marketforge.api.behavior.response.BehaviorRebateResponse;
import com.lavyoung.marketforge.application.behavior.model.BehaviorRebateOrderCreateResult;
import com.lavyoung.marketforge.application.behavior.model.CreateBehaviorRebateOrderCommand;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * 用户行为返利接口层装配器。
 * <p>
 * 该装配器只负责 API 层和 application 层之间的对象投影：
 * API 请求对象转换为 application 命令，
 * application 结果对象转换为 API 响应对象。
 * <p>
 * 注意：trigger 层不能依赖 domain 层，因此这里不做 {@code BehaviorTypeVO}
 * 转换，也不接收领域层结果对象。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/10
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface BehaviorRebateResponseAssembler {

    /**
     * 将用户行为返利 API 请求转换为应用层创建订单命令。
     * <p>
     * 字段名一致：userId、behaviorType、outBusinessNo，
     * MapStruct 可以自动完成映射。
     *
     * @param request 用户行为返利入账请求
     * @return 创建用户行为返利订单命令
     */
    CreateBehaviorRebateOrderCommand toCommand(BehaviorRebateRequest request);

    /**
     * 将用户行为返利应用结果转换为 API 响应。
     *
     * @param result 用户行为返利订单创建结果
     * @return 用户行为返利入账响应
     */
    BehaviorRebateResponse toResponse(BehaviorRebateOrderCreateResult result);
}