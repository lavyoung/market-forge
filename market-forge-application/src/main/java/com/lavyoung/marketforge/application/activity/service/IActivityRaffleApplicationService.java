package com.lavyoung.marketforge.application.activity.service;

import com.lavyoung.marketforge.application.activity.model.ActivityAwardListCommand;
import com.lavyoung.marketforge.application.activity.model.ActivityAwardResult;
import com.lavyoung.marketforge.application.activity.model.ActivityRaffleCommand;
import com.lavyoung.marketforge.application.activity.model.ActivityRaffleResult;

import java.util.List;

/**
 * 活动抽奖应用用例入口。
 * <p>
 * 负责跨活动域、策略域和用户中奖域完成一次完整抽奖：先创建并消费活动参与订单，
 * 再执行策略抽奖，最后保存中奖记录和可靠消息任务。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
public interface IActivityRaffleApplicationService {

    /**
     * 装配指定活动的运行时缓存。
     * <p>
     * 该方法是活动维度的一键装配入口，负责编排活动域和策略域：
     * 先预热活动 SKU、活动详情和次数配置，再预热活动绑定策略的概率表、权重规则和奖品库存。
     *
     * @param activityId 抽奖活动标识
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 活动配置不存在、活动未配置 SKU、活动未绑定策略或装配失败时抛出
     */
    void armory(Long activityId);

    /**
     * 执行一次活动抽奖。
     *
     * @param command 活动抽奖命令
     * @return 活动抽奖结果，包含活动订单号和命中奖品信息
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 活动额度不足、策略抽奖失败或中奖记录保存失败时抛出
     */
    ActivityRaffleResult raffle(ActivityRaffleCommand command);

    /**
     * 查询活动奖品列表。
     * <p>
     * 该方法用于活动抽奖页展示奖品，不执行真实抽奖，也不扣减库存。
     * 它会根据活动找到绑定策略，再聚合策略奖品、次数锁规则和用户参与次数，
     * 最终计算每个奖品在当前用户视角下是否已经解锁。
     *
     * @param command 活动奖品列表查询命令
     * @return 活动奖品展示结果列表
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 活动不存在、活动未绑定策略或规则配置异常时抛出
     */
    List<ActivityAwardResult> queryAwardList(ActivityAwardListCommand command);
}
