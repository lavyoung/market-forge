package com.lavyoung.marketforge.api.credit;

import com.lavyoung.marketforge.api.credit.response.UserCreditAccountResponse;
import com.lavyoung.marketforge.types.model.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 用户积分账户查询 REST API 契约。
 *
 * <p>该接口为前端提供用户当前可用积分余额，用于展示积分账户状态，
 * 并帮助用户在兑换前判断余额是否可能充足。</p>
 *
 * <p>该接口是只读接口，不创建积分账户流水、不调整余额，也不承担积分兑换校验。
 * 最终能否兑换仍由积分兑换应用服务按最新账户余额进行原子扣减判断。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/12
 */
@Tag(name = "用户积分账户", description = "提供用户可用积分余额查询能力")
@RequestMapping(path = ICreditAccountApi.BASE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public interface ICreditAccountApi {

    /**
     * 用户积分账户查询接口基础路径。
     */
    String BASE_PATH = "/credit/accounts";

    /**
     * 查询指定用户的可用积分余额。
     *
     * <p>返回的 {@code availableAmount} 是用户可参与积分兑换的当前余额快照。
     * 查询完成到用户实际提交兑换请求之间，余额可能因其他并发兑换发生变化；
     * 因此页面展示的余额不能替代兑换时的服务端扣减校验。</p>
     *
     * @param userId 用户标识
     * @return 用户积分账户可用余额
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 当用户标识非法、账户不存在或查询失败时抛出
     */
    @Operation(
            operationId = "queryUserCreditAccount",
            summary = "查询用户可用积分",
            description = "按用户标识查询当前可用于兑换的积分余额。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "400", description = "用户标识为空或格式错误"),
            @ApiResponse(responseCode = "422", description = "积分账户不存在"),
            @ApiResponse(responseCode = "500", description = "系统内部错误")
    })
    @GetMapping
    Response<UserCreditAccountResponse> queryCreditAccount(@RequestParam("userId")
                                                           @NotBlank(message = "用户标识不能为空")
                                                           String userId);
}
