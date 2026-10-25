package com.lavyoung.marketforge.domain.strategy.model.vo;

/**
 * 奖品次数锁规则配置值对象。
 *
 * <p>次数锁规则表示用户至少完成多少次有效抽奖后，指定奖品才允许进入
 * 后续库存校验。配置值 {@code 0} 表示奖品没有次数门槛。</p>
 *
 * @param requiredCount 解锁奖品所需的最低完成次数
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
public record LockRuleValueVO(int requiredCount) {

    /**
     * 创建并校验次数锁规则值对象。
     */
    public LockRuleValueVO {
        if (requiredCount < 0) {
            throw new IllegalArgumentException("requiredCount must not be negative");
        }
    }

    /**
     * 从数据库规则值解析次数锁配置。
     *
     * @param ruleValue 非负整数格式的规则值
     * @return 完成校验的次数锁规则值对象
     * @throws IllegalArgumentException 规则值为空、不是整数或小于零时抛出
     */
    public static LockRuleValueVO parse(String ruleValue) {
        if (ruleValue == null || ruleValue.isBlank()) {
            throw new IllegalArgumentException("lock rule value must not be blank");
        }
        try {
            return new LockRuleValueVO(Integer.parseInt(ruleValue.trim()));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("lock rule value must be a valid integer", exception);
        }
    }

    /**
     * 判断用户已经完成的次数是否达到解锁要求。
     *
     * @param completedCount 用户已经完成的有效抽奖次数
     * @return 已完成次数不小于解锁次数时返回 {@code true}
     * @throws IllegalArgumentException 已完成次数小于零时抛出
     */
    public boolean isSatisfiedBy(long completedCount) {
        if (completedCount < 0) {
            throw new IllegalArgumentException("completedCount must not be negative");
        }
        return completedCount >= requiredCount;
    }

    /**
     * 转换为数据库使用的标准规则值。
     *
     * @return 非负整数字符串
     */
    public String toConfigValue() {
        return Integer.toString(requiredCount);
    }
}
