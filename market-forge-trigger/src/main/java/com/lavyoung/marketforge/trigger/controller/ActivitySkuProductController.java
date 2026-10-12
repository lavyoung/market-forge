package com.lavyoung.marketforge.trigger.controller;

import com.lavyoung.marketforge.api.activity.IActivitySkuProductApi;
import com.lavyoung.marketforge.api.activity.response.ActivitySkuProductResponse;
import com.lavyoung.marketforge.application.activity.model.ActivitySkuProductResult;
import com.lavyoung.marketforge.application.activity.service.IActivitySkuProductApplicationService;
import com.lavyoung.marketforge.trigger.assembler.ActivitySkuProductResponseAssembler;
import com.lavyoung.marketforge.types.model.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 活动 SKU 商品查询 HTTP 适配器。
 *
 * <p>该控制器实现活动 SKU 商品查询 REST 契约，面向前端积分兑换页提供商品列表。
 * 它只承担协议适配职责：接收活动标识、调用 application 查询用例、
 * 并将应用层结果转换为 API 响应对象。</p>
 *
 * <p>商品列表查询不会创建订单、不会扣减积分、不会扣减库存。
 * 真正的兑换动作由积分兑换接口完成。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/12
 */
@RestController
@RequiredArgsConstructor
public class ActivitySkuProductController implements IActivitySkuProductApi {

    private final IActivitySkuProductApplicationService activitySkuProductApplicationService;
    private final ActivitySkuProductResponseAssembler activitySkuProductResponseAssembler;

    @Override
    public Response<List<ActivitySkuProductResponse>> querySkuProducts(Long activityId) {
        List<ActivitySkuProductResult> results = activitySkuProductApplicationService.querySkuProducts(activityId);
        return Response.success(activitySkuProductResponseAssembler.toResponses(results));
    }
}
