package com.lavyoung.marketforge.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * 业务时间基础配置。
 *
 * <p>统一提供应用使用的 {@link Clock}，避免不同服务自行选择时区，也方便测试环境
 * 使用固定时钟替换系统时间。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/05
 */
@Configuration(proxyBeanMethods = false)
public class BusinessTimeConfiguration {

    /**
     * 创建与数据库配置一致的业务时钟。
     *
     * @return 使用 Asia/Shanghai 时区的系统时钟
     */
    @Bean
    public Clock businessClock() {
        return Clock.system(ZoneId.of("Asia/Shanghai"));
    }
}