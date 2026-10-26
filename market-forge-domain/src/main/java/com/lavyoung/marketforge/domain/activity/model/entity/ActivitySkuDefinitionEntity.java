package com.lavyoung.marketforge.domain.activity.model.entity;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 活动 SKU 运营定义实体。
 *
 * <p>该实体服务于运营后台的 SKU 创建、配置修改和库存管理，与高频兑换链路使用的
 * {@link ActivitySkuEntity} 分离。运营模型额外维护乐观锁版本和审计信息，避免把后台
 * 管理字段带入运行时额度入账流程。</p>
 *
 * <p>{@code stockCount} 表示累计配置的库存总量，{@code stockCountSurplus}
 * 表示当前可用库存。兑换成功只能减少剩余库存，不能减少库存总量；库存总量只能通过
 * 独立库存调整用例修改，普通配置修改不得重置剩余库存。</p>
 *
 * @param sku               SKU 业务标识
 * @param activityId        SKU 所属活动标识
 * @param activityCountId   兑换该 SKU 后获得的活动次数配置标识
 * @param stockCount        累计配置的库存总量
 * @param stockCountSurplus 当前剩余库存
 * @param creditAmount      兑换一个 SKU 所需积分；为空表示不支持积分兑换
 * @param version           乐观锁版本，从 1 开始
 * @param createdBy         创建人标识
 * @param updatedBy         最后修改人标识
 * @param createTime        创建时间；首次持久化前允许为空
 * @param updateTime        最后修改时间；首次持久化前允许为空
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/26
 */
public record ActivitySkuDefinitionEntity(
        Long sku,
        Long activityId,
        Long activityCountId,
        long stockCount,
        long stockCountSurplus,
        Long creditAmount,
        int version,
        String createdBy,
        String updatedBy,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {

    /**
     * 创建并校验活动 SKU 运营定义。
     *
     * <p>库存总量必须大于零，剩余库存必须处于 {@code [0, stockCount]} 区间。
     * 积分价格允许为空；只要配置了价格，就必须大于零。</p>
     *
     * @throws NullPointerException     SKU、活动、次数配置或审计人员为空时抛出
     * @throws IllegalArgumentException 标识、库存、积分价格、版本或审计信息非法时抛出
     */
    public ActivitySkuDefinitionEntity {
        Objects.requireNonNull(sku, "sku must not be null");
        Objects.requireNonNull(activityId, "activityId must not be null");
        Objects.requireNonNull(activityCountId, "activityCountId must not be null");
        requireText(createdBy, "createdBy", 64);
        requireText(updatedBy, "updatedBy", 64);

        if (sku <= 0 || activityId <= 0 || activityCountId <= 0) {
            throw new IllegalArgumentException("SKU relation identifiers must be positive");
        }
        if (stockCount <= 0) {
            throw new IllegalArgumentException("stockCount must be greater than zero");
        }
        if (stockCountSurplus < 0 || stockCountSurplus > stockCount) {
            throw new IllegalArgumentException("stockCountSurplus must be between zero and stockCount");
        }
        if (creditAmount != null && creditAmount <= 0) {
            throw new IllegalArgumentException("creditAmount must be greater than zero");
        }
        if (version < 1) {
            throw new IllegalArgumentException("version must be greater than zero");
        }
    }

    /**
     * 判断 SKU 是否已经发生过库存消费。
     *
     * @return 剩余库存小于累计库存总量时返回 {@code true}
     */
    public boolean hasBeenUsed() {
        return stockCountSurplus < stockCount;
    }

    /**
     * 判断该 SKU 是否支持积分兑换。
     *
     * @return 配置了有效积分价格时返回 {@code true}
     */
    public boolean supportsCreditExchange() {
        return creditAmount != null;
    }

    /**
     * 修改 SKU 的活动关联、次数配置和积分价格。
     *
     * <p>该操作不会接收库存总量或剩余库存，因此普通编辑无法把已消费库存重置。
     * SKU 一旦发生过消费，也不能再修改所属活动和次数配置，否则历史订单和后续入账
     * 将指向不同业务含义。已使用 SKU 仍可以调整未来订单使用的积分价格。</p>
     *
     * <p>本方法不增加版本号。仓储更新时使用当前版本作为 SQL 条件，更新成功后由
     * 数据库原子增加版本号。</p>
     *
     * @param newActivityId      新的活动标识
     * @param newActivityCountId 新的活动次数配置标识
     * @param newCreditAmount    新的积分价格；为空表示关闭积分兑换
     * @param operator           本次修改的运营人员
     * @return 保留库存、创建信息和当前版本的新 SKU 定义
     * @throws NullPointerException     活动、次数配置或操作人为空时抛出
     * @throws IllegalArgumentException 标识、积分价格或操作人非法时抛出
     * @throws IllegalStateException    SKU 已使用且试图修改活动或次数配置时抛出
     */
    public ActivitySkuDefinitionEntity updateConfiguration(Long newActivityId, Long newActivityCountId, Long newCreditAmount, String operator) {
        if (hasBeenUsed() && (!activityId.equals(newActivityId) || !activityCountId.equals(newActivityCountId))) {
            throw new IllegalStateException("used SKU cannot change activity relation");
        }
        return new ActivitySkuDefinitionEntity(sku, newActivityId, newActivityCountId, stockCount,
                stockCountSurplus, newCreditAmount, version, createdBy, operator, createTime, updateTime);
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
