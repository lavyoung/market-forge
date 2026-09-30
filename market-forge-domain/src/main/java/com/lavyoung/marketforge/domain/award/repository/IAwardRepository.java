package com.lavyoung.marketforge.domain.award.repository;

import com.lavyoung.marketforge.domain.award.model.aggreate.UserAwardRecordAggregate;
import com.lavyoung.marketforge.domain.award.model.entity.UserAwardRecordEntity;
import com.lavyoung.marketforge.domain.award.model.valobj.AwardStateVO;

import java.util.Optional;

/**
 * 用户中奖记录仓储端口。
 * <p>
 * 负责中奖记录、发奖状态和可靠消息任务的持久化抽象。领域层只表达需要保存或推进的业务事实，
 * 具体分片路由、幂等约束和本地消息表写入由基础设施实现。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/23
 */
public interface IAwardRepository {

    /**
     * 保存用户中奖记录并创建对应的本地消息任务。
     * <p>
     * 实现方应在同一事务中写入中奖记录和任务表，确保事务提交后可由即时投递或补偿任务发送 MQ。
     *
     * @param aggregate 用户中奖记录聚合
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 中奖记录或任务保存失败时抛出
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
