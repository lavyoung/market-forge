package com.lavyoung.marketforge.types.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * 业务编码
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/02
 */
@Getter
@AllArgsConstructor
public enum BusinessResponseCode implements IResponseCode {

    /*
     * 编码结构：AAA_BBB_CCC
     * AAA：业务域；BBB：业务场景；CCC：具体状态。
     * 公共域编码在展示为九位编码时需要在左侧补零，源码中不使用前导零以避免被 Java 解析为八进制。
     */

    // 100 - 策略域
    STRATEGY_NOT_FOUND(100_001_001, "strategy.not-found", "抽奖策略不存在"),
    STRATEGY_AWARD_NOT_CONFIGURED(100_001_002, "strategy.award.not-configured", "抽奖策略未配置奖品"),
    STRATEGY_RATE_INVALID(100_001_003, "strategy.rate.invalid", "抽奖策略概率配置无效"),
    STRATEGY_RATE_TOTAL_INVALID(100_001_004, "strategy.rate.total-invalid", "抽奖策略概率总和无效"),
    STRATEGY_NOT_ASSEMBLED(100_002_001, "strategy.not-assembled", "抽奖策略尚未装配"),
    STRATEGY_ASSEMBLY_FAILED(100_002_002, "strategy.assembly.failed", "抽奖策略装配失败"),
    STRATEGY_RULE_NOT_FOUND(100_003_001, "strategy.rule.not-found", "抽奖策略规则不存在"),
    STRATEGY_RULE_VALUE_INVALID(100_003_002, "strategy.rule.value-invalid", "抽奖策略规则值格式无效"),
    STRATEGY_WEIGHT_NOT_MATCHED(100_003_003, "strategy.weight.not-matched", "未匹配到对应的积分权重范围"),
    STRATEGY_RULE_TREE_NOT_FOUND(100_004_001, "strategy.rule-tree.not-found", "抽奖规则树不存在"),
    STRATEGY_RULE_TREE_NODE_NOT_FOUND(100_004_002, "strategy.rule-tree.node.not-found", "抽奖规则树节点不存在"),
    STRATEGY_RULE_TREE_LINE_NOT_FOUND(100_004_003, "strategy.rule-tree.line.not-found", "抽奖规则树连线不存在"),
    STRATEGY_RULE_TREE_CONFIG_INVALID(100_004_004, "strategy.rule-tree.config.invalid", "抽奖规则树配置无效"),
    STRATEGY_RULE_EXECUTE_FAILED(100_004_005, "strategy.rule.execute-failed", "抽奖规则执行失败"),
    STRATEGY_BLACKLIST_HIT(100_005_001, "strategy.blacklist.hit", "用户命中抽奖黑名单"),
    STRATEGY_LOCK_NOT_SATISFIED(100_005_002, "strategy.lock.not-satisfied", "用户未满足奖品解锁条件"),

    // 200 - 奖品域
    AWARD_NOT_FOUND(200_001_001, "award.not-found", "奖品不存在"),
    AWARD_NOT_AVAILABLE(200_001_002, "award.not-available", "奖品当前不可用"),
    AWARD_CONFIG_INVALID(200_001_003, "award.config.invalid", "奖品配置无效"),
    AWARD_TYPE_NOT_SUPPORTED(200_001_004, "award.type.not-supported", "奖品类型暂不支持"),
    AWARD_STATE_ERROR(200_001_005, "award.state-error", "奖品状态异常"),
    AWARD_OUT_OF_STOCK(200_002_001, "award.out-of-stock", "奖品库存不足"),
    AWARD_STOCK_DEDUCTION_FAILED(200_002_002, "award.stock.deduction-failed", "奖品库存扣减失败"),
    AWARD_STOCK_LOCK_FAILED(200_002_003, "award.stock.lock-failed", "奖品库存锁定失败"),
    AWARD_STOCK_RELEASE_FAILED(200_002_004, "award.stock.release-failed", "奖品库存释放失败"),
    AWARD_STOCK_SYNC_FAILED(200_002_005, "award.stock.sync-failed", "奖品库存同步失败"),
    AWARD_GRANT_FAILED(200_003_001, "award.grant.failed", "奖品通用发放失败"),
    AWARD_GRANT_DUPLICATED(200_003_002, "award.grant.duplicated", "奖品已发放，请勿重复领取"),
    AWARD_GRANT_NOT_SUPPORTED(200_003_003, "award.grant.not-supported", "奖品发放方式暂不支持"),
    AWARD_GRANT_CHANNEL_ERROR(200_003_004, "award.grant.channel-error", "奖品发放渠道异常"),
    AWARD_GRANT_RESULT_INCONSISTENT(200_003_005, "award.grant.result.inconsistent", "奖品发放结果不一致"),

