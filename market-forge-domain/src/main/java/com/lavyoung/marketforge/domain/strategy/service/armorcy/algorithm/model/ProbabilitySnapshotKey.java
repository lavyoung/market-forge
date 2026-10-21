package com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model;

/**
 * 唯一定位一份不可变概率快照的领域键。
 *
 * <p>装配键区分普通策略和权重奖池，版本区分同一概率空间的多次装配结果。
 * 仓储实现使用两个字段构造版本化存储键，但该模型本身不感知 Redis。</p>
 *
 * @param assembleKey 普通策略标识或“策略标识_权重档位”装配键
 * @param version     大于零的概率快照版本
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/21
 */
public record ProbabilitySnapshotKey(
        String assembleKey,
        long version
) {
    /**
     * 校验概率快照键的不变量。
     *
     * @throws IllegalArgumentException 装配键为空白或版本不是正整数时抛出
     */
    public ProbabilitySnapshotKey {
        if (assembleKey == null || assembleKey.isBlank()) {
            throw new IllegalArgumentException("assembleKey must not be blank");
        }
        if (version <= 0) {
            throw new IllegalArgumentException("version must be greater than zero");
        }
    }
}
