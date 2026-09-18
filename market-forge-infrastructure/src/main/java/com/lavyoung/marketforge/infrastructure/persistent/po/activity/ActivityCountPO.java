package com.lavyoung.marketforge.infrastructure.persistent.po.activity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lavyoung.marketforge.infrastructure.persistent.po.BasePO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 抽奖活动参与次数配置持久化对象。
 * <p>
 * 定义单个用户可获得的总次数、每日次数与每月次数上限。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/18
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("raffle_activity_count")
public class ActivityCountPO extends BasePO {

    /**
     * 数据库自增主键。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 活动参与次数配置标识。
     */
    private Long activityCountId;

    /**
     * 可参与的总次数。
     */
    private Integer totalCount;

    /**
     * 每日可参与次数。
     */
    private Integer dayCount;

    /**
     * 每月可参与次数。
     */
    private Integer monthCount;
}
