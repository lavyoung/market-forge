use `market-forge`;

ALTER TABLE strategy_award
    MODIFY COLUMN award_rate DECIMAL(10, 5) DEFAULT 0.00000 NOT NULL COMMENT '奖品中奖概率';

INSERT INTO award (id, award_key, award_config, award_desc)
VALUES (900901011, 'random_ore', 'quantity=1', '随机矿石'),
       (900901012, 'course_coupon_50', 'discount=50', '课程 5 折兑换券'),
       (900901013, 'mentholatum_lip_balm', 'sku=MF-LIP-001', '曼秀雷敦唇膏'),
       (900901014, 'blind_box_4201', 'series=4201', '4201 随机盲盒'),
       (900901015, 'sleep_night_light', 'sku=MF-LIGHT-001', '睡眠日小夜灯'),
       (900901016, 'gold_eye_mask', 'sku=MF-EYE-001', '金眼罩'),
       (900901017, 'pico_neo3', 'sku=PICO-NEO3', 'Pico Neo3 VR 一体机'),
       (900901018, 'xbox_controller', 'sku=XBOX-CONTROLLER', 'Xbox 无线手柄'),
       (900901019, 'apple_airpods', 'sku=APPLE-AIRPODS', 'Apple AirPods4')
ON DUPLICATE KEY
    UPDATE award_key    =
               VALUES(award_key),
           award_config =
               VALUES(award_config),
           award_desc   =
               VALUES(award_desc);

INSERT INTO strategy (id, strategy_id, strategy_desc, rule_models)
VALUES (900902001, 900901001, '九宫格抽奖开发验证策略', 'rule_weight')
ON DUPLICATE KEY
    UPDATE strategy_id   =
               VALUES(strategy_id),
           strategy_desc =
               VALUES(strategy_desc),
           rule_models   =
               VALUES(rule_models);

INSERT INTO strategy_award
(id, strategy_id, award_id, award_title, award_subtitle, award_count,
 award_count_surplus, award_rate, rule_models, sort)
VALUES (900902011, 900901001, 900901011, '随机矿石', '随机数量矿石', 100000, 100000, 0.30000, 'rule_stock', 1),
       (900902012, 900901001, 900901012, '课程 5 折兑换券', '指定课程可用', 20000, 20000, 0.20000, 'rule_stock', 2),
       (900902013, 900901001, 900901013, '曼秀雷敦唇膏', '实物奖品', 5000, 5000, 0.15000, 'rule_stock', 3),
       (900902014, 900901001, 900901014, '4201 随机盲盒', '随机款式', 3000, 3000, 0.12000, 'rule_stock', 4),
       (900902015, 900901001, 900901015, '睡眠日小夜灯', '实物奖品', 2000, 2000, 0.10000, 'rule_stock', 5),
       (900902016, 900901001, 900901016, '金眼罩', '实物奖品', 1000, 1000, 0.08000, 'rule_stock', 6),
       (900902017, 900901001, 900901017, 'Pico Neo3 VR 一体机', '再抽 1 次解锁', 10, 10, 0.01000,
        'rule_lock,rule_stock', 7),
       (900902018, 900901001, 900901018, 'Xbox 无线手柄', '再抽 2 次解锁', 30, 30, 0.01500, 'rule_lock,rule_stock', 8),
       (900902019, 900901001, 900901019, 'Apple AirPods', '再抽 3 次解锁', 50, 50, 0.02500, 'rule_lock,rule_stock',
        9)
ON DUPLICATE KEY
    UPDATE strategy_id         =
               VALUES(strategy_id),
           award_id            =
               VALUES(award_id),
           award_title         =
               VALUES(award_title),
           award_subtitle      =
               VALUES(award_subtitle),
           award_count         =
               VALUES(award_count),
           award_count_surplus =
               VALUES(award_count_surplus),
           award_rate          =
               VALUES(award_rate),
           rule_models         =
               VALUES(rule_models),
           sort                =
               VALUES(sort);