    // 300 - 抽奖域
    LOTTERY_ACTIVITY_NOT_FOUND(300_001_001, "lottery.activity.not-found", "抽奖活动不存在"),
    LOTTERY_ACTIVITY_NOT_STARTED(300_001_002, "lottery.activity.not-started", "抽奖活动尚未开始"),
    LOTTERY_ACTIVITY_ENDED(300_001_003, "lottery.activity.ended", "抽奖活动已结束"),
    LOTTERY_ACTIVITY_CLOSED(300_001_004, "lottery.activity.closed", "抽奖活动已关闭"),
    LOTTERY_CHANCE_NOT_ENOUGH(300_002_001, "lottery.chance.not-enough", "抽奖次数不足"),
    LOTTERY_DRAW_DUPLICATED(300_002_002, "lottery.draw.duplicated", "抽奖请求正在处理中"),
    LOTTERY_DRAW_FAILED(300_002_003, "lottery.draw.failed", "抽奖失败，请稍后重试"),
    LOTTERY_DRAW_RESULT_EMPTY(300_002_004, "lottery.draw.result.empty", "抽奖结果为空"),
    LOTTERY_DRAW_RESULT_SAVE_FAILED(300_002_005, "lottery.draw.result.save-failed", "抽奖结果保存失败"),
    LOTTERY_DRAW_ORDER_NOT_FOUND(300_002_006, "lottery.draw.order.not-found", "抽奖订单不存在"),
    LOTTERY_DRAW_ORDER_STATE_ERROR(300_002_007, "lottery.draw.order.state-error", "抽奖订单状态异常"),
    LOTTERY_DRAW_NO_AWARD(300_002_008, "lottery.draw.no-award", "本次抽奖未中奖"),
    LOTTERY_AWARD_LOCKED(300_003_001, "lottery.award.locked", "奖品尚未解锁"),
    LOTTERY_AWARD_NOT_MATCHED(300_003_002, "lottery.award.not-matched", "抽奖奖品不匹配"),

    // 400 - 积分账户域
    POINTS_ACCOUNT_NOT_FOUND(400_001_001, "points.account.not-found", "积分账户不存在"),
    POINTS_ACCOUNT_FROZEN(400_001_002, "points.account.frozen", "积分账户已冻结"),
    POINTS_ACCOUNT_STATE_ERROR(400_001_003, "points.account.state-error", "积分账户状态异常"),
    POINTS_BALANCE_NOT_ENOUGH(400_002_001, "points.balance.not-enough", "积分余额不足"),
    POINTS_DEDUCTION_FAILED(400_002_002, "points.deduction.failed", "积分扣减失败"),
    POINTS_GRANT_FAILED(400_002_003, "points.grant.failed", "积分发放失败"),
    POINTS_FREEZE_FAILED(400_002_004, "points.freeze.failed", "积分冻结失败"),
    POINTS_UNFREEZE_FAILED(400_002_005, "points.unfreeze.failed", "积分解冻失败"),
    POINTS_REFUND_FAILED(400_002_006, "points.refund.failed", "积分退回失败"),
    POINTS_TRANSACTION_DUPLICATED(400_003_001, "points.transaction.duplicated", "积分流水已处理"),
    POINTS_TRANSACTION_NOT_FOUND(400_003_002, "points.transaction.not-found", "积分流水不存在"),
    POINTS_TRANSACTION_STATE_ERROR(400_003_003, "points.transaction.state-error", "积分流水状态异常"),
    POINTS_TRANSACTION_AMOUNT_INVALID(400_003_004, "points.transaction.amount.invalid", "积分流水金额无效"),

