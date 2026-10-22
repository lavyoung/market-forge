package com.lavyoung.marketforge.infrastructure.persistent.po.activity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lavyoung.marketforge.infrastructure.persistent.po.BasePO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 活动展台持久化对象。
 *
 * <p>对应 {@code raffle_activity_stage} 表，保存渠道、来源、关联活动、生命周期状态、
 * 乐观锁版本和运营审计信息。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("raffle_activity_stage")
public class ActivityStagePO extends BasePO {

    /**
     * 数据库自增主键。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 展台业务标识。
     */
    private Long stageId;

    /**
     * 投放渠道。
     */
    private String channel;

    /**
     * 流量来源。
     */
    private String source;

    /**
     * 上架的活动标识。
     */
    private Long activityId;

    /**
     * 展台生命周期状态编码。
     */
    private String state;

    /**
     * 乐观锁版本。
     */
    private Integer version;

    /**
     * 创建人标识。
     */
    private String createdBy;

    /**
     * 最后修改人标识。
     */
    private String updatedBy;
}
