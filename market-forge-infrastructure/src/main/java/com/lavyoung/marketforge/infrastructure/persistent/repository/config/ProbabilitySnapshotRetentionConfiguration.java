package com.lavyoung.marketforge.infrastructure.persistent.repository.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 概率快照生命周期基础设施配置。
 *
 * <p>负责把外部配置绑定为强类型快照回收策略，供 Redis 仓储实现使用。
 * 本配置不参与领域装配和抽奖规则判断。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/21
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ProbabilitySnapshotRetentionProperties.class)
public class ProbabilitySnapshotRetentionConfiguration {
}