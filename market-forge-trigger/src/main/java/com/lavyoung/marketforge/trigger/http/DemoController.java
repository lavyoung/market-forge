package com.lavyoung.marketforge.trigger.http;

import com.lavyoung.marketforge.api.demo.dto.DemoDTO;
import com.lavyoung.marketforge.api.demo.request.CreateDemoRequest;
import com.lavyoung.marketforge.api.demo.service.IDemoApi;
import com.lavyoung.marketforge.application.demo.command.CreateDemoCommand;
import com.lavyoung.marketforge.application.demo.result.DemoResult;
import com.lavyoung.marketforge.application.demo.service.DemoApplicationService;
import com.lavyoung.marketforge.types.model.Response;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 演示用例的 HTTP 适配器。
 *
 * @author lavyoung
 * @email lavyoung1325@outlook.com
 * @version 1.0.0-SNAPSHOT
 */
@RestController
@RequestMapping("/api/demo")
public class DemoController implements IDemoApi {

    private final DemoApplicationService applicationService;

    public DemoController(DemoApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @Override
    @PostMapping
    public Response<DemoDTO> create(
            @Valid @RequestBody CreateDemoRequest request) {

        DemoResult result = applicationService.create(
                new CreateDemoCommand(request.name())
        );

        DemoDTO dto = new DemoDTO(
                result.id(),
                result.name()
        );

        return Response.success(dto);
    }

}
