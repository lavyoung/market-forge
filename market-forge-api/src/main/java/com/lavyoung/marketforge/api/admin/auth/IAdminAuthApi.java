package com.lavyoung.marketforge.api.admin.auth;

import com.lavyoung.marketforge.api.admin.auth.request.AdminLoginRequest;
import com.lavyoung.marketforge.api.admin.auth.response.AdminCsrfTokenResponse;
import com.lavyoung.marketforge.api.admin.auth.response.AdminOperatorResponse;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 运营后台身份认证 REST API 契约。
 *
 * <p>该接口定义 CSRF 令牌获取、运营人员登录、当前用户查询和退出登录能力。
 * 认证成功后由服务端建立 Session，并通过 HttpOnly Cookie 向浏览器传递
 * Session 标识。</p>
 *
 * <p>接口只定义稳定的 HTTP 协议，不暴露 Spring Security 的
 * {@code Authentication}、{@code SecurityContext} 或 CSRF 内部对象。</p>
 *
 * <p>登录和退出属于会修改服务端会话状态的请求，调用方必须先获取 CSRF
 * 令牌，并按照响应中的请求头名称回传令牌。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/19
 */
@Tag(name = "运营后台身份认证", description = "提供运营人员登录、会话查询和退出登录能力")
@RequestMapping(path = IAdminAuthApi.BASE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public interface IAdminAuthApi {

    /**
     * 运营后台身份认证接口基础路径。
     */
    String BASE_PATH = "/admin/auth";

    /**
     * 获取当前会话的 CSRF 令牌。
     *
     * <p>该接口允许匿名访问。前端在调用登录、退出以及其他修改服务端状态的
     * 接口前，需要将返回的令牌放入指定 HTTP 请求头。</p>
     *
     * @return 当前会话对应的 CSRF 令牌信息
     * @throws IllegalStateException 服务端无法生成 CSRF 令牌时抛出
     */
    @Operation(operationId = "getAdminCsrfToken", summary = "获取 CSRF 令牌", description = "获取登录和其他写操作所需的 CSRF 令牌。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "令牌获取成功"),
            @ApiResponse(responseCode = "500", description = "令牌生成失败")
    })
    @GetMapping("/csrf")
    Response<AdminCsrfTokenResponse> getCsrfToken();

    /**
     * 使用运营人员账号和密码登录。
     *
     * <p>认证成功后，服务端将认证信息写入 Spring Security 上下文，
     * 并把 Session 保存到 Redis。响应体只返回非敏感的运营人员信息。</p>
     *
     * @param request 运营后台登录凭证
     * @return 已通过认证的运营人员信息
     * @throws BusinessException 用户名或密码错误、账号不可用时抛出
     */
    @Operation(operationId = "loginAdminOperator", summary = "运营人员登录", description = "校验运营人员账号和密码，并建立服务端登录会话。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "登录成功"),
            @ApiResponse(responseCode = "400", description = "登录参数不合法"),
            @ApiResponse(responseCode = "401", description = "用户名或密码错误"),
            @ApiResponse(responseCode = "403", description = "CSRF 令牌缺失或无效"),
            @ApiResponse(responseCode = "500", description = "认证服务异常")
    })
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    Response<AdminOperatorResponse> login(@Valid @RequestBody AdminLoginRequest request);

    /**
     * 查询当前会话中的运营人员。
     *
     * <p>该接口用于前端页面刷新后恢复用户状态。客户端不应仅依靠本地缓存判断
     * 登录状态，而应以服务端返回的认证信息为准。</p>
     *
     * @return 当前登录运营人员及其权限集合
     * @throws BusinessException 当前会话未认证或认证信息无效时抛出
     */
    @Operation(operationId = "getCurrentAdminOperator", summary = "查询当前登录用户", description = "根据服务端 Session 返回当前运营人员及其权限。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "当前用户查询成功"),
            @ApiResponse(responseCode = "401", description = "尚未登录或会话已经失效")
    })
    @GetMapping("/me")
    Response<AdminOperatorResponse> getCurrentOperator();

    /**
     * 注销当前运营后台会话。
     *
     * <p>退出成功后，服务端清理认证上下文并使 Redis Session 失效。
     * 浏览器中原有的 Session Cookie 随后不再具备认证能力。</p>
     *
     * @return 不包含业务数据的成功响应
     * @throws BusinessException 当前会话注销失败时抛出
     */
    @Operation(operationId = "logoutAdminOperator", summary = "运营人员退出登录", description = "清理当前认证上下文并使服务端 Session 失效。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "退出登录成功"),
            @ApiResponse(responseCode = "401", description = "当前会话尚未登录"),
            @ApiResponse(responseCode = "403", description = "CSRF 令牌缺失或无效")
    })
    @PostMapping("/logout")
    Response<Void> logout();
}
