package com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm;

import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.StrategyProbabilitySpace;

/**
 * 概率抽奖算法统一协议。
 * <p>
 * 每一种算法必须同时实现适用性判断、概率数据装配和奖品派发，保证抽奖阶段
 * 使用的数据结构与装配阶段生成的数据结构一致。策略装配服务负责选择算法，
 * 不负责算法内部的数据组织和查找过程。
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
     * 装配指定策略概率空间。
     *
     * @param probabilitySpace 已完成整数化和完整性校验的概率空间
     * @throws NullPointerException 概率空间为空时抛出
     * @throws IllegalArgumentException 概率空间无法被当前算法装配时抛出
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 概率空间超出算法容量时抛出
     */
    void assemble(StrategyProbabilitySpace probabilitySpace);

    /**
     * 从已经装配的概率数据中随机选取奖品。
     *
     * @param assembleKey 策略装配键
     * @return 命中的奖品标识
     * @throws IllegalArgumentException 装配键为空白时抛出
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 策略尚未装配或概率数据不完整时抛出
     */
    long dispatch(String assembleKey);
}
