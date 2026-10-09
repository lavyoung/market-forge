package com.lavyoung.marketforge.infrastructure.persistent.po.behavior;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lavyoung.marketforge.infrastructure.persistent.po.BasePO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 用户行为返利订单持久化对象。
 *
 * <p>对应 {@code user_behavior_rebate_order} 表，记录某个用户因一次行为生成的具体返利。
 * 该表需要通过唯一索引保证同一笔返利不会重复入账。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/08
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("user_behavior_rebate_order")
public class UserBehaviorRebateOrderPO extends BasePO {

    /**
     * 数据库自增主键。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户标识，同时作为分库路由键。
     */
    private String userId;

    /**
     * 返利订单号。
     */
    private String orderId;

    /**
     * 行为类型编码，例如 {@code sign}。
     */
    private String behaviorType;

    /**
     * 返利类型编码，例如 {@code sku}、{@code integral}。
     */
    private String rebateType;

    /**
     * 返利配置值。
     */
    private String rebateConfig;

    /**
     * 下游发放幂等标识，例如 {@code xiaofuge_sku_20240429}。
     */
    private String bizId;

    /**
     * 外部业务幂等号，例如签到日期 {@code 20240429}。
     */
    private String outBusinessNo;

    /**
     * 返利订单创建时间。
     */
    private LocalDateTime orderTime;
}
