package com.lavyoung.marketforge.infrastructure.persistent.po.activity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lavyoung.marketforge.infrastructure.persistent.po.BasePO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 抽奖活动 SKU 持久化对象。
 *
 * <p>该对象与 {@code raffle_activity_sku} 表一一对应，保存活动 SKU 与活动、
 * 抽奖次数配置、库存、积分兑换价格之间的关联关系。它只负责数据库映射，
 * 不承载兑换校验、订单创建或积分扣减等领域规则。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/18
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("raffle_activity_sku")
public class ActivitySkuPO extends BasePO {

    /**
     * 数据库自增主键。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 商品 SKU。
     */
    private Long sku;

    /**
     * 抽奖活动业务标识。
     */
    private Long activityId;

    /**
     * 活动参与次数配置标识。
     */
    private Long activityCountId;

    /**
     * 商品库存。
     */
    private Long stockCount;

    /**
     * 商品当前剩余库存。
     *
     * <p>该字段用于展示 SKU 当前是否还可兑换，也用于后续库存扣减。
     * {@code stockCount} 是初始配置值，{@code stockCountSurplus} 是运行时剩余值。</p>
     */
    private Long stockCountSurplus;

    /**
     * 兑换一个该 SKU 所需消耗的积分数量。
     *
     * <p>值为 {@code null} 表示该 SKU 不支持积分兑换，
     * 或价格尚未由运营配置完成。</p>
     */
    private Long creditAmount;
}
