package com.lavyoung.marketforge.api.behavior;

import com.lavyoung.marketforge.api.behavior.request.BehaviorRebateRequest;
import com.lavyoung.marketforge.api.behavior.response.BehaviorRebateResponse;
import com.lavyoung.marketforge.types.model.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 *
 * 用户行为返利 REST API 契约。
 * <p>
 * 该接口以“用户发生了某个行为”为入口，创建返利订单并生成后续发放消息任务。
 * Controller 只实现协议适配，不承载返利规则判断、订单创建、消息任务保存等业务逻辑。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/10
 */
@Tag(name = "用户行为返利", description = "提供用户行为返利入账能力")
@RequestMapping(path = IBehaviorRebateApi.BASE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public interface IBehaviorRebateApi {

    /**
     * 用户行为返利接口基础路径。
     */
    String BASE_PATH = "/behavior/rebates";

    /**
     * 创建用户行为返利订单。
     * <p>
     * 典型场景：用户完成签到后，调用该接口把签到行为写入返利系统。
     * 系统会根据行为类型查询返利配置，创建用户返利订单，并生成可靠消息任务。
     *
     * @param request 用户行为返利入账请求
     * @return 本次入账创建的返利订单号和消息事件号
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 行为类型无效、返利配置异常或订单创建失败时抛出
     */
    @Operation(
            operationId = "createBehaviorRebateOrder",
            summary = "创建用户行为返利订单",
            description = "根据用户行为创建返利订单，并生成后续返利发放消息任务。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "入账成功"),
            @ApiResponse(responseCode = "400", description = "请求体缺失、格式错误或字段校验失败"),
            @ApiResponse(responseCode = "422", description = "行为类型无效、返利配置异常或返利订单创建失败"),
            @ApiResponse(responseCode = "500", description = "系统内部错误")
    })
    @PostMapping(path = "/orders", consumes = MediaType.APPLICATION_JSON_VALUE)
    Response<BehaviorRebateResponse> createOrder(@Valid @RequestBody BehaviorRebateRequest request);
}
