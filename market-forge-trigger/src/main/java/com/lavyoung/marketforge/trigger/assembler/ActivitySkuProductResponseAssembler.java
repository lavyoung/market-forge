package com.lavyoung.marketforge.trigger.assembler;

import com.lavyoung.marketforge.api.activity.response.ActivitySkuProductResponse;
import com.lavyoung.marketforge.application.activity.model.ActivitySkuProductResult;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * 活动 SKU 商品响应装配器。
 *
 * <p>该装配器负责把 application 层的商品查询结果转换为 API 层响应对象。
 * 它只做字段投影，不承载商品可兑换校验、库存扣减或积分扣减等业务逻辑。</p>
 *
 * <p>当前结果对象和响应对象字段名称一致，MapStruct 可以自动完成映射。
 * 如果后续 API 字段名为了前端语义发生变化，也应该优先在这里做协议层转换，
 * 避免污染 application 层模型。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/12
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ActivitySkuProductResponseAssembler {

    /**
     * 将活动 SKU 商品应用查询结果转换为 API 响应。
     *
     * @param result 活动 SKU 商品应用查询结果
     * @return 活动 SKU 商品 API 响应
     */
    ActivitySkuProductResponse toResponse(ActivitySkuProductResult result);

    /**
     * 批量转换活动 SKU 商品列表。
     *
     * @param results 活动 SKU 商品应用查询结果列表
     * @return 活动 SKU 商品 API 响应列表
     */
    List<ActivitySkuProductResponse> toResponses(List<ActivitySkuProductResult> results);
}
