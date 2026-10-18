package com.lavyoung.marketforge.application.activity.model;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 活动订单应用层查询条件。
 *
 * <p>该模型由运营接口请求转换而来，只表达应用用例允许使用的过滤条件，
 * 不包含 HTTP 注解、Elasticsearch DSL 或具体索引信息。</p>
 *
 * @param userId     用户标识；为空时不限制用户
 * @param activityId 活动标识；为空时不限制活动
 * @param orderId    全业务唯一订单号；为空时不限制订单
 * @param orderState 订单状态；为空时不限制状态
 * @param startTime  下单时间起点，包含该时间
 * @param endTime    下单时间终点，不包含该时间
 * @param pageNumber 页码，从 1 开始
 * @param pageSize   每页记录数
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/18
 */
public record ActivityOrderSearchQuery(
        String userId,
        Long activityId,
        String orderId,
        String orderState,
        LocalDateTime startTime,
        LocalDateTime endTime,
        int pageNumber,
        int pageSize
) {

    private static final Set<String> SUPPORTED_STATES = Set.of("create", "used", "cancel", "wait_pay", "complete");

    /**
     * 规范化并校验活动订单查询条件。
     *
     * @throws IllegalArgumentException 页码、每页数量、订单状态或时间范围非法时抛出
     */
    public ActivityOrderSearchQuery {
        userId = normalize(userId);
        orderId = normalize(orderId);
        orderState = normalize(orderState);

        if (activityId != null && activityId < 1) {
            throw new IllegalArgumentException("activityId must be greater than or equal to 1");
        }
        if (orderState != null && !SUPPORTED_STATES.contains(orderState)) {
            throw new IllegalArgumentException("unsupported activity order state: " + orderState);
        }
        if (startTime != null && endTime != null && !startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("startTime must be earlier than endTime");
        }
        if (pageNumber < 1) {
            throw new IllegalArgumentException("pageNumber must be greater than or equal to 1");
        }
        if (pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("pageSize must be between 1 and 100");
        }
    }

    /**
     * 计算当前页在搜索结果中的起始位置。
     *
     * @return 从零开始的分页偏移量
     * @throws ArithmeticException 页码计算超过整数范围时抛出
     */
    public int offset() {
        return Math.multiplyExact(pageNumber - 1, pageSize);
    }

    /**
     * 将空白查询值规范化为空值。
     *
     * @param value 原始查询值
     * @return 去除首尾空白后的值；内容为空时返回 {@code null}
     */
    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
