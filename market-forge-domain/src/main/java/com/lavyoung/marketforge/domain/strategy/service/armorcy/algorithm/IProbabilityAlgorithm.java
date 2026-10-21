package com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm;

import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.ProbabilitySnapshotKey;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.StrategyProbabilitySpace;

/**
 * 概率抽奖算法统一协议。
 * <p>
 * 每一种算法必须同时实现适用性判断、版本化概率数据装配和奖品派发。
 * 调度服务负责生成快照版本并发布生效指针，算法只负责读写指定版本的数据，
 * 从而保证一次抽奖不会混用不同版本的概率结构。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/19
 */
public interface IProbabilityAlgorithm {

    /**
     * 获取当前实现支持的算法类型。
     *
     * @return 唯一算法类型
     */
    ProbabilityAlgorithmType type();

    /**
     * 判断当前算法是否适合给定概率空间。
     *
     * @param probabilitySpace 已完成整数化和完整性校验的概率空间
     * @return 当前算法能够安全装配时返回 {@code true}
     * @throws NullPointerException 概率空间为空时抛出
     */
    boolean supports(StrategyProbabilitySpace probabilitySpace);

    /**
     * 将概率空间写入指定版本的不可变快照。
     *
     * <p>{@code probabilitySpace} 与 {@code snapshotKey} 必须使用相同装配键。
     * 算法完成全部数据写入后返回，但不负责发布生效指针。</p>
     *
     * @param probabilitySpace 已完成整数化和完整性校验的概率空间
     * @param snapshotKey      本次装配使用的快照键和版本
     * @throws NullPointerException 概率空间或快照键为空时抛出
     * @throws IllegalArgumentException 两者装配键不一致或概率空间无法装配时抛出
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 概率空间超出算法容量时抛出
     */
    void assemble(StrategyProbabilitySpace probabilitySpace, ProbabilitySnapshotKey snapshotKey);

    /**
     * 从指定版本的不可变概率快照中随机选取奖品。
     *
     * @param snapshotKey 当前抽奖请求固定使用的快照键和版本
     * @return 命中的奖品标识
     * @throws NullPointerException 快照键为空时抛出
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 快照数据不存在或不完整时抛出
     */
    long dispatch(ProbabilitySnapshotKey snapshotKey);
}
