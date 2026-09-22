-- auto-generated definition
create table award
(
    id           int auto_increment comment '自增id'
        primary key,
    award_key    varchar(32)                        not null comment '奖品key',
    award_config varchar(128)                       not null comment '奖品配置',
    award_desc varchar(256) null comment '奖品描述',
    create_time  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    index idx_award_key (award_key)
) comment '奖品';


create table strategy
(
    id            int auto_increment comment '自增id'
        primary key,
    strategy_id   int                      not null comment '策略id',
    strategy_desc varchar(256)             not null comment '描述',
    rule_models varchar(32) null comment '规则模型',
    create_time   datetime default (now()) not null comment '创建时间',
    update_time   datetime default (now()) not null on update CURRENT_TIMESTAMP comment '更新时间',
    index idx_strategy_id (strategy_id)
) comment '抽奖策略表';

create table strategy_award
(
    id                  int auto_increment comment '自增id'
        primary key,
    strategy_id         int                                      not null comment '抽奖策略id',
    award_id            int                                      not null comment '奖品ID',
    award_title         varchar(128)                             not null comment '奖品标题',
    award_subtitle varchar(128) null comment '奖品副标题',
    award_count         int            default 0                 not null comment '奖品库存总量',
    award_count_surplus int            default 0                 not null comment '奖品库存剩余量',
    award_rate          decimal(10, 5) default 0.00000           not null comment '奖品中奖概率',
    rule_models    varchar(32)  null comment '规则模型',
    sort                int                                      not null comment '奖品顺序',
    create_time         datetime       default (now())           not null comment '创建时间',
    update_time         datetime       default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    index idx_strategy_award (strategy_id, award_id),
    index idx_strategy_sort (strategy_id, sort)
) comment '抽奖详情表';

create table strategy_rule
(
    id          int auto_increment comment '自增id'
        primary key,
    strategy_id int                                not null comment '策略id',
    award_id int null comment '奖品id',
    rule_type   tinyint                            not null comment '规则类型：【1-策略规则、2-奖品规则】',
    rule_model  varchar(32)                        not null comment '抽奖规则类型:【rule_lock】',
    rule_value  varchar(128)                       not null comment '抽奖规则比值',
    rule_desc   varchar(128)                       not null comment '抽奖规则描述',
    create_time datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time datetime default (now())           not null on update CURRENT_TIMESTAMP comment '更新时间',
    index idx_strategy_rule (strategy_id, award_id, rule_model)
) comment '策略规则';


create table rule_tree
(
    id                 int auto_increment comment '自增ID' primary key,
    tree_id            varchar(128)                       not null comment '规则树ID',
    tree_desc   varchar(512)             null comment '规则树描述',
    tree_node_rule_key varchar(64)                        not null comment '规则树描述',
    create_time        datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time datetime default (now()) not null on update CURRENT_TIMESTAMP comment '更新时间'
) comment '规则树';


create table rule_tree_node
(
    id          int auto_increment comment '自增ID' primary key,
    tree_id     varchar(128)                       not null comment '规则树ID',
    rule_key    varchar(64)                        not null comment '规则key',
    rule_desc   varchar(512)             null comment '规则描述',
    rule_value  varchar(128)                       not null comment '规则值',
    create_time datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time datetime default (now()) not null on update CURRENT_TIMESTAMP comment '更新时间'
) comment '规则树节点';

create table rule_tree_node_line
(
    id               int auto_increment comment '自增ID' primary key,
    tree_id          varchar(128)                       not null comment '规则树ID',
    rule_node_from   varchar(64)                        not null comment '节点FROM',
    rule_node_to     varchar(64)                        not null comment '节点TO',
    rule_limit_type varchar(512)             null comment '链接符号',
    rule_limit_value varchar(128)                       not null comment '目标值',
    create_time      datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time     datetime default (now()) not null on update CURRENT_TIMESTAMP comment '更新时间'
) comment '规则树节点连线规则';


