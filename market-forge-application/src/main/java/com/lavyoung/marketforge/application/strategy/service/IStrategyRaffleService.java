package com.lavyoung.marketforge.application.strategy.service;

import com.lavyoung.marketforge.application.strategy.model.RaffleCommand;
import com.lavyoung.marketforge.application.strategy.model.RaffleResult;
import com.lavyoung.marketforge.application.strategy.model.StrategyAwardResult;

import java.util.List;

/**
 * 策略抽奖应用服务。
 * <p>
 * 本接口位于 application 层，负责对外提供策略域相关用例入口，并隔离触发层与策略领域模型。
 * 其中“执行抽奖”用于活动抽奖等上层用例复用，“策略装配”用于把策略概率表、权重规则、
 * 奖品库存等运行时数据预热到缓存，“奖品列表查询”用于前端展示或调试验证策略配置。
 * <p>
 * 注意：本服务不负责校验用户是否具备活动参与资格，也不负责创建活动参与订单和中奖记录。
 * 这些跨域流程应由活动抽奖应用服务编排，策略应用服务只聚焦“如何基于策略计算奖品结果”。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
public interface IStrategyRaffleService {

    /**
     * 基于指定策略执行一次抽奖。
     * <p>
     * 调用方应在进入本方法前完成活动资格、账户额度和参与订单等业务校验。本方法只根据
     * {@link RaffleCommand} 中的用户标识与策略标识调用策略领域服务，完成责任链、规则树、
     * 库存扣减等策略内部计算，并返回命中奖品结果。
     *
     * @param command 策略抽奖命令，包含用户标识和抽奖策略标识
     * @return 策略抽奖结果，包含命中奖品、奖品业务标识和奖品配置
     * @throws NullPointerException command 为 {@code null} 时抛出
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 策略不存在、规则配置异常、奖品库存不足或抽奖业务规则不满足时抛出
     */
    RaffleResult raffle(RaffleCommand command);


    /**
     * 装配指定抽奖策略的运行时缓存。
     * <p>
     * 该方法以策略标识为入口，预热策略概率表、权重概率表、奖品库存和相关规则配置。
     * 适用于策略配置变更、服务启动后预热、压测前初始化等场景。
     *
     * @param strategyId 抽奖策略标识
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 策略不存在、策略奖品配置缺失或装配失败时抛出
     */
    void initStrategyRaffle(Long strategyId);

    /**
     * 根据活动标识装配活动绑定的抽奖策略。
     * <p>
     * 该方法用于活动维度的策略装配场景：调用方只需要提供活动标识，应用服务负责查询活动绑定的
     * 策略标识，并复用策略装配能力完成策略缓存预热。活动 SKU、活动详情和次数配置装配
     * 应由活动装配服务处理。
     *
     * @param activityId 抽奖活动标识
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 活动不存在、活动未绑定策略或策略配置不完整时抛出
     */
    void initStrategyRaffleByActivityId(Long activityId);

    /**
     * 查询指定策略的可抽取奖品列表。
     * <p>
     * 查询结果面向应用层和触发层展示使用，只返回经过应用模型隔离后的奖品信息，
     * 避免直接暴露策略领域实体或持久化对象。
     *
     * @param strategyId 抽奖策略标识
     * @return 隔离领域内部字段后的策略奖品查询结果；策略未配置奖品时返回空列表
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 策略标识非法或奖品配置查询失败时抛出
     */
    List<StrategyAwardResult> queryRaffleStrategyAwardList(Long strategyId);
}
