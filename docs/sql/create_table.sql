-- auto-generated definition
create table award
(
    id           int auto_increment comment '自增id'
        primary key,
    award_key    varchar(32)                        not null comment '奖品key',
    award_config varchar(128)                       not null comment '奖品配置',
    award_desc   varchar(256)                       null comment '奖品描述',
    create_time  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    index idx_award_key (award_key)
)
    comment '奖品';


create table strategy
(
    id            int auto_increment comment '自增id'
        primary key,
    strategy_id   int                      not null comment '策略id',
    strategy_desc varchar(256)             not null comment '描述',
    rule_models   varchar(32)              null comment '规则模型',
    create_time   datetime default (now()) not null comment '创建时间',
    update_time   datetime default (now()) not null on update CURRENT_TIMESTAMP comment '更新时间',
    index idx_strategy_id (strategy_id)
)
    comment '抽奖策略表';

create table strategy_award
(
    id                  int auto_increment comment '自增id'
        primary key,
    strategy_id         int                                not null comment '抽奖策略id',
    award_id            int                                not null comment '奖品ID',
    award_title         varchar(128)                       not null comment '奖品标题',
    award_subtitle      varchar(128)                       null comment '奖品副标题',
    award_count         int      default 0                 not null comment '奖品库存总量',
    award_count_surplus int      default 0                 not null comment '奖品库存剩余量',
    award_rate          decimal  default 0                 not null comment '奖品中奖概率',
    rule_models         varchar(32)                        null comment '规则模型',
    sort                int                                not null comment '奖品顺序',
    create_time         datetime default (now())           not null comment '创建时间',
    update_time         datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    index idx_strategy_award (strategy_id, award_id),
    index idx_strategy_sort (strategy_id, sort)
)
    comment '抽奖详情表';

create table strategy_rule
(
    id          int auto_increment comment '自增id'
        primary key,
    strategy_id int                                not null comment '策略id',
    award_id    int                                null comment '奖品id',
    rule_type   tinyint                            not null comment '规则类型：【1-策略规则、2-奖品规则】',
    rule_model  varchar(32)                        not null comment '抽奖规则类型:【rule_lock】',
    rule_value  varchar(128)                       not null comment '抽奖规则比值',
    rule_desc   varchar(128)                       not null comment '抽奖规则描述',
    create_time datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time datetime default (now())           not null on update CURRENT_TIMESTAMP comment '更新时间',
    index idx_strategy_rule (strategy_id, award_id, rule_model)
)
    comment '策略规则';
