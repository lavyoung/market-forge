package com.lavyoung.marketforge.types.utils;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轻量级业务 ID 生成工具。
 * <p>
 * 该工具用于生成一般业务场景下的订单号、流水号、消息号等标识。它通过当前时间、进程内递增序列
 * 和随机片段组合来降低重复概率，但不提供雪花算法那种跨机器、跨时钟回拨场景下的严格唯一性保证。
 * 对强一致唯一 ID 有要求的主键、支付单号或外部三方幂等号，仍应使用数据库唯一约束、号段服务或
 * 专门的分布式 ID 服务兜底。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/23
 */
public final class IdGenerator {

    /**
     * 数字 ID 允许的最小长度。
     */
    private static final int MIN_NUMERIC_LENGTH = 8;

    /**
     * 数字 ID 允许的最大长度。
     */
    private static final int MAX_NUMERIC_LENGTH = 32;

    /**
     * 每毫秒内的进程级滚动序列。
     */
    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    /**
     * 随机数来源。
     */
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 时钟来源，保留为字段便于统一替换和测试。
     */
    private static final Clock CLOCK = Clock.systemDefaultZone();

    private IdGenerator() {
    }

    /**
     * 生成一个通用业务 ID。
     * <p>
     * ID 由毫秒时间戳的三十六进制表示、四位三十六进制进程内序列和八位三十六进制随机片段组成，
     * 字符集仅包含数字和小写字母，适合作为内部订单号、流水号和消息号使用。
     *
     * @return 通用业务 ID
     */
    public static String nextId() {
        return Long.toString(CLOCK.millis(), Character.MAX_RADIX)
                + nextSequence36()
                + randomBase36(16);
    }

    /**
     * 生成带指定前缀的通用业务 ID。
     * <p>
     * 前缀会按原样拼接在生成的 ID 前面。调用方应根据数据库字段长度和外部接口约束选择短前缀，
     * 例如 {@code "ORD"}、{@code "ACT"}、{@code "MSG"}。
     *
     * @param prefix 业务前缀，不能为空白
     * @return 带指定前缀的通用业务 ID
     * @throws IllegalArgumentException 当前缀为空白时抛出
     */
    public static String nextId(String prefix) {
        return requireText(prefix, "prefix") + nextId();
    }

    /**
     * 生成指定长度的纯数字业务 ID。
     * <p>
     * 数字 ID 会优先使用当前毫秒时间戳后缀、进程内序列和随机数填充。长度越短，重复概率越高；
     * 如果业务字段允许，推荐使用不少于 12 位。
     *
     * @param length 数字 ID 长度，范围为 8 到 32
     * @return 指定长度的纯数字业务 ID
     * @throws IllegalArgumentException 当长度不在允许范围内时抛出
     */
    public static String nextNumericId(int length) {
        if (length < MIN_NUMERIC_LENGTH || length > MAX_NUMERIC_LENGTH) {
            throw new IllegalArgumentException("length must be between " + MIN_NUMERIC_LENGTH + " and " + MAX_NUMERIC_LENGTH);
        }

        String timestamp = Long.toString(CLOCK.millis());
        String sequence = leftPad(Integer.toString(Math.floorMod(SEQUENCE.incrementAndGet(), 10_000)), 4);
        StringBuilder source = new StringBuilder(timestamp).append(sequence);
        while (source.length() < length) {
            source.append(RANDOM.nextInt(10));
        }
        if (source.length() == length) {
            return source.toString();
        }
        return source.substring(source.length() - length);
    }

    /**
     * 生成带指定前缀的纯数字业务 ID。
     * <p>
     * 前缀按原样拼接，{@code numericLength} 只约束前缀后的数字部分长度。
     *
     * @param prefix        业务前缀，不能为空白
     * @param numericLength 数字部分长度，范围为 8 到 32
     * @return 带指定前缀的纯数字业务 ID
     * @throws IllegalArgumentException 当前缀为空白或数字长度非法时抛出
     */
    public static String nextNumericId(String prefix, int numericLength) {
        return requireText(prefix, "prefix") + nextNumericId(numericLength);
    }

    private static String nextSequence36() {
        int value = Math.floorMod(SEQUENCE.incrementAndGet(), 1_679_616);
        return leftPad(Integer.toString(value, Character.MAX_RADIX), 4);
    }

    private static String randomBase36(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(Character.forDigit(RANDOM.nextInt(Character.MAX_RADIX), Character.MAX_RADIX));
        }
        return builder.toString().toLowerCase(Locale.ROOT);
    }

    private static String leftPad(String value, int length) {
        if (value.length() >= length) {
            return value;
        }
        return "0".repeat(length - value.length()) + value;
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
