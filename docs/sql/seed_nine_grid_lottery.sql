ALTER TABLE strategy_award
    MODIFY COLUMN award_rate DECIMAL (10, 5) DEFAULT 0.00000 NOT NULL COMMENT '奖品中奖概率';

INSERT INTO award (id, award_key, award_config, award_desc)
VALUES (900901011, 'random_ore', 'quantity=1', '随机矿石'),
       (900901012, 'course_coupon_50', 'discount=50', '课程 5 折兑换券'),
       (900901013, 'mentholatum_lip_balm', 'sku=MF-LIP-001', '曼秀雷敦唇膏'),
       (900901014, 'blind_box_4201', 'series=4201', '4201 随机盲盒'),
       (900901015, 'sleep_night_light', 'sku=MF-LIGHT-001', '睡眠日小夜灯'),
       (900901016, 'gold_eye_mask', 'sku=MF-EYE-001', '金眼罩'),
       (900901017, 'pico_neo3', 'sku=PICO-NEO3', 'Pico Neo3 VR 一体机'),
       (900901018, 'xbox_controller', 'sku=XBOX-CONTROLLER', 'Xbox 无线手柄'),
       (900901019, 'apple_airpods', 'sku=APPLE-AIRPODS', 'Apple AirPods') ON DUPLICATE KEY
UPDATE award_key =
VALUES (award_key), award_config =
VALUES (award_config), award_desc =
VALUES (award_desc);

INSERT INTO strategy (id, strategy_id, strategy_desc, rule_models)
VALUES (900902001, 900901001, '九宫格抽奖开发验证策略', 'rule_lock') ON DUPLICATE KEY
UPDATE strategy_id =
VALUES (strategy_id), strategy_desc =
VALUES (strategy_desc), rule_models =
VALUES (rule_models);

INSERT INTO strategy_award
(id, strategy_id, award_id, award_title, award_subtitle, award_count,
 award_count_surplus, award_rate, rule_models, sort)
VALUES (900902011, 900901001, 900901011, '随机矿石', '随机数量矿石', 100000, 100000, 0.30000, NULL, 1),
       (900902012, 900901001, 900901012, '课程 5 折兑换券', '指定课程可用', 20000, 20000, 0.20000, NULL, 2),
       (900902013, 900901001, 900901013, '曼秀雷敦唇膏', '实物奖品', 5000, 5000, 0.15000, NULL, 3),
       (900902014, 900901001, 900901014, '4201 随机盲盒', '随机款式', 3000, 3000, 0.12000, NULL, 4),
       (900902015, 900901001, 900901015, '睡眠日小夜灯', '实物奖品', 2000, 2000, 0.10000, NULL, 5),
       (900902016, 900901001, 900901016, '金眼罩', '实物奖品', 1000, 1000, 0.08000, NULL, 6),
       (900902017, 900901001, 900901017, 'Pico Neo3 VR 一体机', '再抽 1 次解锁', 10, 10, 0.01000, 'rule_lock', 7),
       (900902018, 900901001, 900901018, 'Xbox 无线手柄', '再抽 2 次解锁', 30, 30, 0.01500, 'rule_lock', 8),
       (900902019, 900901001, 900901019, 'Apple AirPods', '再抽 3 次解锁', 50, 50, 0.02500, 'rule_lock',
        9) ON DUPLICATE KEY
UPDATE strategy_id =
VALUES (strategy_id), award_id =
VALUES (award_id), award_title =
VALUES (award_title), award_subtitle =
VALUES (award_subtitle), award_count =
VALUES (award_count), award_count_surplus =
VALUES (award_count_surplus), award_rate =
VALUES (award_rate), rule_models =
VALUES (rule_models), sort =
VALUES (sort);

INSERT INTO strategy_rule
(id, strategy_id, award_id, rule_type, rule_model, rule_value, rule_desc)
VALUES (900903001, 900901001, 900901017, 2, 'rule_lock', '1', '累计抽奖 1 次后解锁 Pico Neo3'),
       (900903002, 900901001, 900901018, 2, 'rule_lock', '2', '累计抽奖 2 次后解锁 Xbox 无线手柄'),
       (900903003, 900901001, 900901019, 2, 'rule_lock', '3', '累计抽奖 3 次后解锁 Apple AirPods') ON DUPLICATE KEY
UPDATE strategy_id =
VALUES (strategy_id), award_id =
VALUES (award_id), rule_type =
VALUES (rule_type), rule_model =
VALUES (rule_model), rule_value =
VALUES (rule_value), rule_desc =
VALUES (rule_desc);
