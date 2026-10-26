-- Market Forge MVP quick-start seed data.
--
-- Run after docs/sql/create_table.sql. This script assumes a clean database.

USE `market-forge-001`;

INSERT INTO award
(id, award_id, award_key, award_name, award_type, claim_mode, provider_code,
 award_config, config_version, award_desc, state, version, created_by, updated_by)
VALUES (900901011, 900901011, 'integral', '10 积分', 'internal_credit', 'automatic', NULL,
        '{"amount":10}', 1, '抽奖后自动发放 10 积分', 'active', 1, 'system_seed', 'system_seed'),
       (900901012, 900901012, 'course_coupon_50', '课程 5 折兑换券', 'third_party_coupon', 'automatic',
        'course_center', '{"templateCode":"course_coupon_50","discountPercent":50}', 1,
        '指定课程可用的 5 折兑换券', 'active', 1, 'system_seed', 'system_seed'),
       (900901013, 900901013, 'mentholatum_lip_balm', '曼秀雷敦唇膏', 'physical_goods', 'manual',
        'internal_fulfillment', '{"skuCode":"MF-LIP-001"}', 1,
        '需要用户填写收货信息', 'active', 1, 'system_seed', 'system_seed'),
       (900901014, 900901014, 'blind_box_4201', '4201 随机盲盒', 'physical_goods', 'manual',
        'internal_fulfillment', '{"seriesCode":"4201"}', 1,
        '需要用户填写收货信息', 'active', 1, 'system_seed', 'system_seed'),
       (900901015, 900901015, 'sleep_night_light', '睡眠日小夜灯', 'physical_goods', 'manual',
        'internal_fulfillment', '{"skuCode":"MF-LIGHT-001"}', 1, '需要用户填写收货信息', 'active', 1, 'system_seed',
        'system_seed'),
       (900901016, 900901016, 'gold_eye_mask', '金眼罩', 'physical_goods', 'manual',
        'internal_fulfillment', '{"skuCode":"MF-EYE-001"}', 1,
        '需要用户填写收货信息', 'active', 1, 'system_seed', 'system_seed'),
       (900901017, 900901017, 'pico_neo3', 'Pico Neo3 VR 一体机', 'physical_goods', 'manual',
        'internal_fulfillment', '{"skuCode":"PICO-NEO3"}', 1,
        '需要用户填写收货信息', 'active', 1, 'system_seed', 'system_seed'),
       (900901018, 900901018, 'xbox_controller', 'Xbox 无线手柄', 'physical_goods', 'manual',
        'internal_fulfillment', '{"skuCode":"XBOX-CONTROLLER"}', 1, '需要用户填写收货信息', 'active', 1, 'system_seed',
        'system_seed'),
       (900901019, 900901019, 'apple_airpods', 'Apple AirPods4', 'physical_goods', 'manual',
        'internal_fulfillment', '{"skuCode":"APPLE-AIRPODS"}', 1, '需要用户填写收货信息', 'active', 1, 'system_seed',
        'system_seed');

INSERT INTO strategy
(id, strategy_id, strategy_desc, rule_models, state, version, created_by, updated_by)
VALUES (900902001, 900901001, '九宫格抽奖：黑名单、权重、次数锁、库存和幸运奖综合验证策略',
        'rule_blacklist,rule_weight', 'active', 1, 'system_seed', 'system_seed');

INSERT INTO strategy_award
(id, strategy_id, award_id, award_title, award_subtitle, award_count,
 award_count_surplus, award_rate, rule_models, sort)
