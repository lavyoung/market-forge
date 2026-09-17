USE
`market-forge-002`;

CREATE TABLE `raffle_activity_order`
(
    `id`            bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `user_id`       varchar(32) NOT NULL COMMENT '用户ID',
    `activity_id`   bigint      NOT NULL COMMENT '活动ID',
    `activity_name` varchar(64) NOT NULL COMMENT '活动名称',
    `strategy_id`   bigint      NOT NULL COMMENT '抽奖策略ID',
    `order_id`      varchar(12) NOT NULL COMMENT '订单ID',
    `order_time`    datetime    NOT NULL COMMENT '下单时间',
    `state`         varchar(8)  NOT NULL COMMENT '订单状态',
    `create_time`   datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_order_id` (`order_id`),
    KEY             `idx_user_id_activity_id` (`user_id`, `activity_id`, `state`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT ='抽奖活动单';

CREATE TABLE `raffle_activity_account`
(
    `id`                  bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `user_id`             varchar(32) NOT NULL COMMENT '用户ID',
    `activity_id`         bigint      NOT NULL COMMENT '活动ID',
    `total_count`         int         NOT NULL COMMENT '总次数',
    `total_count_surplus` int         NOT NULL COMMENT '总次数-剩余',
    `day_count`           int         NOT NULL COMMENT '日次数',
    `day_count_surplus`   int         NOT NULL COMMENT '日次数-剩余',
    `month_count`         int         NOT NULL COMMENT '月次数',
    `month_count_surplus` int         NOT NULL COMMENT '月次数-剩余',
    `create_time`         datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_user_id_activity_id` (`user_id`, `activity_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT ='抽奖活动账户表';

CREATE TABLE `raffle_activity_account_flow`
(
    `id`           int unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `user_id`      varchar(32) NOT NULL COMMENT '用户ID',
    `activity_id`  bigint      NOT NULL COMMENT '活动ID',
    `total_count`  int         NOT NULL COMMENT '总次数',
    `day_count`    int         NOT NULL COMMENT '日次数',
    `month_count`  int         NOT NULL COMMENT '月次数',
    `flow_id`      varchar(32) NOT NULL COMMENT '流水ID',
    `flow_channel` varchar(12) NOT NULL DEFAULT 'activity' COMMENT '流水渠道',
    `biz_id`       varchar(12) NOT NULL COMMENT '业务ID',
    `create_time`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_flow_id` (`flow_id`),
    UNIQUE KEY `uq_biz_id` (`biz_id`),
    KEY            `idx_user_id_activity_id` (`user_id`, `activity_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT ='抽奖活动账户流水表';