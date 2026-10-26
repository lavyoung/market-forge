package com.lavyoung.marketforge.infrastructure.persistent.po.activity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 活动 SKU 库存调整流水持久化对象。
 *
 * <p>该对象与 {@code raffle_activity_sku_stock_adjustment} 表一一对应，
 * 保存每次库存调整的增量、调整前后快照、操作原因和操作人员。</p>
 *
 * <p>库存调整流水属于只追加审计数据，写入后不得修改，因此不继承同时包含
 * {@code updateTime} 的 {@code BasePO}。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/26
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("raffle_activity_sku_stock_adjustment")
public class ActivitySkuStockAdjustmentPO {

    /**
     * 数据库自增主键。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 库存调整业务标识。
     */
    private Long adjustmentId;

    /**
     * 被调整的 SKU 标识。
     */
    private Long sku;

    /**
     * 调整数量；正数表示增加，负数表示减少。
     */
    private Long quantityDelta;

    /**
     * 调整前库存总量。
     */
    private Long stockCountBefore;

    /**
     * 调整后库存总量。
     */
    private Long stockCountAfter;

    /**
     * 调整前剩余库存。
     */
    private Long stockCountSurplusBefore;

    /**
     * 调整后剩余库存。
     */
    private Long stockCountSurplusAfter;

    /**
     * 调整时预期的 SKU 乐观锁版本。
     */
    private Integer expectedVersion;

    /**
     * 调整原因。
     */
    private String reason;

    /**
     * 操作人标识。
     */
    private String operator;

    /**
     * 调整流水创建时间。
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
