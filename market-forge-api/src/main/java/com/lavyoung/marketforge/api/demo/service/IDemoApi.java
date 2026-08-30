package com.lavyoung.marketforge.api.demo.service;

import com.lavyoung.marketforge.api.demo.dto.DemoDTO;
import com.lavyoung.marketforge.api.demo.request.CreateDemoRequest;
import com.lavyoung.marketforge.types.model.Response;

/**
 * 演示业务的对外 API 契约。
 *
 * @author lavyoung
 * @email lavyoung1325@outlook.com
 * @version 1.0.0-SNAPSHOT
 */
public interface IDemoApi {

    Response<DemoDTO> create(CreateDemoRequest request);

}