VALUES (900902011, 900901001, 900901011, '10 积分', '自动入账积分账户', 100000, 100000, 0.30000, 'rule_stock', 1),
       (900902012, 900901001, 900901012, '课程 5 折兑换券', '指定课程可用', 20000, 20000, 0.20000, 'rule_stock', 2),
       (900902013, 900901001, 900901013, '曼秀雷敦唇膏', '实物奖品', 5000, 5000, 0.15000, 'rule_stock', 3),
       (900902014, 900901001, 900901014, '4201 随机盲盒', '无门槛次数锁', 3000, 3000, 0.12000,
        'rule_lock/rule_stock', 4),
       (900902015, 900901001, 900901015, '睡眠日小夜灯', '累计抽奖 1 次解锁', 2000, 2000, 0.10000,
        'rule_lock/rule_stock', 5),
       (900902016, 900901001, 900901016, '金眼罩', '累计抽奖 2 次解锁', 1000, 1000, 0.08000,
        'rule_lock/rule_stock', 6),
       (900902017, 900901001, 900901017, 'Pico Neo3 VR 一体机', '累计抽奖 3 次解锁', 10, 10, 0.01000,
        'rule_lock/rule_stock', 7),
       (900902018, 900901001, 900901018, 'Xbox 无线手柄', '累计抽奖 5 次解锁', 30, 30, 0.01500,
        'rule_lock/rule_stock', 8),
       (900902019, 900901001, 900901019, 'Apple AirPods', '累计抽奖 10 次解锁', 50, 50, 0.02500,
        'rule_lock/rule_stock', 9);

INSERT INTO strategy_rule
(id, strategy_id, award_id, rule_type, rule_model, rule_value, rule_desc)
VALUES (900903001, 900901001, 900901014, 2, 'rule_lock', '0', '无门槛次数锁'),
       (900903002, 900901001, 900901015, 2, 'rule_lock', '1', '累计抽奖 1 次后解锁睡眠日小夜灯'),
       (900903003, 900901001, 900901016, 2, 'rule_lock', '2', '累计抽奖 2 次后解锁金眼罩'),
       (900903004, 900901001, 900901017, 2, 'rule_lock', '3', '累计抽奖 3 次后解锁 Pico Neo3'),
       (900903005, 900901001, 900901018, 2, 'rule_lock', '5', '累计抽奖 5 次后解锁 Xbox 无线手柄'),
       (900903006, 900901001, 900901019, 2, 'rule_lock', '10', '累计抽奖 10 次后解锁 Apple AirPods'),
       (900903007, 900901001, NULL, 1, 'rule_blacklist',
        '900901011:blocked-user-001/blocked-user-002', '命中黑名单时直接返回 10 积分'),
       (900903008, 900901001, NULL, 1, 'rule_weight',
        '40000:900901013/900901014/900901015/900901016;60000:900901017/900901018/900901019;',
        '用户权重奖池规则');

INSERT INTO rule_tree (id, tree_id, tree_desc, tree_node_rule_key)
VALUES (900904001, '900904001', '高价值奖品树：次数锁 -> 库存 -> 默认/幸运奖', 'rule_lock'),
       (900904002, '900904002', '普通奖品树：库存 -> 默认/幸运奖', 'rule_stock');

INSERT INTO rule_tree_node (id, tree_id, rule_key, rule_desc, rule_value)
VALUES (900904011, '900904001', 'rule_lock', '校验奖品累计抽奖次数锁', 'strategy_rule'),
       (900904012, '900904001', 'rule_stock', '校验奖品剩余库存', 'award_count_surplus'),
       (900904013, '900904001', 'rule_luck_award', '发放未解锁或库存不足时的幸运奖', '900901011:1/100'),
       (900904014, '900904001', 'default', '保留原奖品', ''),
       (900904015, '900904002', 'rule_stock', '校验普通奖品剩余库存', 'award_count_surplus'),
       (900904016, '900904002', 'rule_luck_award', '普通奖品库存不足时发放幸运奖', '900901011:1/100'),
       (900904017, '900904002', 'default', '保留普通奖品', '');

INSERT INTO rule_tree_node_line
(id, tree_id, rule_node_from, rule_node_to, rule_limit_type, rule_limit_value)
VALUES (900904021, '900904001', 'rule_lock', 'rule_stock', '1', '0000'),
       (900904022, '900904001', 'rule_lock', 'rule_luck_award', '1', '0001'),
       (900904023, '900904001', 'rule_stock', 'rule_luck_award', '1', '0001'),
       (900904024, '900904001', 'rule_stock', 'default', '1', '0000'),
       (900904025, '900904002', 'rule_stock', 'rule_luck_award', '1', '0001'),
       (900904026, '900904002', 'rule_stock', 'default', '1', '0000');

