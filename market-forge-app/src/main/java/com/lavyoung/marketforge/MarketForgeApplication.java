package com.lavyoung.marketforge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * DDD Forge 生成项目的 Spring Boot 启动入口。
 *
 * @author lavyoung
 * @email lavyoung1325@outlook.com
 * @version 1.0.0-SNAPSHOT
 */
@SpringBootApplication
public class MarketForgeApplication {

    public static void main(String[] args) {

        SpringApplication.run(
                MarketForgeApplication.class,
                args
        );
    }

}
