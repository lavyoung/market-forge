package com.lavyoung.marketforge.domain.award.repository;

import com.lavyoung.marketforge.domain.award.model.aggreate.UserAwardRecordAggregate;
import com.lavyoung.marketforge.domain.award.model.entity.UserAwardRecordEntity;
import com.lavyoung.marketforge.domain.award.model.valobj.AwardStateVO;

import java.util.Optional;

/**
 *
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/23
 */
public interface IAwardRepository {

    /**
     * 保存用户发奖记录 只做数据持久化
     *
     * @param aggregate
     */
    void saveUserAwardRecord(UserAwardRecordAggregate aggregate);

    /**
     * 按用户和抽奖订单查询中奖记录。
     *
     * @param userId  用户标识
     * @param orderId 抽奖订单标识
     * @return 匹配的中奖记录；不存在时返回空
     */
    Optional<UserAwardRecordEntity> queryUserAwardRecord(String userId, String orderId);

    /**
     * 使用当前状态作为并发条件推进中奖记录状态。
     *
     * @param userId       用户标识
     * @param orderId      抽奖订单标识
     * @param currentState 当前状态
     * @param targetState  目标状态
     * @return 状态更新成功时返回 {@code true}
     */
    boolean updateAwardState(String userId, String orderId, AwardStateVO currentState, AwardStateVO targetState);
}
