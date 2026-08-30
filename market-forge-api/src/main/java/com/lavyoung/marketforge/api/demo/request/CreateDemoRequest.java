package com.lavyoung.marketforge.api.demo.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 创建演示对象的接口请求。
 *
 * @param name 待创建的演示对象名称
 * @author lavyoung
 * @email lavyoung1325@outlook.com
 * @version 1.0.0-SNAPSHOT
 */
public record CreateDemoRequest(

        @NotBlank(message = "name cannot be blank")
        String name

) {
}
