package com.lavyoung.marketforge.domain.activity.model.entity;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 活动 SKU 库存调整实体。
 *
 * <p>库存调整与普通 SKU 配置修改属于两个不同业务动作。普通配置修改负责活动关联、
 * 次数配置和积分价格；库存调整负责增加或收回尚未消费的库存，并保存完整的调整快照。</p>
 *
 * <p>{@code quantityDelta} 为正数表示补充库存，为负数表示收回库存。调整时库存总量和
 * 剩余库存必须增加或减少相同数量，从而保证已经消费的库存数量保持不变：</p>
 *
 * <pre>
 * 已消费库存 = 库存总量 - 剩余库存
 * </pre>
 *
 * <p>例如当前总库存为 100、剩余库存为 80，说明已经消费 20。补充 50 后应变为
 * 总库存 150、剩余库存 130，已经消费的 20 不会被重置。</p>
 *
 * @param adjustmentId            库存调整业务标识，用于幂等和审计
 * @param sku                     被调整的 SKU 标识
 * @param quantityDelta           调整数量；正数为增加，负数为减少，不允许为零
 * @param stockCountBefore        调整前库存总量
 * @param stockCountAfter         调整后库存总量
 * @param stockCountSurplusBefore 调整前剩余库存
 * @param stockCountSurplusAfter  调整后剩余库存
 * @param expectedVersion         调整时预期的 SKU 乐观锁版本
 * @param reason                  调整原因
 * @param operator                操作人标识
 * @param createTime              调整记录创建时间；首次持久化前允许为空
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/26
 */
public record ActivitySkuStockAdjustmentEntity(
        Long adjustmentId,
        Long sku,
        long quantityDelta,
        long stockCountBefore,
        long stockCountAfter,
        long stockCountSurplusBefore,
        long stockCountSurplusAfter,
        int expectedVersion,
        String reason,
        String operator,
        LocalDateTime createTime
) {

    /**
     * 创建并校验库存调整记录。
     *
     * <p>调整数量不能为零；调整后的总库存和剩余库存必须分别等于调整前数值加上调整量；
     * 调整后总库存必须大于零，剩余库存必须处于合法区间。</p>
     *
     * @throws NullPointerException     调整标识、SKU、原因或操作人为空时抛出
     * @throws IllegalArgumentException 标识、调整数量、库存快照、版本或文本字段非法时抛出
     * @throws ArithmeticException      库存加减发生 long 溢出时抛出
     */
    public ActivitySkuStockAdjustmentEntity {
        Objects.requireNonNull(adjustmentId, "adjustmentId must not be null");
        Objects.requireNonNull(sku, "sku must not be null");
        requireText(reason, "reason", 256);
        requireText(operator, "operator", 64);

        if (adjustmentId <= 0 || sku <= 0) {
            throw new IllegalArgumentException("adjustment identifiers must be positive");
        }
        if (quantityDelta == 0) {
            throw new IllegalArgumentException("quantityDelta must not be zero");
        }
        if (stockCountBefore <= 0) {
            throw new IllegalArgumentException("stockCountBefore must be greater than zero");
        }
        if (stockCountSurplusBefore < 0 || stockCountSurplusBefore > stockCountBefore) {
            throw new IllegalArgumentException("stockCountSurplusBefore is outside the valid range");
        }
        if (Math.addExact(stockCountBefore, quantityDelta) != stockCountAfter) {
            throw new IllegalArgumentException("stockCountAfter does not match quantityDelta");
        }
        if (Math.addExact(stockCountSurplusBefore, quantityDelta) != stockCountSurplusAfter) {
            throw new IllegalArgumentException("stockCountSurplusAfter does not match quantityDelta");
        }
        if (stockCountAfter <= 0) {
            throw new IllegalArgumentException("stockCountAfter must be greater than zero");
        }
        if (stockCountSurplusAfter < 0 || stockCountSurplusAfter > stockCountAfter) {
            throw new IllegalArgumentException("stockCountSurplusAfter is outside the valid range");
        }
        if (expectedVersion < 1) {
            throw new IllegalArgumentException("expectedVersion must be greater than zero");
        }
    }

    /**
     * 根据当前 SKU 定义生成库存调整记录。
     *
     * <p>本方法同时调整总库存和剩余库存，不改变已经消费的库存数量。它只在内存中
     * 计算调整结果，不修改数据库。仓储实现必须使用 {@code expectedVersion}、
     * 调整前库存和 SKU 作为 SQL 条件执行原子更新。</p>
     *
     * @param adjustmentId  库存调整业务标识
     * @param current       当前 SKU 定义
     * @param quantityDelta 调整数量；正数为补充，负数为收回
     * @param reason        调整原因
     * @param operator      操作人标识
     * @return 包含调整前后库存快照的库存调整实体
     * @throws NullPointerException     当前 SKU、原因或操作人为空时抛出
     * @throws IllegalArgumentException 调整数量导致库存越界时抛出
     * @throws ArithmeticException      库存计算发生 long 溢出时抛出
     */
    public static ActivitySkuStockAdjustmentEntity create(Long adjustmentId, ActivitySkuDefinitionEntity current, long quantityDelta, String reason, String operator) {
        Objects.requireNonNull(current, "current must not be null");
        long stockCountAfter = Math.addExact(current.stockCount(), quantityDelta);
        long stockCountSurplusAfter = Math.addExact(current.stockCountSurplus(), quantityDelta);
        return new ActivitySkuStockAdjustmentEntity(adjustmentId, current.sku(), quantityDelta, current.stockCount(),
                stockCountAfter, current.stockCountSurplus(), stockCountSurplusAfter, current.version(), reason,
                operator, null);
    }

    /**
     * 计算调整前已经消费的库存数量。
     *
     * @return 调整前库存总量减去剩余库存
     */
    public long consumedCountBefore() {
        return stockCountBefore - stockCountSurplusBefore;
    }

    /**
     * 计算调整后已经消费的库存数量。
     *
     * @return 调整后库存总量减去剩余库存
     */
    public long consumedCountAfter() {
        return stockCountAfter - stockCountSurplusAfter;
    }

    private static void requireText(String value, String fieldName, int maximumLength) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        if (value.length() > maximumLength) {
            throw new IllegalArgumentException(fieldName + " length must not exceed " + maximumLength);
        }
    }
}