    // 500 - 活动域
    ACTIVITY_ACCOUNT_NOT_FOUND(500_001_001, "activity.account.not-found", "活动账户不存在"),
    ACTIVITY_ACCOUNT_PROCESS_FAILED(500_001_002, "activity.account.process-failed", "活动账户更新失败"),
    ACTIVITY_ACCOUNT_QUOTA_NOT_ENOUGH(500_001_003, "activity.account.quota.not-enough", "活动账户额度不足"),
    ACTIVITY_ACCOUNT_QUOTA_DAY_NOT_ENOUGH(500_001_004, "activity.account.quota.day.not-enough", "活动账户日额度不足"),
    ACTIVITY_ACCOUNT_QUOTA_MONTH_NOT_ENOUGH(500_001_005, "activity.account.quota.month.not-enough", "活动账户月额度不足"),
    ACTIVITY_ACCOUNT_DAY_NOT_FOUND(500_001_006, "activity.account.day.not-found", "活动日账户不存在"),
    ACTIVITY_ACCOUNT_MONTH_NOT_FOUND(500_001_007, "activity.account.month.not-found", "活动月账户不存在"),
    ACTIVITY_ACCOUNT_CREATE_FAILED(500_001_008, "activity.account.create-failed", "活动账户创建失败"),
    ACTIVITY_ACCOUNT_DAY_CREATE_FAILED(500_001_009, "activity.account.day.create-failed", "活动日账户创建失败"),
    ACTIVITY_ACCOUNT_MONTH_CREATE_FAILED(500_001_010, "activity.account.month.create-failed", "活动月账户创建失败"),
    ACTIVITY_ACCOUNT_QUOTA_DEDUCT_FAILED(500_001_011, "activity.account.quota.deduct-failed", "活动账户额度扣减失败"),
    ACTIVITY_ACCOUNT_QUOTA_DAY_DEDUCT_FAILED(500_001_012, "activity.account.quota.day.deduct-failed", "活动账户日额度扣减失败"),
    ACTIVITY_ACCOUNT_QUOTA_MONTH_DEDUCT_FAILED(500_001_013, "activity.account.quota.month.deduct-failed", "活动账户月额度扣减失败"),
    ACTIVITY_ACCOUNT_QUOTA_RECHARGE_FAILED(500_001_014, "activity.account.quota.recharge-failed", "活动账户额度充值失败"),
    ACTIVITY_ACCOUNT_QUOTA_FREEZE_FAILED(500_001_015, "activity.account.quota.freeze-failed", "活动账户额度冻结失败"),
    ACTIVITY_ACCOUNT_QUOTA_UNFREEZE_FAILED(500_001_016, "activity.account.quota.unfreeze-failed", "活动账户额度解冻失败"),
    ACTIVITY_ACCOUNT_QUOTA_REFUND_FAILED(500_001_017, "activity.account.quota.refund-failed", "活动账户额度退回失败"),
    ACTIVITY_ACCOUNT_QUOTA_FLOW_DUPLICATED(500_001_018, "activity.account.quota.flow.duplicated", "活动账户额度流水已处理"),
    ACTIVITY_ACCOUNT_QUOTA_FLOW_NOT_FOUND(500_001_019, "activity.account.quota.flow.not-found", "活动账户额度流水不存在"),