-- 活动相关
CREATE TABLE `raffle_activity`
(
    `id`                  bigint(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `activity_id`         bigint(12)          NOT NULL COMMENT '活动ID',
    `activity_name`       varchar(64)         NOT NULL COMMENT '活动名称',
    `activity_desc`       varchar(128)        NOT NULL COMMENT '活动描述',
    `begin_date_time`     datetime            NOT NULL COMMENT '开始时间',
    `end_date_time`       datetime            NOT NULL COMMENT '结束时间',
    `stock_count`         int(11)             NOT NULL COMMENT '库存总量',
    `stock_count_surplus` int(11)             NOT NULL COMMENT '剩余库存',
    `activity_count_id`   bigint(12)          NOT NULL COMMENT '活动参与次数配置',
    `strategy_id`         bigint(8)           NOT NULL COMMENT '抽奖策略ID',
    `state`               varchar(8)          NOT NULL COMMENT '活动状态',
    `create_time`         datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_activity_id` (`activity_id`),
    KEY `idx_begin_date_time` (`begin_date_time`),
    KEY `idx_end_date_time` (`end_date_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='抽奖活动表';

CREATE TABLE `raffle_activity_count`
(
    `id`                bigint(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `activity_count_id` bigint(12)          NOT NULL COMMENT '活动次数编号',
    `total_count`       int(8)              NOT NULL COMMENT '总次数',
    `day_count`         int(8)              NOT NULL COMMENT '日次数',
    `month_count`       int(8)              NOT NULL COMMENT '月次数',
    `create_time`       datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_activity_count_id` (`activity_count_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='抽奖活动次数配置表';

DROP TABLE IF EXISTS `raffle_activity_account`;

CREATE TABLE `raffle_activity_account`
(
    `id`                  bigint(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `user_id`             varchar(32)         NOT NULL COMMENT '用户ID',
    `activity_id`         bigint(12)          NOT NULL COMMENT '活动ID',
    `total_count`         int(8)              NOT NULL COMMENT '总次数',
    `total_count_surplus` int(8)              NOT NULL COMMENT '总次数-剩余',
    `day_count`           int(8)              NOT NULL COMMENT '日次数',
    `day_count_surplus`   int(8)              NOT NULL COMMENT '日次数-剩余',
    `month_count`         int(8)              NOT NULL COMMENT '月次数',
    `month_count_surplus` int(8)              NOT NULL COMMENT '月次数-剩余',
    `create_time`         datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_user_id_activity_id` (`user_id`, `activity_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='抽奖活动账户表';

DROP TABLE IF EXISTS `raffle_activity_account_day`;

CREATE TABLE `raffle_activity_account_day`
(
    `id`                int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `user_id`           varchar(32)      NOT NULL COMMENT '用户ID',
    `activity_id`       bigint(12)       NOT NULL COMMENT '活动ID',
    `day`               date             NOT NULL COMMENT '日期（yyyy-mm-dd）',
    `day_count`         int(8)           NOT NULL COMMENT '日次数',
    `day_count_surplus` int(8)           NOT NULL COMMENT '日次数-剩余',
    `version`           int(11)          NOT NULL DEFAULT 1 COMMENT '版本号',
    `create_time`       datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_user_id_activity_id_day` (`user_id`, `activity_id`, `day`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='抽奖活动账户表-日次数';



# 转储表 raffle_activity_account_month
# ------------------------------------------------------------

DROP TABLE IF EXISTS `raffle_activity_account_month`;

CREATE TABLE `raffle_activity_account_month`
(
    `id`                  int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `user_id`             varchar(32)      NOT NULL COMMENT '用户ID',
    `activity_id`         bigint(12)       NOT NULL COMMENT '活动ID',
    `month`               varchar(7)       NOT NULL COMMENT '月（yyyy-mm）',
    `month_count`         int(8)           NOT NULL COMMENT '月次数',
    `month_count_surplus` int(8)           NOT NULL COMMENT '月次数-剩余',
    `version`             int(11)          NOT NULL DEFAULT 1 COMMENT '版本号',
    `create_time`         datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_user_id_activity_id_month` (`user_id`, `activity_id`, `month`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='抽奖活动账户表-月次数';



DROP TABLE IF EXISTS `raffle_activity_sku`;

CREATE TABLE `raffle_activity_sku`
(
    `id`                bigint(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `sku`               bigint(12)          NOT NULL COMMENT '商品sku ',
    `activity_id`       bigint(12)          NOT NULL COMMENT '活动ID',
    `activity_count_id` bigint(12)          NOT NULL COMMENT '活动次数编号',
    `stock_count`       bigint(12)          NOT NULL COMMENT '商品库存',
    `create_time`       datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_sku_activity_id_activity_count_id` (`sku`, `activity_id`, `activity_count_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='抽奖活动sku表';



# 转储表 task
# ------------------------------------------------------------

DROP TABLE IF EXISTS `task`;

CREATE TABLE `task`
(
    `id`           int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `topic`        varchar(32)      NOT NULL COMMENT '消息主题',
    `event_id`     varchar(64)      NOT NULL COMMENT '消息ID',
    `event_type`   varchar(512)     NOT NULL COMMENT '消息路由键',
    `message_body` text             NOT NULL COMMENT '消息路由键',
    `occurred_at`  datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '',
    `state`        varchar(16)      NOT NULL DEFAULT 'create' COMMENT '任务状态；create-创建、completed-完成、fail-失败',
    `create_time`  datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_event_id` (`event_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='任务表，发送MQ';



# 转储表 user_award_record
# ------------------------------------------------------------

DROP TABLE IF EXISTS `user_award_record`;

CREATE TABLE `user_award_record`
(
    `id`          int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `user_id`     varchar(32)      NOT NULL COMMENT '用户ID',
    `activity_id` bigint(12)       NOT NULL COMMENT '活动ID',
    `strategy_id` bigint(8)        NOT NULL COMMENT '抽奖策略ID',
    `order_id`    varchar(12)      NOT NULL COMMENT '抽奖订单ID【作为幂等使用】',
    `award_id`    int(11)          NOT NULL COMMENT '奖品ID',
    `award_title` varchar(128)     NOT NULL COMMENT '奖品标题（名称）',
    `award_time`  datetime         NOT NULL COMMENT '中奖时间',
    `award_state` varchar(16)      NOT NULL DEFAULT 'create' COMMENT '奖品状态；create-创建、completed-发奖完成',
    `create_time` datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_order_id` (`order_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_activity_id` (`activity_id`),
    KEY `idx_strategy_id` (`strategy_id`),
    KEY `idx_award_id` (`award_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户中奖记录表';


# 转储表 raffle_activity_order
# ------------------------------------------------------------

DROP TABLE IF EXISTS `raffle_activity_order`;

CREATE TABLE `raffle_activity_order`
(
    `id`              bigint(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `user_id`         varchar(32)         NOT NULL COMMENT '用户ID',
    `activity_id`     bigint(12)          NOT NULL COMMENT '活动ID',
    `sku`             bigint(12)          NOT NULL COMMENT '商品sku',
    `activity_name`   varchar(64)         NOT NULL COMMENT '活动名称',
    `strategy_id`     bigint(8)           NOT NULL COMMENT '抽奖策略ID',
    `order_id`        varchar(12)         NOT NULL COMMENT '订单ID',
    `order_time`      datetime            NOT NULL COMMENT '下单时间',
    `total_count`     int(11)             NOT NULL DEFAULT 0 COMMENT '总次数',
    `day_count`       int(11)             NOT NULL DEFAULT 0 COMMENT '日次数',
    `month_count`     int(11)             NOT NULL DEFAULT 0 COMMENT '月次数',
    `state`           varchar(8)          NOT NULL COMMENT '订单状态（not_used、used、expire、complete）',
    `out_business_no` varchar(64)         NULL COMMENT '业务防重ID',
    `create_time`     datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime            NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_order_id` (`order_id`),
    KEY `idx_user_id_activity_id` (`user_id`, `activity_id`, `state`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='抽奖活动单';

