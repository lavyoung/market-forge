package com.lavyoung.marketforge.domain.strategy.model.vo;

import com.lavyoung.marketforge.types.common.Constants;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 黑名单规则配置值对象。
 *
 * <p>黑名单规则使用“兜底奖品标识:用户标识/用户标识”的持久化格式。
 * 当参与用户命中名单时，抽奖责任链跳过普通概率计算，直接返回
 * {@code fallbackAwardId} 指定的奖品。</p>
 *
 * <p>本值对象负责统一校验和解析黑名单配置，避免发布校验器与运行时
 * 责任链分别解释同一个字符串。</p>
 *
 * @param fallbackAwardId 命中黑名单后返回的兜底奖品标识
 * @param userIds         受黑名单规则约束的用户标识列表
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @throws NullPointerException     奖品标识或用户列表为空时抛出
 * @throws IllegalArgumentException 奖品标识不是正数、用户列表为空、包含空用户或重复用户时抛出
 * @date 2026/10/25
 */
public record BlacklistRuleValueVO(
        Long fallbackAwardId,
        List<String> userIds
) {
    /**
     * 创建并校验黑名单规则值对象。
     */
    public BlacklistRuleValueVO {
        Objects.requireNonNull(fallbackAwardId, "fallbackAwardId must not be null");
        Objects.requireNonNull(userIds, "userIds must not be null");

        if (fallbackAwardId <= 0) {
            throw new IllegalArgumentException("fallbackAwardId must be positive");
        }
        List<String> normalizedUserIds = userIds.stream()
                .map(userId -> Objects.requireNonNull(userId, "userId must not be null"))
                .map(String::trim)
                .toList();
        if (normalizedUserIds.isEmpty()) {
            throw new IllegalArgumentException("userIds must not be empty");
        }
        if (normalizedUserIds.stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException("userIds must not contain blank value");
        }
        if (new HashSet<>(normalizedUserIds).size() != normalizedUserIds.size()) {
            throw new IllegalArgumentException("userIds must not contain duplicate value");
        }
        userIds = List.copyOf(normalizedUserIds);
    }

    /**
     * 从数据库规则值解析黑名单配置。
     *
     * @param ruleValue 格式为“兜底奖品标识:用户标识/用户标识”的规则值
     * @return 完成格式校验的黑名单规则值对象
     * @throws IllegalArgumentException 规则值为空、格式不正确或奖品标识不是有效正整数时抛出
     */
    public static BlacklistRuleValueVO parse(String ruleValue) {
        if (ruleValue == null || ruleValue.isBlank()) {
            throw new IllegalArgumentException("blacklist rule value must not be blank");
        }

        String[] parts = ruleValue.split(Pattern.quote(Constants.COLON), -1);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new IllegalArgumentException("blacklist rule value must use fallbackAwardId:userId/userId format");
        }
        try {
            Long fallbackAwardId = Long.valueOf(parts[0].trim());
            List<String> userIds = Arrays.stream(parts[1].split(Pattern.quote(Constants.SPLIT), -1))
                    .map(String::trim)
                    .toList();
            return new BlacklistRuleValueVO(fallbackAwardId, userIds);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("blacklist fallbackAwardId must be a valid integer", exception);
        }
    }

    /**
     * 判断指定用户是否命中当前黑名单。
     *
     * @param userId 参与抽奖的用户标识
     * @return 用户标识非空且存在于黑名单中时返回 {@code true}
     */
    public boolean contains(String userId) {
        return userId != null && userIds.contains(userId);
    }

    /**
     * 转换为数据库使用的标准规则值。
     *
     * @return “兜底奖品标识:用户标识/用户标识”格式的规则值
     */
    public String toConfigValue() {
        return fallbackAwardId + Constants.COLON + String.join(Constants.SPLIT, userIds);
    }
}