    ACTIVITY_STATE_ERROR(500_002_001, "activity.state.error", "活动未开放"),
    ACTIVITY_TIME_RANGE_ERROR(500_002_002, "activity.time-range.error", "活动不在有效时间"),
    ACTIVITY_NOT_FOUND(500_002_003, "activity.not-found", "活动不存在"),
    ACTIVITY_NOT_STARTED(500_002_004, "activity.not-started", "活动尚未开始"),
    ACTIVITY_ENDED(500_002_005, "activity.ended", "活动已结束"),
    ACTIVITY_CLOSED(500_002_006, "activity.closed", "活动已关闭"),
    ACTIVITY_COUNT_NOT_FOUND(500_002_007, "activity.count.not-found", "活动次数配置不存在"),
    ACTIVITY_COUNT_INVALID(500_002_008, "activity.count.invalid", "活动次数配置无效"),
    ACTIVITY_ASSEMBLY_FAILED(500_002_009, "activity.assembly.failed", "活动装配失败"),
    ACTIVITY_CONFIG_INVALID(500_002_010, "activity.config.invalid", "活动配置无效"),
    ACTIVITY_SKU_NOT_CONFIGURED(500_002_011, "activity.sku.not-configured", "活动未配置 SKU"),
    ACTIVITY_STRATEGY_NOT_CONFIGURED(500_002_012, "activity.strategy.not-configured", "活动未配置抽奖策略"),
    ACTIVITY_PUBLISH_FAILED(500_002_013, "activity.publish-failed", "活动发布失败"),
    ACTIVITY_ARCHIVE_FAILED(500_002_014, "activity.archive-failed", "活动归档失败"),

    ACTIVITY_SKU_NOT_FOUND(500_003_001, "activity.sku.not-found", "活动 SKU 不存在"),
    ACTIVITY_SKU_STOCK_NOT_ENOUGH(500_003_002, "activity.sku.stock.not-enough", "活动 SKU 库存不足"),
    ACTIVITY_SKU_STOCK_DEDUCT_FAILED(500_003_003, "activity.sku.stock.deduct-failed", "活动 SKU 库存扣减失败"),
    ACTIVITY_SKU_STOCK_CACHE_NOT_FOUND(500_003_004, "activity.sku.stock.cache.not-found", "活动 SKU 库存缓存不存在"),
    ACTIVITY_SKU_STOCK_CACHE_DEDUCT_FAILED(500_003_005, "activity.sku.stock.cache.deduct-failed", "活动 SKU 库存缓存扣减失败"),
    ACTIVITY_SKU_STOCK_LOCK_FAILED(500_003_006, "activity.sku.stock.lock-failed", "活动 SKU 库存锁定失败"),
    ACTIVITY_SKU_STOCK_SYNC_FAILED(500_003_007, "activity.sku.stock.sync-failed", "活动 SKU 库存同步失败"),
    ACTIVITY_SKU_STOCK_CLEAR_FAILED(500_003_008, "activity.sku.stock.clear-failed", "活动 SKU 库存清零失败"),
    ACTIVITY_SKU_STOCK_QUEUE_EMPTY(500_003_009, "activity.sku.stock.queue.empty", "活动 SKU 库存同步队列为空"),
    ACTIVITY_SKU_STOCK_MESSAGE_SEND_FAILED(500_003_010, "activity.sku.stock.message.send-failed", "活动 SKU 库存消息发送失败"),

