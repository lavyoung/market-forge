package com.lavyoung.marketforge.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * DDD Forge 生成项目的 Spring Boot 启动入口。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0-SNAPSHOT
 */
@SpringBootApplication
public class MarketForgeApplication {

    /**
     * 启动 Market Forge Spring Boot 应用。
     *
     * @param args 应用启动参数
     */
    public static void main(String[] args) {

        SpringApplication.run(
                MarketForgeApplication.class,
                args
        );
    }

}
