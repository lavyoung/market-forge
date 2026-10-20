package com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * 概率抽奖算法类型。
 * <p>
 * 该枚举描述策略概率数据采用的存储和查询方式。编码会作为装配元数据写入缓存，
 * 抽奖调度器根据编码选择与装配阶段一致的算法实现。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/19
 */
@Getter
@AllArgsConstructor
public enum ProbabilityAlgorithmType {

    /**
     * 直接查表算法。
     * <p>
     * 将奖品标识按照整数权重重复填充到查找表中，抽奖查询复杂度为 O(1)，
     * 空间复杂度为 O(totalWeight)。
     */
    DIRECT_LOOKUP("direct_lookup"),

    /**
     * Alias Method 别名抽样算法。
     *
     * <p>装配阶段为每个奖品建立一个接受概率和候补别名列，抽样查询复杂度为
     * O(1)，空间复杂度为 O(n)。适合候选奖品数量较大的静态概率空间。</p>
     */
    ALIAS_METHOD("alias_method"),

    /**
     * 累计区间二分算法。
     *
     * <p>每个奖品只保存一个累计概率上界，抽奖查询复杂度为 O(log n)，
     * 空间复杂度为 O(n)。</p>
     */
    BINARY_SEARCH("binary_search");
    
    private final String code;

    /**
     * 根据持久化编码解析算法类型。
     *
     * @param code 算法编码
     * @return 匹配的算法类型；编码为空或无法识别时返回空
     */
    public static Optional<ProbabilityAlgorithmType> fromCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }

        return Arrays.stream(values()).filter(type -> type.code.equals(code)).findFirst();
    }
}
