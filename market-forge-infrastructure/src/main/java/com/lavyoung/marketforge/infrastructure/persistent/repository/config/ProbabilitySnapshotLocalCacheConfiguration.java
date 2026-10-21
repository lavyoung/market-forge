package com.lavyoung.marketforge.infrastructure.persistent.repository.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 概率快照 JVM 本地缓存基础设施配置。
 *
 * <p>负责启用本地缓存参数绑定。缓存只优化带版本号的概率数据读取，
 * 不改变 Redis 生效指针承担的跨实例版本协调职责。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/21
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ProbabilitySnapshotLocalCacheProperties.class)
public class ProbabilitySnapshotLocalCacheConfiguration {
}
