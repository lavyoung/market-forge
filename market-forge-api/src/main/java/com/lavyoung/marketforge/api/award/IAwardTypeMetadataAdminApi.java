package com.lavyoung.marketforge.api.award;

import com.lavyoung.marketforge.api.award.response.AwardTypeMetadataResponse;
import com.lavyoung.marketforge.types.model.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * 奖品类型运营元数据 REST API 契约。
 *
 * <p>提供当前系统真实支持的奖品类型和结构化配置表单定义。
 * 本接口只查询应用能力，不读取或修改具体奖品定义。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
@Tag(name = "运营后台奖品管理", description = "提供奖品类型和结构化配置表单元数据")
@RequestMapping(path = IAwardTypeMetadataAdminApi.BASE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public interface IAwardTypeMetadataAdminApi {

    /**
     * 奖品类型运营元数据基础路径。
     */
    String BASE_PATH = "/admin/award-types";

    /**
     * 查询当前可供运营人员配置的全部奖品类型。
     *
     * @return 按奖品类型编码稳定排序的元数据列表
     */
    @Operation(operationId = "listAwardTypes", summary = "查询可用奖品类型")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "运营人员尚未登录"),
            @ApiResponse(responseCode = "403", description = "没有查询权限")
    })
    @GetMapping
    Response<List<AwardTypeMetadataResponse>> listAwardTypes();
}
