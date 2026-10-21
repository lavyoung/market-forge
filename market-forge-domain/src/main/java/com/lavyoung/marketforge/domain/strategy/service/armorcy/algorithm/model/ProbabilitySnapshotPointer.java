package com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model;

import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.ProbabilityAlgorithmType;

import java.util.Objects;

/**
 * 当前生效概率快照的不可变指针。
 *
 * <p>概率表重新装配时，系统先使用新版本保存完整概率数据，确认保存成功后，
 * 再原子替换当前指针。抽奖请求读取指针后，只访问该指针指定的版本，从而
 * 避免把新旧版本的算法类型、奖品列表和概率数据混合使用。</p>
 *
 * <p>{@code version} 表示策略配置重建代次。同一次重建产生的普通奖池和权重奖池
 * 指针共享该值；后续重建的代次必须更大。基础设施应使用 Redis 原子递增生成代次，
 * 不得使用本机时间戳，以避免多实例时钟偏差和版本碰撞。</p>
 *
 * @param assembleKey   普通策略标识或“策略标识_权重档位”装配键
 * @param algorithmType 当前快照使用的概率算法
 * @param version       当前快照所属的正整数策略重建代次
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/21
 */
public record ProbabilitySnapshotPointer(
        String assembleKey,
        ProbabilityAlgorithmType algorithmType,
        long version
) {

    /**
     * 校验快照指针的不变量。
     *
     * @throws IllegalArgumentException 装配键为空白或版本不是正整数时抛出
     * @throws NullPointerException     算法类型为空时抛出
     */
    public ProbabilitySnapshotPointer {
        if (assembleKey == null || assembleKey.isBlank()) {
            throw new IllegalArgumentException("assembleKey must not be blank");
        }
        Objects.requireNonNull(algorithmType, "algorithmType must not be null");
        if (version <= 0) {
            throw new IllegalArgumentException("version must be greater than zero");
        }
    }

    /**
     * 根据当前生效指针构造概率数据查询键。
     *
     * @return 与当前装配键和版本完全一致的快照键
     */
    public ProbabilitySnapshotKey snapshotKey() {
        return new ProbabilitySnapshotKey(assembleKey, version);
    }
}