INSERT INTO strategy_rule
(id, strategy_id, award_id, rule_type, rule_model, rule_value, rule_desc)
VALUES (900903001, 900901001, 900901017, 2, 'rule_lock', '1', '累计抽奖 1 次后解锁 Pico Neo3'),
       (900903002, 900901001, 900901018, 2, 'rule_lock', '2', '累计抽奖 2 次后解锁 Xbox 无线手柄'),
       (900903003, 900901001, 900901019, 2, 'rule_lock', '3', '累计抽奖 3 次后解锁 Apple AirPods'),
       (900903004, 900901001, NULL, 1, 'rule_weight',
        '40000:900901013/900901014/900901015/900901016;60000:900901017/900901018/900901019;',
        '累计消耗积分达到指定档位后，从对应奖品范围内必中一个奖品'),
       (900903005, 900901001, NULL, 1, 'rule_luck_award', '900901011:1/100',
        '次数锁未解锁或库存不足时，兜底发放 1 至 100 个随机矿石')
ON DUPLICATE KEY
    UPDATE strategy_id =
               VALUES(strategy_id),
           award_id    =
               VALUES(award_id),
           rule_type   =
               VALUES(rule_type),
           rule_model  =
               VALUES(rule_model),
           rule_value  =
               VALUES(rule_value),
           rule_desc   =
               VALUES(rule_desc);

-- 奖品执行规则树：高价值奖品先校验次数锁，解锁后校验库存；任一校验被接管时发放幸运奖。
INSERT INTO rule_tree (id, tree_id, tree_desc, tree_node_rule_key)
VALUES (900904001, '900904001', '九宫格奖品次数锁、库存与幸运奖兜底规则树', 'rule_lock'),
       (900904002, '900904002', '九宫格普通奖品库存与幸运奖兜底规则树', 'rule_stock')
ON DUPLICATE KEY
    UPDATE tree_id            =
               VALUES(tree_id),
           tree_desc          =
               VALUES(tree_desc),
           tree_node_rule_key =
               VALUES(tree_node_rule_key);

INSERT INTO rule_tree_node (id, tree_id, rule_key, rule_desc, rule_value)
VALUES (900904011, '900904001', 'rule_lock', '校验奖品累计抽奖次数锁', 'strategy_rule'),
       (900904012, '900904001', 'rule_stock', '校验奖品剩余库存', 'award_count_surplus'),
       (900904013, '900904001', 'rule_luck_award', '发放未解锁或库存不足时的幸运奖', '900901011:1/100'),
       (900904014, '900904001', 'default', '保留库存扣减成功的原奖品', ''),
       (900904015, '900904002', 'rule_stock', '校验普通奖品剩余库存', 'award_count_surplus'),
       (900904016, '900904002', 'rule_luck_award', '普通奖品库存不足时发放幸运奖', '900901011:1/100'),
       (900904017, '900904002', 'default', '保留库存扣减成功的普通奖品', '')
ON DUPLICATE KEY
    UPDATE tree_id    =
               VALUES(tree_id),
           rule_key   =
               VALUES(rule_key),
           rule_desc  =
               VALUES(rule_desc),
           rule_value =
               VALUES(rule_value);

INSERT INTO rule_tree_node_line
(id, tree_id, rule_node_from, rule_node_to, rule_limit_type, rule_limit_value)
VALUES (900904021, '900904001', 'rule_lock', 'rule_stock', '1', '0000'),
       (900904022, '900904001', 'rule_lock', 'rule_luck_award', '1', '0001'),
       (900904023, '900904001', 'rule_stock', 'rule_luck_award', '1', '0001'),
       (900904024, '900904001', 'rule_stock', 'default', '1', '0000'),
       (900904025, '900904002', 'rule_stock', 'rule_luck_award', '1', '0001'),
       (900904026, '900904002', 'rule_stock', 'default', '1', '0000')
ON DUPLICATE KEY
    UPDATE tree_id          =
               VALUES(tree_id),
           rule_node_from   =
               VALUES(rule_node_from),
           rule_node_to     =
               VALUES(rule_node_to),
           rule_limit_type  =
               VALUES(rule_limit_type),
           rule_limit_value =
               VALUES(rule_limit_value);
