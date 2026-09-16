-- 九宫格抽奖开发种子数据。
--
-- 规则执行顺序：
-- 1. 策略级责任链：rule_blacklist -> rule_weight -> default；
-- 2. 黑名单或权重规则命中后直接返回奖品，不受次数锁和库存规则影响；
-- 3. 只有 default 随机抽中的奖品，才根据 strategy_award.rule_models 选择规则树；
-- 4. 普通奖品执行 rule_stock -> default/rule_luck_award；
-- 5. 有次数锁的奖品执行 rule_lock -> rule_stock -> default/rule_luck_award。
--
-- 当前 RuleLockLogicTreeNode 使用开发基准抽奖次数 2：
-- 0/1/2 次门槛会放行，3/5/10 次门槛会转入幸运奖，可用于验证两类分支。
-- rule_tree 由根规则 key 查询，因此同一个根规则只保留一棵树，避免 LIMIT 1 命中不确定。

-- BUG: 当前 UPSERT 会将 award_count_surplus 重置为种子值。
-- 仅限开发环境初始化使用，后续应将“初始化配置”和“重置库存”拆成两份脚本。

-- 普通配置表由 LavShard 固定路由到默认数据源 ds1，即 market-forge-001。
USE `market-forge-001`;

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
VALUES (900902001, 900901001, '九宫格抽奖：黑名单、权重、次数锁、库存和幸运奖综合验证策略',
        'rule_blacklist,rule_weight')
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
       (900902014, 900901001, 900901014, '4201 随机盲盒', '无门槛次数锁，用于验证锁节点放行', 3000, 3000, 0.12000,
        'rule_lock,rule_stock', 4),
       (900902015, 900901001, 900901015, '睡眠日小夜灯', '累计抽奖 1 次解锁', 2000, 2000, 0.10000,
        'rule_lock,rule_stock', 5),
       (900902016, 900901001, 900901016, '金眼罩', '累计抽奖 2 次解锁', 1000, 1000, 0.08000,
        'rule_lock,rule_stock', 6),
       (900902017, 900901001, 900901017, 'Pico Neo3 VR 一体机', '累计抽奖 3 次解锁', 10, 10, 0.01000,
        'rule_lock,rule_stock', 7),
       (900902018, 900901001, 900901018, 'Xbox 无线手柄', '累计抽奖 5 次解锁', 30, 30, 0.01500,
        'rule_lock,rule_stock', 8),
       (900902019, 900901001, 900901019, 'Apple AirPods', '累计抽奖 10 次解锁', 50, 50, 0.02500,
        'rule_lock,rule_stock',
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
VALUES (900903001, 900901001, 900901014, 2, 'rule_lock', '0', '无门槛次数锁，用于验证 rule_lock 放行分支'),
       (900903002, 900901001, 900901015, 2, 'rule_lock', '1', '累计抽奖 1 次后解锁睡眠日小夜灯'),
       (900903003, 900901001, 900901016, 2, 'rule_lock', '2', '累计抽奖 2 次后解锁金眼罩'),
       (900903004, 900901001, 900901017, 2, 'rule_lock', '3', '累计抽奖 3 次后解锁 Pico Neo3'),
       (900903005, 900901001, 900901018, 2, 'rule_lock', '5', '累计抽奖 5 次后解锁 Xbox 无线手柄'),
       (900903006, 900901001, 900901019, 2, 'rule_lock', '10', '累计抽奖 10 次后解锁 Apple AirPods'),
       (900903007, 900901001, NULL, 1, 'rule_blacklist',
        '900901011:blocked-user-001/blocked-user-002',
        '命中黑名单时直接返回低价值随机矿石；开发用户 user-001 不在黑名单中'),
       (900903008, 900901001, NULL, 1, 'rule_weight',
        '40000:900901013/900901014/900901015/900901016;60000:900901017/900901018/900901019;',
        '用户分值达到 40000 或 60000 时，从对应权重奖品池抽奖；当前开发分值 4500 会继续走默认抽奖'),
       (900903009, 900901001, NULL, 1, 'rule_luck_award', '900901011:1/100',
        '幸运奖镜像配置；当前规则树实际执行值以 rule_tree_node.rule_value 为准')
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
VALUES (900904001, '900904001',
        '高价值奖品树：次数锁放行后扣减库存；次数不足或库存不足时转幸运奖', 'rule_lock'),
       (900904002, '900904002',
        '普通奖品树：库存充足时保留原奖品，库存不足时转幸运奖', 'rule_stock')
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

-- ----------------------------------------------------------------------------
-- 数据校验：执行完脚本后应得到 1 个策略、9 个奖品、9 条策略规则、2 棵规则树。
-- ----------------------------------------------------------------------------
SELECT s.strategy_id,
       s.strategy_desc,
       s.rule_models,
       COUNT(sa.id)                               AS award_count,
       CAST(SUM(sa.award_rate) AS DECIMAL(10, 5)) AS total_award_rate
FROM strategy s
         JOIN strategy_award sa ON sa.strategy_id = s.strategy_id
WHERE s.strategy_id = 900901001
GROUP BY s.strategy_id, s.strategy_desc, s.rule_models;

SELECT sr.rule_type,
       sr.rule_model,
       COUNT(*) AS rule_count
FROM strategy_rule sr
WHERE sr.strategy_id = 900901001
GROUP BY sr.rule_type, sr.rule_model
ORDER BY sr.rule_type, sr.rule_model;

SELECT rt.tree_id,
       rt.tree_desc,
       rt.tree_node_rule_key   AS root_rule,
       COUNT(DISTINCT rtn.id)  AS node_count,
       COUNT(DISTINCT rtnl.id) AS line_count
FROM rule_tree rt
         LEFT JOIN rule_tree_node rtn ON rtn.tree_id = rt.tree_id
         LEFT JOIN rule_tree_node_line rtnl ON rtnl.tree_id = rt.tree_id
WHERE rt.tree_id IN ('900904001', '900904002')
GROUP BY rt.tree_id, rt.tree_desc, rt.tree_node_rule_key
ORDER BY rt.tree_id;