    ACTIVITY_ORDER_NOT_FOUND(500_004_001, "activity.order.not-found", "活动订单不存在"),
    ACTIVITY_ORDER_CREATE_FAILED(500_004_002, "activity.order.create-failed", "活动订单创建失败"),
    ACTIVITY_ORDER_STATE_ERROR(500_004_003, "activity.order.state-error", "活动订单状态异常"),
    ACTIVITY_ORDER_DUPLICATED(500_004_004, "activity.order.duplicated", "活动订单已存在，请勿重复创建"),
    ACTIVITY_ORDER_OUT_BUSINESS_NO_DUPLICATED(500_004_005, "activity.order.out-business-no.duplicated", "活动订单业务幂等号已处理"),
    ACTIVITY_SKU_ORDER_NOT_FOUND(500_004_006, "activity.sku.order.not-found", "活动 SKU 订单不存在"),
    ACTIVITY_SKU_ORDER_CREATE_FAILED(500_004_007, "activity.sku.order.create-failed", "活动 SKU 订单创建失败"),
    ACTIVITY_ORDER_PAY_REQUIRED(500_004_008, "activity.order.pay.required", "活动订单需要支付"),
    ACTIVITY_ORDER_PAY_FAILED(500_004_009, "activity.order.pay.failed", "活动订单支付失败"),
    ACTIVITY_ORDER_PAY_TIMEOUT(500_004_010, "activity.order.pay.timeout", "活动订单支付超时"),
    ACTIVITY_ORDER_CANCEL_FAILED(500_004_011, "activity.order.cancel-failed", "活动订单取消失败"),
    ACTIVITY_ORDER_REFUND_FAILED(500_004_012, "activity.order.refund-failed", "活动订单退款失败"),
    ACTIVITY_ORDER_EXPIRED(500_004_013, "activity.order.expired", "活动订单已过期"),
    ACTIVITY_SKU_ORDER_STATE_ERROR(500_004_014, "activity.sku.order.state-error", "活动 SKU 订单状态异常"),

    ACTIVITY_PARTAKE_ORDER_NOT_FOUND(500_005_001, "activity.partake.order.not-found", "活动参与订单不存在"),
    ACTIVITY_PARTAKE_ORDER_CREATE_FAILED(500_005_002, "activity.partake.order.create-failed", "活动参与订单创建失败"),
    ACTIVITY_PARTAKE_ORDER_USED(500_005_003, "activity.partake.order.used", "活动参与订单已使用"),
    ACTIVITY_PARTAKE_ORDER_CANCELED(500_005_004, "activity.partake.order.canceled", "活动参与订单已作废"),
    ACTIVITY_PARTAKE_ORDER_STATE_ERROR(500_005_005, "activity.partake.order.state-error", "活动参与订单状态异常"),
    ACTIVITY_PARTAKE_ORDER_PENDING(500_005_006, "activity.partake.order.pending", "存在未使用的活动参与订单"),

    // 600 - 用户中奖与发奖域
    USER_AWARD_RECORD_NOT_FOUND(600_001_001, "user-award.record.not-found", "用户中奖记录不存在"),
    USER_AWARD_RECORD_CREATE_FAILED(600_001_002, "user-award.record.create-failed", "用户中奖记录创建失败"),
    USER_AWARD_RECORD_UPDATE_FAILED(600_001_003, "user-award.record.update-failed", "用户中奖记录更新失败"),
    USER_AWARD_RECORD_DUPLICATED(600_001_004, "user-award.record.duplicated", "用户中奖记录已存在"),
    USER_AWARD_RECORD_STATE_ERROR(600_001_005, "user-award.record.state-error", "用户中奖记录状态异常"),
    USER_AWARD_RECORD_ORDER_NOT_MATCHED(600_001_006, "user-award.record.order.not-matched", "中奖记录与抽奖订单不匹配"),
    USER_AWARD_RECORD_ACTIVITY_NOT_MATCHED(600_001_007, "user-award.record.activity.not-matched", "中奖记录与活动不匹配"),
    USER_AWARD_RECORD_AWARD_NOT_MATCHED(600_001_008, "user-award.record.award.not-matched", "中奖记录与奖品不匹配"),

    USER_AWARD_CLAIM_NOT_ALLOWED(600_002_001, "user-award.claim.not-allowed", "当前奖品不允许领取"),
    USER_AWARD_CLAIM_EXPIRED(600_002_002, "user-award.claim.expired", "奖品领取已过期"),
    USER_AWARD_CLAIM_DUPLICATED(600_002_003, "user-award.claim.duplicated", "奖品已领取，请勿重复领取"),
    USER_AWARD_CLAIM_FAILED(600_002_004, "user-award.claim.failed", "奖品领取失败"),
    USER_AWARD_CLAIM_INFO_INVALID(600_002_005, "user-award.claim.info.invalid", "奖品领取信息无效"),
    USER_AWARD_CLAIM_ADDRESS_REQUIRED(600_002_006, "user-award.claim.address.required", "领取实物奖品需要填写收货地址"),
    USER_AWARD_CLAIM_PHONE_REQUIRED(600_002_007, "user-award.claim.phone.required", "领取奖品需要填写手机号"),
    USER_AWARD_CLAIM_REAL_NAME_REQUIRED(600_002_008, "user-award.claim.real-name.required", "领取奖品需要填写真实姓名"),
    USER_AWARD_CLAIM_QUALIFICATION_NOT_MATCHED(600_002_009, "user-award.claim.qualification.not-matched", "不满足奖品领取资格"),

