package com.lavyoung.marketforge.infrastructure.persistent.repository.config;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.Objects;

/**
 * 概率快照 JVM 本地缓存配置。
 *
 * <p>本地缓存只保存包含版本号的不可变概率数据，不缓存当前生效指针。
 * 每次抽奖仍从 Redis 读取生效指针，因此多实例能够及时发现新版本；
 * 同一版本的概率数据则优先从 JVM 内存读取。</p>
 *
 * <p>缓存同时受快照数量和数据元素数量限制，避免策略版本持续增长
 * 导致 JVM 堆内存被无限占用。</p>
 *
 * @param enabled             是否启用本地概率快照缓存
 * @param expireAfterAccess   快照最后一次访问后的保留时间
 * @param maximumSnapshots    每类缓存允许保存的最大快照数量
 * @param maximumDataElements 每类概率数据缓存允许保存的最大元素总数
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/21
 */
@ConfigurationProperties(prefix = "market-forge.strategy.snapshot.local-cache")
public record ProbabilitySnapshotLocalCacheProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("30m") Duration expireAfterAccess,
        @DefaultValue("10000") long maximumSnapshots,
        @DefaultValue("100000") long maximumDataElements) {

    /**
     * 校验本地缓存的容量与生命周期约束。
     *
     * @throws NullPointerException     访问过期时间为空时抛出
     * @throws IllegalArgumentException 过期时间或容量不大于零时抛出
     */
    public ProbabilitySnapshotLocalCacheProperties {
        Objects.requireNonNull(expireAfterAccess, "expireAfterAccess must not be null");
        if (expireAfterAccess.isZero() || expireAfterAccess.isNegative()) {
            throw new IllegalArgumentException("expireAfterAccess must be greater than zero");
        }
        if (maximumSnapshots <= 0) {
            throw new IllegalArgumentException("maximumSnapshots must be greater than zero");
        }
        if (maximumDataElements <= 0) {
            throw new IllegalArgumentException("maximumDataElements must be greater than zero");
        }
    }
}