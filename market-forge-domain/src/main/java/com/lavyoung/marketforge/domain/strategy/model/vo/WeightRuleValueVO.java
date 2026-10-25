package com.lavyoung.marketforge.domain.strategy.model.vo;

import com.lavyoung.marketforge.types.common.Constants;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 权重奖池规则配置值对象。
 *
 * <p>权重规则由一个或多个累计次数档位组成，数据库格式为
 * “次数门槛:奖品ID/奖品ID;次数门槛:奖品ID”。用户参与抽奖时，
 * 系统选择不高于其累计次数的最高档位，并在该档位的候选奖品中抽奖。</p>
 *
 * <p>档位按照次数门槛升序保存。同一档位不能重复引用奖品，同一条规则
 * 也不能出现重复门槛。</p>
 *
 * @param tiers 按次数门槛升序保存的权重档位
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @throws NullPointerException     档位集合或档位元素为空时抛出
 * @throws IllegalArgumentException 档位集合为空或包含重复次数门槛时抛出
 * @date 2026/10/25
 */
public record WeightRuleValueVO(List<WeightTierVO> tiers) {

    /**
     * 创建并校验权重规则值对象。
     */
    public WeightRuleValueVO {
        Objects.requireNonNull(tiers, "tiers must not be null");

        List<WeightTierVO> normalizedTiers = tiers.stream()
                .map(tier -> Objects.requireNonNull(tier, "weight tier must not be null"))
                .sorted(Comparator.comparingLong(WeightTierVO::threshold))
                .toList();

        if (normalizedTiers.isEmpty()) {
            throw new IllegalArgumentException("weight tiers must not be empty");
        }
        if (normalizedTiers.stream().map(WeightTierVO::threshold).distinct().count() != normalizedTiers.size()) {
            throw new IllegalArgumentException("weight tiers must not contain duplicate threshold");
        }

        tiers = List.copyOf(normalizedTiers);
    }


    /**
     * 从数据库规则值解析全部权重档位。
     *
     * <p>为兼容已有数据，允许规则值末尾存在一个分号；转换回标准配置时
     * 不会保留末尾分号。中间空档位和连续分号仍会被拒绝。</p>
     *
     * @param ruleValue 权重规则持久化字符串
     * @return 完成排序和校验的权重规则值对象
     * @throws IllegalArgumentException 规则值为空、档位格式错误或包含非法奖品标识时抛出
     */
    public static WeightRuleValueVO parse(String ruleValue) {
        if (ruleValue == null || ruleValue.isBlank()) {
            throw new IllegalArgumentException("weight rule value must not be blank");
        }

        String normalizedValue = ruleValue.trim();
        if (normalizedValue.endsWith(Constants.SEMICOLON)) {
            normalizedValue = normalizedValue.substring(0, normalizedValue.length() - 1);
        }
        if (normalizedValue.isBlank() || normalizedValue.endsWith(Constants.SEMICOLON)) {
            throw new IllegalArgumentException("weight rule value contains empty tier");
        }

        List<WeightTierVO> tiers = Arrays.stream(normalizedValue.split(Pattern.quote(Constants.SEMICOLON), -1))
                .map(WeightTierVO::parse)
                .toList();
        return new WeightRuleValueVO(tiers);
    }

    /**
     * 根据用户累计次数选择能够命中的最高权重档位。
     *
     * @param completedCount 用户累计完成的抽奖次数
     * @return 不高于用户累计次数的最高档位；没有满足条件的档位时返回空
     * @throws IllegalArgumentException 用户累计次数小于零时抛出
     */
    public Optional<WeightTierVO> match(long completedCount) {
        if (completedCount < 0) {
            throw new IllegalArgumentException("completedCount must not be negative");
        }
        return tiers.stream().filter(tier -> tier.threshold() <= completedCount)
                .max(Comparator.comparingLong(WeightTierVO::threshold));
    }

    /**
     * 按门槛返回权重档位映射。
     *
     * @return 次数门槛到候选奖品列表的不可修改映射
     */
    public Map<Long, List<Long>> toTierMap() {
        return tiers.stream().collect(Collectors.toUnmodifiableMap(WeightTierVO::threshold, WeightTierVO::awardIds));
    }


    /**
     * 转换为数据库使用的标准规则值。
     *
     * @return 不带末尾分号的标准权重配置
     */
    public String toConfigValue() {
        return tiers.stream().map(WeightTierVO::toConfigValue).collect(Collectors.joining(Constants.SEMICOLON));
    }


}
