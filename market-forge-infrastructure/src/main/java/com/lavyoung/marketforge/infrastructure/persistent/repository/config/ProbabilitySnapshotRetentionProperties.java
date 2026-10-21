package com.lavyoung.marketforge.infrastructure.persistent.repository.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Objects;

/**
 * 概率快照回收策略配置。
 *
 * <p>当前生效快照不会设置过期时间。只有已经被新版本替换、装配失败或者
 * 并发发布失败的快照才会进入退役状态，并在宽限期结束后由 Redis 自动删除。</p>
 *
 * <p>宽限期用于保护已经读取旧指针、但尚未完成概率查询的在途抽奖请求。
 * 因此该值应明显大于正常抽奖请求的最大执行时间。</p>
 *
 * @param retirementGracePeriod 退役快照被 Redis 删除前的保留时间
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/21
 */
@ConfigurationProperties(prefix = "market-forge.strategy.snapshot")
public record ProbabilitySnapshotRetentionProperties(Duration retirementGracePeriod) {

    private static final Duration DEFAULT_RETIREMENT_GRACE_PERIOD = Duration.ofMinutes(10);

    /**
     * 补充默认宽限期并校验配置不变量。
     *
     * @throws IllegalArgumentException 宽限期为零或负数时抛出
     */
    public ProbabilitySnapshotRetentionProperties {
        retirementGracePeriod = Objects.requireNonNullElse(
                retirementGracePeriod, DEFAULT_RETIREMENT_GRACE_PERIOD);
        if (retirementGracePeriod.isZero() || retirementGracePeriod.isNegative()) {
            throw new IllegalArgumentException(
                    "market-forge.strategy.snapshot.retirement-grace-period must be greater than zero");
        }
    }
}
