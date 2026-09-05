package com.lavyoung.marketforge.api.demo.service;

import com.lavyoung.marketforge.api.demo.dto.DemoDTO;
import com.lavyoung.marketforge.api.demo.request.CreateDemoRequest;
import com.lavyoung.marketforge.types.model.Response;

/**
 * 演示业务的对外 API 契约。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0-SNAPSHOT
 */
public interface IDemoApi {

    /**
     * 创建演示对象。
     *
     * @param request 创建请求
     * @return 包含新建演示对象的统一响应
     */
    Response<DemoDTO> create(CreateDemoRequest request);

}
