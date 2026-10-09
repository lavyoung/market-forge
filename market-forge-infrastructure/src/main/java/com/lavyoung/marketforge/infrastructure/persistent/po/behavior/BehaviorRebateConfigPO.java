package com.lavyoung.marketforge.infrastructure.persistent.po.behavior;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lavyoung.marketforge.infrastructure.persistent.po.BasePO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 用户行为返利配置持久化对象。
 *
 * <p>对应 {@code behavior_rebate_config} 表，用于配置某类用户行为可以获得哪些返利。
 * 例如签到行为可以同时配置 SKU 返利和积分返利。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/08
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("behavior_rebate_config")
public class BehaviorRebateConfigPO extends BasePO {
    /**
     * 数据库自增主键。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 行为类型编码，例如 {@code sign}。
     */
    private String behaviorType;

    /**
     * 返利类型编码，例如 {@code sku}、{@code integral}。
     */
    private String rebateType;

    /**
     * 返利配置值，例如 SKU 编号 {@code 9011} 或积分数量 {@code 10}。
     */
    private String rebateConfig;

    /**
     * 返利说明。
     */
    private String rebateDesc;
}
