package com.lavyoung.marketforge.application.award.service;

import com.lavyoung.marketforge.application.award.model.GrantAwardCommand;
import com.lavyoung.marketforge.application.award.model.SaveUserAwardRecordCommand;

/**
 * 奖品应用用例入口。
 * <p>
 * 负责中奖记录持久化、可靠消息创建以及奖品状态流转的事务编排，
 * 不承载具体奖品规则。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
public interface IAwardApplicationService {

    /**
     * 保存用户中奖记录并在同一事务中创建待投递消息。
     *
     * @param command 保存用户中奖记录命令
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 中奖记录或消息任务保存失败时抛出
     */
    void saveUserAwardRecord(SaveUserAwardRecordCommand command);

    /**
     * 将已创建的中奖记录幂等推进为待领取状态。
     *
     * @param userId  用户标识
     * @param orderId 抽奖订单标识
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 中奖记录不存在或状态更新失败时抛出
     */
    void prepareAwardClaim(String userId, String orderId);

    /**
     * 发放用户中奖奖品。
     *
     * <p>应用层根据履约快照中的 {@code awardType} 选择发奖处理器。
     * 处理器负责执行具体权益履约；处理器成功返回后，应用服务统一将
     * 中奖记录推进为已完成状态。</p>
     *
     * <p>同一个抽奖订单可能因可靠消息重试而被重复处理，因此具体处理器
     * 必须使用订单号保证履约幂等。</p>
     *
     * @param command 包含用户、订单和不可变履约快照的发奖命令
     * @throws NullPointerException 命令为空时抛出
     * @throws com.lavyoung.marketforge.types.exception.BusinessException
     *                              处理器不存在、配置非法或奖品履约失败时抛出
     */
    void grantAward(GrantAwardCommand command);
}