    USER_AWARD_GRANT_PENDING(600_003_001, "user-award.grant.pending", "奖品待发放"),
    USER_AWARD_GRANT_PROCESSING(600_003_002, "user-award.grant.processing", "奖品发放处理中"),
    USER_AWARD_GRANT_COMPLETED(600_003_003, "user-award.grant.completed", "奖品已发放"),
    USER_AWARD_GRANT_FAILED(600_003_004, "user-award.grant.failed", "用户中奖奖品发放失败"),
    USER_AWARD_GRANT_RETRY_EXCEEDED(600_003_005, "user-award.grant.retry-exceeded", "奖品发放重试次数已耗尽"),
    USER_AWARD_GRANT_CHANNEL_NOT_FOUND(600_003_006, "user-award.grant.channel.not-found", "奖品发放渠道不存在"),
    USER_AWARD_GRANT_CHANNEL_UNAVAILABLE(600_003_007, "user-award.grant.channel.unavailable", "奖品发放渠道不可用"),
    USER_AWARD_GRANT_RESULT_UNKNOWN(600_003_008, "user-award.grant.result.unknown", "奖品发放结果未知"),
    USER_AWARD_GRANT_COMPENSATION_FAILED(600_003_009, "user-award.grant.compensation.failed", "奖品发放补偿失败"),

    USER_AWARD_DELIVERY_NOT_REQUIRED(600_004_001, "user-award.delivery.not-required", "当前奖品无需物流配送"),
    USER_AWARD_DELIVERY_INFO_INVALID(600_004_002, "user-award.delivery.info.invalid", "奖品配送信息无效"),
    USER_AWARD_DELIVERY_CREATE_FAILED(600_004_003, "user-award.delivery.create-failed", "奖品配送单创建失败"),
    USER_AWARD_DELIVERY_NOT_FOUND(600_004_004, "user-award.delivery.not-found", "奖品配送单不存在"),
    USER_AWARD_DELIVERY_STATE_ERROR(600_004_005, "user-award.delivery.state-error", "奖品配送状态异常"),
    USER_AWARD_DELIVERY_PROVIDER_UNAVAILABLE(600_004_006, "user-award.delivery.provider.unavailable", "奖品配送服务不可用"),

    USER_AWARD_COUPON_TEMPLATE_NOT_FOUND(600_005_001, "user-award.coupon.template.not-found", "奖券模板不存在"),
    USER_AWARD_COUPON_OUT_OF_STOCK(600_005_002, "user-award.coupon.out-of-stock", "奖券库存不足"),
    USER_AWARD_COUPON_ISSUE_FAILED(600_005_003, "user-award.coupon.issue-failed", "奖券发放失败"),
    USER_AWARD_COUPON_USED(600_005_004, "user-award.coupon.used", "奖券已使用"),
    USER_AWARD_COUPON_EXPIRED(600_005_005, "user-award.coupon.expired", "奖券已过期"),

    USER_AWARD_POINTS_GRANT_FAILED(600_006_001, "user-award.points.grant-failed", "积分奖品发放失败"),
    USER_AWARD_POINTS_ACCOUNT_NOT_FOUND(600_006_002, "user-award.points.account.not-found", "积分奖品发放账户不存在"),
    USER_AWARD_POINTS_AMOUNT_INVALID(600_006_003, "user-award.points.amount.invalid", "积分奖品数量无效"),

