USE
`market-forge-001`;

DROP TABLE IF EXISTS `behavior_rebate_config`;

CREATE TABLE `behavior_rebate_config`
(
    `id`            bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `behavior_type` varchar(32)  NOT NULL COMMENT '行为类型：sign-签到',
    `rebate_type`   varchar(32)  NOT NULL COMMENT '返利类型：sku、integral',
    `rebate_config` varchar(128) NOT NULL COMMENT '返利配置值',
    `rebate_desc`   varchar(128) NULL COMMENT '返利说明',
    `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY             `idx_behavior_type` (`behavior_type`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='行为返利配置表';

DROP TABLE IF EXISTS `user_behavior_rebate_order`;

CREATE TABLE `user_behavior_rebate_order`
(
    `id`              bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `user_id`         varchar(32)  NOT NULL COMMENT '用户ID，同时作为分片键',
    `order_id`        varchar(32)  NOT NULL COMMENT '返利订单号',
    `behavior_type`   varchar(32)  NOT NULL COMMENT '行为类型',
    `rebate_type`     varchar(32)  NOT NULL COMMENT '返利类型',
    `rebate_config`   varchar(128) NOT NULL COMMENT '返利配置值',
    `biz_id`          varchar(128) NOT NULL COMMENT '下游发放幂等号',
    `out_business_no` varchar(64)  NOT NULL COMMENT '外部业务幂等号',
    `order_time`      datetime     NOT NULL COMMENT '订单时间',
    `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_order_id` (`order_id`),
    UNIQUE KEY `uq_biz_id` (`biz_id`),
    KEY               `idx_user_behavior` (`user_id`, `behavior_type`, `out_business_no`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户行为返利订单表';

USE
`market-forge-002`;

DROP TABLE IF EXISTS `user_behavior_rebate_order`;

CREATE TABLE `user_behavior_rebate_order`
(
    `id`              bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `user_id`         varchar(32)  NOT NULL COMMENT '用户ID，同时作为分片键',
    `order_id`        varchar(32)  NOT NULL COMMENT '返利订单号',
    `behavior_type`   varchar(32)  NOT NULL COMMENT '行为类型',
    `rebate_type`     varchar(32)  NOT NULL COMMENT '返利类型',
    `rebate_config`   varchar(128) NOT NULL COMMENT '返利配置值',
    `biz_id`          varchar(128) NOT NULL COMMENT '下游发放幂等号',
    `out_business_no` varchar(64)  NOT NULL COMMENT '外部业务幂等号',
    `order_time`      datetime     NOT NULL COMMENT '订单时间',
    `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_order_id` (`order_id`),
    UNIQUE KEY `uq_biz_id` (`biz_id`),
    KEY               `idx_user_behavior` (`user_id`, `behavior_type`, `out_business_no`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户行为返利订单表';
