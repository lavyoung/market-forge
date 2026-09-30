USE
`market-forge-002`;

DROP TABLE IF EXISTS `raffle_activity_account`;

CREATE TABLE `raffle_activity_account`
(
    `id`                  bigint(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `user_id`             varchar(32) NOT NULL COMMENT '用户ID',
    `activity_id`         bigint(12) NOT NULL COMMENT '活动ID',
    `total_count`         int(8) NOT NULL COMMENT '总次数',
    `total_count_surplus` int(8) NOT NULL COMMENT '总次数-剩余',
    `day_count`           int(8) NOT NULL COMMENT '日次数',
    `day_count_surplus`   int(8) NOT NULL COMMENT '日次数-剩余',
    `month_count`         int(8) NOT NULL COMMENT '月次数',
    `month_count_surplus` int(8) NOT NULL COMMENT '月次数-剩余',
    `create_time`         datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_user_id_activity_id` (`user_id`,`activity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抽奖活动账户表';

DROP TABLE IF EXISTS `raffle_activity_account_day`;

CREATE TABLE `raffle_activity_account_day`
(
    `id`                int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `user_id`           varchar(32) NOT NULL COMMENT '用户ID',
    `activity_id`       bigint(12) NOT NULL COMMENT '活动ID',
    `day`               date        NOT NULL COMMENT '日期（yyyy-mm-dd）',
    `day_count`         int(8) NOT NULL COMMENT '日次数',
    `day_count_surplus` int(8) NOT NULL COMMENT '日次数-剩余',
    `version`           int(11) NOT NULL DEFAULT 1 COMMENT '版本号',
    `create_time`       datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_user_id_activity_id_day` (`user_id`,`activity_id`,`day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抽奖活动账户表-日次数';

#
转储表 raffle_activity_account_month
# ------------------------------------------------------------

DROP TABLE IF EXISTS `raffle_activity_account_month`;

CREATE TABLE `raffle_activity_account_month`
(
    `id`                  int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `user_id`             varchar(32) NOT NULL COMMENT '用户ID',
    `activity_id`         bigint(12) NOT NULL COMMENT '活动ID',
    `month`               varchar(7)  NOT NULL COMMENT '月（yyyy-mm）',
    `month_count`         int(8) NOT NULL COMMENT '月次数',
    `month_count_surplus` int(8) NOT NULL COMMENT '月次数-剩余',
    `version`             int(11) NOT NULL DEFAULT 1 COMMENT '版本号',
    `create_time`         datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_user_id_activity_id_month` (`user_id`,`activity_id`,`month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抽奖活动账户表-月次数';


#
转储表 task
# ------------------------------------------------------------


DROP TABLE IF EXISTS `task`;

CREATE TABLE `task`
(
    `id`           int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `user_id`         varchar(32) NOT NULL COMMENT '用户ID，同时作为分片键',
    `topic`        varchar(32)  NOT NULL COMMENT '消息主题',
    `event_id`     varchar(64)  NOT NULL COMMENT '消息ID',
    `event_type`   varchar(512) NOT NULL COMMENT '消息路由键',
    `message_body` text         NOT NULL COMMENT '消息路由键',
    `occurred_at`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '',
    `state`           varchar(16) NOT NULL DEFAULT 'create' COMMENT '任务状态；create-待投递、publishing-投递中、published-已投递、publish_failed-投递失败',
    `retry_count`     int unsigned NOT NULL DEFAULT 0 COMMENT '投递失败重试次数',
    `next_retry_time` datetime NULL COMMENT '下一次允许重试时间',
    `last_error`      varchar(1024) NULL COMMENT '最近一次投递失败原因',
    `create_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_event_id` (`event_id`),
    KEY               `idx_task_publish` (`state`, `next_retry_time`, `create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='任务表，发送MQ';


#
CREATE TABLE `processed_message`
(
    `id`          bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `message_id`  varchar(64)  NOT NULL COMMENT '消息唯一标识',
    `event_type`  varchar(128) NOT NULL COMMENT '事件类型',
    `occurred_at` datetime     NOT NULL COMMENT '事件发生时间',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_processed_message_id` (`message_id`),
    KEY           `idx_processed_message_create_time` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='已成功消费消息表';

#
转储表 user_award_record
# ------------------------------------------------------------

DROP TABLE IF EXISTS `user_award_record`;

CREATE TABLE `user_award_record`
(
    `id`          int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `user_id`     varchar(32)  NOT NULL COMMENT '用户ID',
    `activity_id` bigint(12) NOT NULL COMMENT '活动ID',
    `strategy_id` bigint(8) NOT NULL COMMENT '抽奖策略ID',
    `order_id`    varchar(12)  NOT NULL COMMENT '抽奖订单ID【作为幂等使用】',
    `award_id`    int(11) NOT NULL COMMENT '奖品ID',
    `award_title` varchar(128) NOT NULL COMMENT '奖品标题（名称）',
    `award_time`  datetime     NOT NULL COMMENT '中奖时间',
    `award_state` varchar(16) NOT NULL DEFAULT 'create' COMMENT '奖品状态；create-创建、wait_claim-待领取、granting-发奖中、grant_failed-发奖失败、completed-发奖完成',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_order_id` (`order_id`),
    KEY           `idx_user_id` (`user_id`),
    KEY           `idx_activity_id` (`activity_id`),
    KEY           `idx_strategy_id` (`strategy_id`),
    KEY           `idx_award_id` (`award_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户中奖记录表';


#
转储表 raffle_activity_order
# ------------------------------------------------------------

DROP TABLE IF EXISTS `raffle_activity_order`;

CREATE TABLE `raffle_activity_order`
(
    `id`            bigint(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `user_id`       varchar(32) NOT NULL COMMENT '用户ID',
    `activity_id`   bigint(12) NOT NULL COMMENT '活动ID',
    `activity_name` varchar(64) NOT NULL COMMENT '活动名称',
    `strategy_id`   bigint(8) NOT NULL COMMENT '抽奖策略ID',
    `order_id`      varchar(12) NOT NULL COMMENT '订单ID',
    `order_time`    datetime    NOT NULL COMMENT '下单时间',
    `state`         varchar(8)  NOT NULL COMMENT '订单状态（not_used、used、expire）',
    `create_time`   datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_order_id` (`order_id`),
    KEY             `idx_user_id_activity_id` (`user_id`,`activity_id`,`state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抽奖活动单';

