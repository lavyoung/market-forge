package com.lavyoung.marketforge.infrastructure.persistent.po.activity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lavyoung.marketforge.infrastructure.persistent.po.BasePO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 活动展台发布记录持久化对象。
 *
 * <p>对应 {@code raffle_activity_stage_publication} 表，保存某个展台版本的发布状态、
 * 执行阶段、重试次数和失败信息。活动、渠道和来源以快照形式保存，避免展台后续修改
 * 影响历史发布记录。</p>
 *
 * <p>该对象只负责数据库字段映射，不承担发布状态迁移规则。状态迁移合法性由
 * {@code ActivityStagePublicationEntity} 负责校验。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/05
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("raffle_activity_stage_publication")
public class ActivityStagePublicationPO extends BasePO {

    /**
     * 数据库自增主键。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 发布任务业务标识。
     */
    private Long publicationId;

    /**
     * 活动展台业务标识。
     */
    private Long stageId;

    /**
     * 本次发布对应的展台乐观锁版本。
     */
    private Integer stageVersion;

    /**
     * 本次发布的活动业务标识快照。
     */
    private Long activityId;

    /**
     * 投放渠道快照。
     */
    private String channel;

    /**
     * 流量来源快照。
     */
    private String source;

    /**
     * 发布任务状态编码。
     */
    private String state;

    /**
     * 发布执行阶段编码。
     */
    private String phase;

    /**
     * 已执行的发布尝试次数。
     */
    private Integer attemptCount;

    /**
     * 最近一次执行发布操作的运营人员。
     */
    private String operator;

    /**
     * 最近一次失败的稳定错误码。
     */
    private String failureCode;

    /**
     * 最近一次失败的安全说明。
     */
    private String failureReason;

    /**
     * 发布成功时间。
     */
    private LocalDateTime publishedAt;
}