INSERT INTO raffle_activity_count
(id, activity_count_id, activity_count_name, total_count, day_count, month_count, version, created_by, updated_by)
VALUES (100301, 100301, '九宫格活动基础次数包', 100, 10, 100, 1, 'system_seed', 'system_seed');

INSERT INTO raffle_activity
(id, activity_id, activity_name, activity_desc, begin_date_time, end_date_time,
 stock_count, stock_count_surplus, activity_count_id, strategy_id, award_unlock_scope,
 state, version, created_by, updated_by)
VALUES (100301, 100301, '九宫格活动抽奖', '九宫格抽奖活动演示数据',
        '2026-01-01 00:00:00', '2036-12-31 23:59:59',
        100000, 100000, 100301, 900901001, 'total',
        'open', 1, 'system_seed', 'system_seed');

INSERT INTO raffle_activity_stage
(id, stage_id, channel, source, activity_id, state, version, created_by, updated_by)
VALUES (900905001, 900905001, 'web', 'official', 100301, 'create', 1, 'system_seed', 'system_seed');

INSERT INTO raffle_activity_sku
(sku, activity_id, activity_count_id, stock_count, stock_count_surplus, credit_amount,
 version, created_by, updated_by)
VALUES (9011, 100301, 100301, 100000, 100000, 100,
        1, 'system_seed', 'system_seed');

INSERT INTO behavior_rebate_config
    (behavior_type, rebate_type, rebate_config, rebate_desc)
VALUES ('sign', 'sku', '9011', '签到返利：发放活动 SKU 9011'),
       ('sign', 'integral', '10', '签到返利：发放 10 积分');

INSERT INTO raffle_activity_account
(user_id, activity_id, total_count, total_count_surplus, day_count, day_count_surplus,
 month_count, month_count_surplus, version)
VALUES ('user-001', 100301, 100, 100, 10, 10, 100, 100, 1),
       ('demo-user-001', 100301, 100, 100, 10, 10, 100, 100, 1),
       ('blocked-user-001', 100301, 100, 100, 10, 10, 100, 100, 1),
       ('blocked-user-002', 100301, 100, 100, 10, 10, 100, 100, 1),
       ('low-credit-user-001', 100301, 100, 100, 10, 10, 100, 100, 1);

INSERT INTO user_credit_account (user_id, total_amount, available_amount)
VALUES ('user-001', 10000, 10000),
       ('demo-user-001', 10000, 10000),
       ('blocked-user-001', 10000, 10000),
       ('blocked-user-002', 10000, 10000),
       ('low-credit-user-001', 50, 50);

USE `market-forge-002`;

INSERT INTO raffle_activity_account
(user_id, activity_id, total_count, total_count_surplus, day_count, day_count_surplus,
 month_count, month_count_surplus, version)
VALUES ('user-001', 100301, 100, 100, 10, 10, 100, 100, 1),
       ('demo-user-001', 100301, 100, 100, 10, 10, 100, 100, 1),
       ('blocked-user-001', 100301, 100, 100, 10, 10, 100, 100, 1),
       ('blocked-user-002', 100301, 100, 100, 10, 10, 100, 100, 1),
       ('low-credit-user-001', 100301, 100, 100, 10, 10, 100, 100, 1);

INSERT INTO user_credit_account (user_id, total_amount, available_amount)
VALUES ('user-001', 10000, 10000),
       ('demo-user-001', 10000, 10000),
       ('blocked-user-001', 10000, 10000),
       ('blocked-user-002', 10000, 10000),
       ('low-credit-user-001', 50, 50);

USE `market-forge-001`;

SELECT strategy_id, strategy_desc, rule_models, state, version, created_by, updated_by
FROM strategy
WHERE strategy_id = 900901001;

SELECT activity_id, activity_name, strategy_id, award_unlock_scope, state
FROM raffle_activity
WHERE activity_id = 100301;

SELECT behavior_type, rebate_type, rebate_config
FROM behavior_rebate_config
ORDER BY behavior_type, rebate_type;
