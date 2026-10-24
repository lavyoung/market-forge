package com.lavyoung.marketforge.trigger.controller;

import com.lavyoung.marketforge.api.award.IAwardTypeMetadataAdminApi;
import com.lavyoung.marketforge.api.award.response.AwardTypeMetadataResponse;
import com.lavyoung.marketforge.application.award.model.AwardTypeMetadata;
import com.lavyoung.marketforge.application.award.service.IAwardTypeMetadataApplicationService;
import com.lavyoung.marketforge.trigger.assembler.AwardTypeMetadataApiAssembler;
import com.lavyoung.marketforge.types.model.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 奖品类型运营元数据 HTTP 入站适配器。
 *
 * <p>只负责调用 Application 查询服务、转换协议模型并包装统一响应，
 * 不扫描 Spring Bean，也不重新判断奖品履约能力。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
@RestController
@RequiredArgsConstructor
public class AwardTypeMetadataAdminController implements IAwardTypeMetadataAdminApi {

    private final IAwardTypeMetadataApplicationService applicationService;
    private final AwardTypeMetadataApiAssembler apiAssembler;

    @Override
    public Response<List<AwardTypeMetadataResponse>> listAwardTypes() {
        List<AwardTypeMetadata> metadata = applicationService.listSupportedTypes();
        return Response.success(apiAssembler.toResponses(metadata));
    }
}