    USER_AWARD_MESSAGE_SEND_FAILED(600_007_001, "user-award.message.send-failed", "中奖通知发送失败"),
    USER_AWARD_MESSAGE_TEMPLATE_NOT_FOUND(600_007_002, "user-award.message.template.not-found", "中奖通知模板不存在"),
    USER_AWARD_MESSAGE_CHANNEL_UNAVAILABLE(600_007_003, "user-award.message.channel.unavailable", "中奖通知渠道不可用"),

    USER_AWARD_AUDIT_REQUIRED(600_008_001, "user-award.audit.required", "奖品领取需要审核"),
    USER_AWARD_AUDIT_REJECTED(600_008_002, "user-award.audit.rejected", "奖品领取审核未通过"),
    USER_AWARD_AUDIT_FAILED(600_008_003, "user-award.audit.failed", "奖品领取审核失败"),
    USER_AWARD_REVOKE_NOT_ALLOWED(600_009_001, "user-award.revoke.not-allowed", "当前奖品不允许撤销"),
    USER_AWARD_REVOKE_FAILED(600_009_002, "user-award.revoke.failed", "奖品撤销失败"),
    USER_AWARD_REVOKED(600_009_003, "user-award.revoked", "奖品已撤销"),
    USER_AWARD_AFTERSALE_NOT_ALLOWED(600_010_001, "user-award.aftersale.not-allowed", "当前奖品不支持售后"),
    USER_AWARD_AFTERSALE_CREATE_FAILED(600_010_002, "user-award.aftersale.create-failed", "奖品售后单创建失败"),
    USER_AWARD_AFTERSALE_STATE_ERROR(600_010_003, "user-award.aftersale.state-error", "奖品售后状态异常"),
    USER_AWARD_RETURN_NOT_ALLOWED(600_011_001, "user-award.return.not-allowed", "当前奖品不支持退回"),
    USER_AWARD_RETURN_FAILED(600_011_002, "user-award.return.failed", "奖品退回失败"),
    USER_AWARD_SETTLEMENT_FAILED(600_012_001, "user-award.settlement.failed", "奖品结算失败"),
    USER_AWARD_SETTLEMENT_STATE_ERROR(600_012_002, "user-award.settlement.state-error", "奖品结算状态异常"),

    // 700 - 任务与消息事务域
    TASK_NOT_FOUND(700_001_001, "task.not-found", "任务不存在"),
    TASK_CREATE_FAILED(700_001_002, "task.create-failed", "任务创建失败"),
    TASK_UPDATE_FAILED(700_001_003, "task.update-failed", "任务更新失败"),
    TASK_STATE_ERROR(700_001_004, "task.state-error", "任务状态异常"),
    TASK_DUPLICATED(700_001_005, "task.duplicated", "任务已存在"),
    TASK_RETRY_EXCEEDED(700_001_006, "task.retry-exceeded", "任务重试次数已耗尽"),
    TASK_COMPENSATION_FAILED(700_001_007, "task.compensation.failed", "任务补偿失败"),
    TASK_LOCK_FAILED(700_001_008, "task.lock-failed", "任务锁定失败"),
    TASK_UNLOCK_FAILED(700_001_009, "task.unlock-failed", "任务解锁失败"),
    TASK_TIMEOUT(700_001_010, "task.timeout", "任务处理超时"),

    MESSAGE_NOT_FOUND(700_002_001, "message.not-found", "消息不存在"),
    MESSAGE_CREATE_FAILED(700_002_002, "message.create-failed", "消息创建失败"),
    MESSAGE_SEND_FAILED(700_002_003, "message.send-failed", "消息发送失败"),
    MESSAGE_CONSUME_FAILED(700_002_004, "message.consume-failed", "消息消费失败"),
    MESSAGE_DUPLICATED(700_002_005, "message.duplicated", "消息已处理"),
    MESSAGE_FORMAT_INVALID(700_002_006, "message.format.invalid", "消息格式无效"),
    MESSAGE_TOPIC_NOT_FOUND(700_002_007, "message.topic.not-found", "消息主题不存在"),
    MESSAGE_ROUTE_FAILED(700_002_008, "message.route-failed", "消息路由失败"),
    MESSAGE_DEAD_LETTERED(700_002_009, "message.dead-lettered", "消息已进入死信队列"),
    MESSAGE_REPLAY_FAILED(700_002_010, "message.replay-failed", "消息重放失败"),

