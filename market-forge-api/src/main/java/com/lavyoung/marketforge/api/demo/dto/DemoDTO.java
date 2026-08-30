package com.lavyoung.marketforge.api.demo.dto;

/**
 * 演示领域对象的接口传输模型。
 *
 * @param id 演示对象标识
 * @param name 演示对象名称
 * @author lavyoung
 * @email lavyoung1325@outlook.com
 * @version 1.0.0-SNAPSHOT
 */
public record DemoDTO(

        Long id,

        String name

) {
}
