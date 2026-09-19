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
 * <p>
 * 保存可售 SKU 与活动、次数配置及库存之间的关联关系。
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
}
