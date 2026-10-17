-- Market Forge MVP database initialization.
--
-- Destructive by design: this script drops and recreates both development
-- databases. Use only for local/MVP quick start, never for production upgrade.

DROP DATABASE IF EXISTS `market-forge-001`;
DROP DATABASE IF EXISTS `market-forge-002`;

CREATE DATABASE `market-forge-001`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

CREATE DATABASE `market-forge-002`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE `market-forge-001`;

CREATE TABLE award
(
    id           int          NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    award_id     bigint       NOT NULL COMMENT '奖品业务ID',
    award_key    varchar(32)  NOT NULL COMMENT '奖品业务Key',
    award_config varchar(128) NOT NULL COMMENT '奖品配置',
    award_desc   varchar(256) NULL COMMENT '奖品描述',
    create_time  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uq_award_id (award_id),
    KEY idx_award_key (award_key)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='奖品表';

CREATE TABLE strategy
(
    id            int          NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    strategy_id   bigint       NOT NULL COMMENT '抽奖策略ID',
    strategy_desc varchar(256) NOT NULL COMMENT '策略描述',
    rule_models   varchar(64)  NULL COMMENT '策略级责任链规则模型',
    create_time   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uq_strategy_id (strategy_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='抽奖策略表';

CREATE TABLE strategy_award
(
    id                  int            NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    strategy_id         bigint         NOT NULL COMMENT '抽奖策略ID',
    award_id            bigint         NOT NULL COMMENT '奖品ID',
    award_title         varchar(128)   NOT NULL COMMENT '奖品标题',
    award_subtitle      varchar(128)   NULL COMMENT '奖品副标题',
    award_count         int            NOT NULL DEFAULT 0 COMMENT '奖品库存总量',
    award_count_surplus int            NOT NULL DEFAULT 0 COMMENT '奖品库存剩余量',
    award_rate          decimal(10, 5) NOT NULL DEFAULT 0.00000 COMMENT '奖品中奖概率',
    rule_models         varchar(64)    NULL COMMENT '奖品规则模型',
    sort                int            NOT NULL COMMENT '奖品顺序',
    create_time         datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uq_strategy_award (strategy_id, award_id),
    KEY idx_strategy_sort (strategy_id, sort)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='抽奖策略奖品表';

CREATE TABLE strategy_rule
(
    id          int          NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    strategy_id bigint       NOT NULL COMMENT '策略ID',
    award_id    bigint       NULL COMMENT '奖品ID；为空表示策略级规则',
    rule_type   tinyint      NOT NULL COMMENT '规则类型：1-策略规则、2-奖品规则',
    rule_model  varchar(32)  NOT NULL COMMENT '规则模型',
    rule_value  varchar(256) NOT NULL COMMENT '规则值',
    rule_desc   varchar(256) NOT NULL COMMENT '规则描述',
    create_time datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_strategy_rule (strategy_id, award_id, rule_model)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='抽奖策略规则表';

CREATE TABLE rule_tree
(
    id                 int          NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    tree_id            varchar(128) NOT NULL COMMENT '规则树ID',
    tree_desc          varchar(512) NULL COMMENT '规则树描述',
    tree_node_rule_key varchar(64)  NOT NULL COMMENT '根节点规则Key',
    create_time        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uq_tree_id (tree_id),
    KEY idx_root_rule_key (tree_node_rule_key)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='规则树表';

CREATE TABLE rule_tree_node
(
    id          int          NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    tree_id     varchar(128) NOT NULL COMMENT '规则树ID',
    rule_key    varchar(64)  NOT NULL COMMENT '规则Key',
    rule_desc   varchar(512) NULL COMMENT '规则描述',
    rule_value  varchar(128) NOT NULL COMMENT '规则值',
    create_time datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_tree_rule (tree_id, rule_key)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='规则树节点表';

CREATE TABLE rule_tree_node_line
(
    id               int          NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    tree_id          varchar(128) NOT NULL COMMENT '规则树ID',
    rule_node_from   varchar(64)  NOT NULL COMMENT '来源节点',
    rule_node_to     varchar(64)  NOT NULL COMMENT '目标节点',
    rule_limit_type  varchar(64)  NULL COMMENT '连接条件类型',
    rule_limit_value varchar(128) NOT NULL COMMENT '连接条件值',
    create_time      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_tree_from (tree_id, rule_node_from)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='规则树节点连线表';

CREATE TABLE raffle_activity
(
    id                  bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    activity_id         bigint          NOT NULL COMMENT '活动ID',
    activity_name       varchar(64)     NOT NULL COMMENT '活动名称',
    activity_desc       varchar(128)    NOT NULL COMMENT '活动描述',
    begin_date_time     datetime        NOT NULL COMMENT '开始时间',
    end_date_time       datetime        NOT NULL COMMENT '结束时间',
    stock_count         int             NOT NULL COMMENT '库存总量',
    stock_count_surplus int             NOT NULL COMMENT '剩余库存',
    activity_count_id   bigint          NOT NULL COMMENT '活动次数配置ID',
    strategy_id         bigint          NOT NULL COMMENT '抽奖策略ID',
    award_unlock_scope varchar(16) NOT NULL COMMENT '奖品解锁次数范围：day-当日、total-活动累计',
    state               varchar(16)     NOT NULL COMMENT '活动状态',
    create_time         datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uq_activity_id (activity_id),
    KEY idx_activity_time (begin_date_time, end_date_time)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='抽奖活动表';

CREATE TABLE raffle_activity_count
(
    id                bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    activity_count_id bigint          NOT NULL COMMENT '活动次数配置ID',
    total_count       int             NOT NULL COMMENT '总次数',
    day_count         int             NOT NULL COMMENT '日次数',
    month_count       int             NOT NULL COMMENT '月次数',
    create_time       datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uq_activity_count_id (activity_count_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='抽奖活动次数配置表';

CREATE TABLE raffle_activity_sku
(
    id                  bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    sku                 bigint          NOT NULL COMMENT '活动SKU',
    activity_id         bigint          NOT NULL COMMENT '活动ID',
    activity_count_id   bigint          NOT NULL COMMENT '活动次数配置ID',
    stock_count         bigint          NOT NULL COMMENT 'SKU库存总量',
    stock_count_surplus bigint          NOT NULL COMMENT 'SKU剩余库存',
    credit_amount       bigint          NULL COMMENT '兑换所需积分；为空表示不支持积分兑换',
    create_time         datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uq_sku_activity_count (sku, activity_id, activity_count_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='抽奖活动SKU表';

CREATE TABLE behavior_rebate_config
(
    id            bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    behavior_type varchar(32) NOT NULL COMMENT '行为类型',
    rebate_type   varchar(32)     NOT NULL COMMENT '返利类型：sku、integral',
    rebate_config varchar(128)    NOT NULL COMMENT '返利配置值',
    rebate_desc   varchar(128)    NULL COMMENT '返利说明',
    create_time   datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_behavior_type (behavior_type)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='行为返利配置表';

CREATE TABLE processed_message
(
    id          bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    message_id  varchar(64)     NOT NULL COMMENT '消息唯一标识',
    event_type  varchar(128)    NOT NULL COMMENT '事件类型',
    occurred_at datetime        NOT NULL COMMENT '事件发生时间',
    create_time datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uq_processed_message_id (message_id),
    KEY idx_processed_message_create_time (create_time)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='已成功消费消息表';

CREATE TABLE raffle_activity_account
(
    id                  bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    user_id             varchar(32)     NOT NULL COMMENT '用户ID',
    activity_id         bigint          NOT NULL COMMENT '活动ID',
    total_count         int             NOT NULL COMMENT '总次数',
    total_count_surplus int             NOT NULL COMMENT '总次数剩余',
    day_count           int             NOT NULL COMMENT '日次数',
    day_count_surplus   int             NOT NULL COMMENT '日次数剩余',
    month_count         int             NOT NULL COMMENT '月次数',
    month_count_surplus int             NOT NULL COMMENT '月次数剩余',
    version             int             NOT NULL DEFAULT 1 COMMENT '乐观锁版本',
    create_time         datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uq_user_activity (user_id, activity_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='抽奖活动账户表';

CREATE TABLE raffle_activity_account_day
(
    id                bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    user_id           varchar(32)     NOT NULL COMMENT '用户ID',
    activity_id       bigint          NOT NULL COMMENT '活动ID',
    day               date            NOT NULL COMMENT '日期',
    day_count         int             NOT NULL COMMENT '日次数',
    day_count_surplus int             NOT NULL COMMENT '日次数剩余',
    version           int             NOT NULL DEFAULT 1 COMMENT '乐观锁版本',
    create_time       datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uq_user_activity_day (user_id, activity_id, day)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='抽奖活动账户日次数表';

CREATE TABLE raffle_activity_account_month
(
    id                  bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    user_id             varchar(32)     NOT NULL COMMENT '用户ID',
    activity_id         bigint          NOT NULL COMMENT '活动ID',
    month               varchar(7)      NOT NULL COMMENT '月份 yyyy-MM',
    month_count         int             NOT NULL COMMENT '月次数',
    month_count_surplus int             NOT NULL COMMENT '月次数剩余',
    version             int             NOT NULL DEFAULT 1 COMMENT '乐观锁版本',
    create_time         datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uq_user_activity_month (user_id, activity_id, month)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='抽奖活动账户月次数表';

CREATE TABLE task
(
    id              bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    user_id         varchar(32)     NOT NULL COMMENT '用户ID，同时作为分片键',
    topic           varchar(64)     NOT NULL COMMENT '消息主题或交换机',
    event_id        varchar(64)     NOT NULL COMMENT '事件ID',
    event_type      varchar(128)    NOT NULL COMMENT '事件类型或路由键',
    message_body    text            NOT NULL COMMENT '消息体',
    occurred_at     datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '事件发生时间',
    state           varchar(16)     NOT NULL DEFAULT 'create' COMMENT '任务状态',
    retry_count     int unsigned    NOT NULL DEFAULT 0 COMMENT '重试次数',
    next_retry_time datetime        NULL COMMENT '下一次重试时间',
    last_error      varchar(1024)   NULL COMMENT '最近失败原因',
    create_time     datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uq_event_id (event_id),
    KEY idx_task_publish (state, next_retry_time, create_time)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='可靠消息任务表';

CREATE TABLE user_award_record
(
    id          bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    user_id     varchar(32)     NOT NULL COMMENT '用户ID',
    activity_id bigint          NOT NULL COMMENT '活动ID',
    strategy_id bigint          NOT NULL COMMENT '抽奖策略ID',
    order_id    varchar(32)     NOT NULL COMMENT '抽奖订单ID',
    award_id    bigint          NOT NULL COMMENT '奖品ID',
    award_title varchar(128)    NOT NULL COMMENT '奖品标题',
    award_time  datetime        NOT NULL COMMENT '中奖时间',
    award_state varchar(16)     NOT NULL DEFAULT 'create' COMMENT '发奖状态',
    create_time datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uq_order_id (order_id),
    KEY idx_user_activity_award_time (user_id, activity_id, award_time),
    KEY idx_award_id (award_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户中奖记录表';

CREATE TABLE raffle_activity_order
(
    id              bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    user_id         varchar(32)     NOT NULL COMMENT '用户ID',
    activity_id     bigint          NOT NULL COMMENT '活动ID',
    sku             bigint          NOT NULL COMMENT '活动SKU',
    activity_name   varchar(64)     NOT NULL COMMENT '活动名称快照',
    strategy_id     bigint          NOT NULL COMMENT '抽奖策略ID',
    order_id        varchar(32)     NOT NULL COMMENT '活动订单ID',
    order_time      datetime        NOT NULL COMMENT '下单时间',
    total_count     int             NOT NULL DEFAULT 0 COMMENT '总次数',
    day_count       int             NOT NULL DEFAULT 0 COMMENT '日次数',
    month_count     int             NOT NULL DEFAULT 0 COMMENT '月次数',
    state           varchar(16)     NOT NULL COMMENT '订单状态',
    out_business_no varchar(64)     NULL COMMENT '外部业务幂等号',
    create_time     datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uq_order_id (order_id),
    UNIQUE KEY uq_user_out_business_no (user_id, out_business_no),
    KEY idx_user_activity_state (user_id, activity_id, state)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='抽奖活动订单表';

CREATE TABLE user_behavior_rebate_order
(
    id              bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    user_id         varchar(32)     NOT NULL COMMENT '用户ID',
    order_id        varchar(32)     NOT NULL COMMENT '返利订单号',
    behavior_type   varchar(32)     NOT NULL COMMENT '行为类型',
    rebate_type     varchar(32)     NOT NULL COMMENT '返利类型',
    rebate_config   varchar(128)    NOT NULL COMMENT '返利配置值',
    biz_id          varchar(128)    NOT NULL COMMENT '下游发放幂等号',
    out_business_no varchar(64)     NOT NULL COMMENT '外部业务幂等号',
    order_time      datetime        NOT NULL COMMENT '订单时间',
    create_time     datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uq_order_id (order_id),
    UNIQUE KEY uq_biz_id (biz_id),
    KEY idx_user_behavior (user_id, behavior_type, out_business_no)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户行为返利订单表';

CREATE TABLE user_credit_account
(
    id               bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    user_id          varchar(32)     NOT NULL COMMENT '用户ID',
    total_amount     bigint          NOT NULL DEFAULT 0 COMMENT '累计获得积分总额',
    available_amount bigint          NOT NULL DEFAULT 0 COMMENT '当前可用积分余额',
    create_time      datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time      datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uq_user_id (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户积分账户表';

CREATE TABLE user_credit_account_order
(
    id            bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    user_id       varchar(32)     NOT NULL COMMENT '用户ID',
    order_id      varchar(32)     NOT NULL COMMENT '积分账户订单号',
    biz_id        varchar(128) NOT NULL COMMENT '业务幂等号',
    trade_type    varchar(32)  NOT NULL COMMENT '交易类型',
    business_type varchar(32)  NOT NULL COMMENT '业务类型',
    amount        bigint       NOT NULL COMMENT '积分变动数量',
    state         varchar(32)  NOT NULL COMMENT '订单状态',
    order_time    datetime        NOT NULL COMMENT '订单时间',
    create_time   datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uq_order_id (order_id),
    UNIQUE KEY uq_biz_id (biz_id),
    KEY idx_user_order_time (user_id, order_time)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户积分账户订单表';

USE `market-forge-002`;

CREATE TABLE raffle_activity_account LIKE `market-forge-001`.raffle_activity_account;
CREATE TABLE raffle_activity_account_day LIKE `market-forge-001`.raffle_activity_account_day;
CREATE TABLE raffle_activity_account_month LIKE `market-forge-001`.raffle_activity_account_month;
CREATE TABLE task LIKE `market-forge-001`.task;
CREATE TABLE user_award_record LIKE `market-forge-001`.user_award_record;
CREATE TABLE raffle_activity_order LIKE `market-forge-001`.raffle_activity_order;
CREATE TABLE user_behavior_rebate_order LIKE `market-forge-001`.user_behavior_rebate_order;
CREATE TABLE user_credit_account LIKE `market-forge-001`.user_credit_account;
CREATE TABLE user_credit_account_order LIKE `market-forge-001`.user_credit_account_order;
