package com.lavyoung.marketforge.domain.message.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 本地消息任务状态。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/30
 */
@Getter
@AllArgsConstructor
public enum TaskStateVO {

    /**
     * 待投递。
     */
    CREATE("create", "待投递"),

    /**
     * 投递处理中。
     */
    PUBLISHING("publishing", "投递中"),

    /**
     * 已投递完成。
     */
    PUBLISHED("published", "已投递"),

    /**
     * 投递失败，等待补偿重试。
     */
    PUBLISH_FAILED("publish_failed", "投递失败");

    private final String code;

    private final String desc;
}
