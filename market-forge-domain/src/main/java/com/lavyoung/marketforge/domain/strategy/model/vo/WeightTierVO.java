package com.lavyoung.marketforge.domain.strategy.model.vo;

import com.lavyoung.marketforge.types.common.Constants;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 权重奖池的单个次数档位。
 *
 * <p>一个权重档位表达“用户累计完成指定次数后，可以进入哪些奖品组成的
 * 子奖池”。例如 {@code 10:100001/100002} 表示用户累计完成 10 次抽奖后，
 * 可以在奖品 100001 和 100002 组成的子奖池中参与抽奖。</p>
 *
 * <p>次数门槛允许为零，表示没有累计次数限制。候选奖品不能为空，
 * 所有奖品标识必须为正数，并且同一档位不能重复引用相同奖品。</p>
 *
 * @param threshold 用户进入当前子奖池所需的最低累计次数
 * @param awardIds  当前档位允许参与抽奖的奖品标识
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @throws NullPointerException     奖品标识集合或集合元素为空时抛出
 * @throws IllegalArgumentException 门槛为负数，或者奖品集合为空、包含非法标识或重复标识时抛出
 * @date 2026/10/25
 */
public record WeightTierVO(long threshold, List<Long> awardIds) {

    /**
     * 创建并校验单个权重档位。
     */
    public WeightTierVO {
        Objects.requireNonNull(awardIds, "awardIds must not be null");

        if (threshold < 0) {
            throw new IllegalArgumentException("weight threshold must not be negative");
        }

        List<Long> normalizedAwardIds = awardIds.stream()
                .map(awardId -> Objects.requireNonNull(awardId, "awardId must not be null"))
                .toList();

        if (normalizedAwardIds.isEmpty()) {
            throw new IllegalArgumentException("weight tier awardIds must not be empty");
        }
        if (normalizedAwardIds.stream().anyMatch(awardId -> awardId <= 0)) {
            throw new IllegalArgumentException("weight tier awardId must be positive");
        }
        if (new HashSet<>(normalizedAwardIds).size() != normalizedAwardIds.size()) {
            throw new IllegalArgumentException("weight tier awardIds must not contain duplicate value");
        }

        awardIds = List.copyOf(normalizedAwardIds);
    }

    /**
     * 解析单个权重档位。
     *
     * @param group 格式为“次数门槛:奖品ID/奖品ID”的档位字符串
     * @return 完成格式和业务约束校验的权重档位
     * @throws IllegalArgumentException 档位为空、格式错误或数字无法解析时抛出
     */
    public static WeightTierVO parse(String group) {
        if (group == null || group.isBlank()) {
            throw new IllegalArgumentException("weight tier must not be blank");
        }

        String[] parts = group.split(Pattern.quote(Constants.COLON), -1);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new IllegalArgumentException("weight tier must use threshold:awardId/awardId format");
        }
        try {
            long threshold = Long.parseLong(parts[0].trim());
            List<Long> awardIds = Arrays.stream(parts[1].split(Pattern.quote(Constants.SPLIT), -1))
                    .map(String::trim)
                    .map(Long::valueOf)
                    .toList();
            return new WeightTierVO(threshold, awardIds);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("weight tier contains invalid numeric value", exception);
        }
    }

    /**
     * 转换为数据库使用的标准档位配置。
     *
     * @return “次数门槛:奖品ID/奖品ID”格式的字符串
     */
    public String toConfigValue() {
        String awardIdValue = awardIds.stream().map(String::valueOf).collect(Collectors.joining(Constants.SPLIT));
        return threshold + Constants.COLON + awardIdValue;
    }
}