    // 800 - 用户与风控域
    USER_NOT_FOUND(800_001_001, "user.not-found", "用户不存在"),
    USER_STATE_ERROR(800_001_002, "user.state-error", "用户状态异常"),
    USER_ACCOUNT_FROZEN(800_001_003, "user.account.frozen", "用户账户已冻结"),
    USER_IDENTITY_NOT_VERIFIED(800_001_004, "user.identity.not-verified", "用户身份未认证"),
    USER_PHONE_NOT_VERIFIED(800_001_005, "user.phone.not-verified", "用户手机号未验证"),

    RISK_CONTROL_REJECTED(800_002_001, "risk-control.rejected", "请求未通过风控校验"),
    RISK_CONTROL_RATE_LIMITED(800_002_002, "risk-control.rate-limited", "请求过于频繁"),
    RISK_CONTROL_DEVICE_LIMITED(800_002_003, "risk-control.device-limited", "设备参与受限"),
    RISK_CONTROL_IP_LIMITED(800_002_004, "risk-control.ip-limited", "IP 参与受限"),
    RISK_CONTROL_USER_LIMITED(800_002_005, "risk-control.user-limited", "用户参与受限"),
    RISK_CONTROL_RULE_NOT_FOUND(800_002_006, "risk-control.rule.not-found", "风控规则不存在"),
    RISK_CONTROL_RULE_INVALID(800_002_007, "risk-control.rule.invalid", "风控规则无效"),

    // 900 - 运营配置与三方集成域
    OPERATION_CONFIG_NOT_FOUND(900_001_001, "operation.config.not-found", "运营配置不存在"),
    OPERATION_CONFIG_INVALID(900_001_002, "operation.config.invalid", "运营配置无效"),
    OPERATION_CONFIG_PUBLISH_FAILED(900_001_003, "operation.config.publish-failed", "运营配置发布失败"),
    OPERATION_CONFIG_ROLLBACK_FAILED(900_001_004, "operation.config.rollback-failed", "运营配置回滚失败"),
    OPERATION_RESOURCE_NOT_FOUND(900_001_005, "operation.resource.not-found", "运营资源不存在"),
    OPERATION_RESOURCE_UNAVAILABLE(900_001_006, "operation.resource.unavailable", "运营资源不可用"),

    THIRD_PARTY_SERVICE_UNAVAILABLE(900_002_001, "third-party.service.unavailable", "第三方服务不可用"),
    THIRD_PARTY_TIMEOUT(900_002_002, "third-party.timeout", "第三方服务调用超时"),
    THIRD_PARTY_RESULT_UNKNOWN(900_002_003, "third-party.result.unknown", "第三方服务结果未知"),
    THIRD_PARTY_SIGNATURE_INVALID(900_002_004, "third-party.signature.invalid", "第三方签名校验失败"),
    THIRD_PARTY_RESPONSE_INVALID(900_002_005, "third-party.response.invalid", "第三方响应格式无效"),
    THIRD_PARTY_IDEMPOTENT_CONFLICT(900_002_006, "third-party.idempotent.conflict", "第三方幂等请求冲突"),

    ;
    /**
     * 业务编码
     */
    private final int code;

    /**
     * 国际化key
     */
    private final String i18nKey;

    /**
     * 默认信息
     */
    private final String msg;

    /**
     * 根据业务编码获取对应的枚举定义。
     *
     * @param code 业务编码
     * @return 匹配的业务响应编码；不存在时返回空
     */
    public static Optional<BusinessResponseCode> fromCode(int code) {
        return Arrays.stream(values())
                .filter(responseCode -> responseCode.code == code)
                .findFirst();
    }
}
