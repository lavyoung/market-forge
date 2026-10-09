package com.lavyoung.marketforge.domain.behavior.repository;

import com.lavyoung.marketforge.domain.behavior.model.aggregate.UserBehaviorRebateAggregate;
import com.lavyoung.marketforge.domain.behavior.model.entity.BehaviorRebateConfigEntity;
import com.lavyoung.marketforge.domain.behavior.model.entity.BehaviorRebateOrderResult;
import com.lavyoung.marketforge.domain.behavior.model.vo.BehaviorTypeVO;

import java.util.List;

/**
 * 用户行为返利仓储端口。
 *
 * <p>该端口屏蔽行为返利配置、返利订单和可靠消息任务的持久化细节。
 * 领域服务只依赖这个抽象，不直接感知 MyBatis、数据库表或 MQ task 表。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/08
 */
public interface IBehaviorRebateRepository {

    /**
     * 查询指定行为类型对应的返利配置。
     *
     * <p>例如签到行为 {@code SIGN} 可以配置多条返利：
     * 一条 SKU 返利、一条积分返利。没有配置时返回空集合，调用方不需要处理 {@code null}。</p>
     *
     * @param behaviorType 用户行为类型
     * @return 行为返利配置集合；没有配置时返回空集合
     */
    List<BehaviorRebateConfigEntity> queryBehaviorRebateConfig(BehaviorTypeVO behaviorType);

    /**
     * 保存用户行为返利聚合。
     *
     * <p>实现方应保证返利订单和本地消息任务在同一事务内完成。
     * 任一订单或任务写入失败，都应回滚整次入账。</p>
     *
     * @param aggregate 用户行为返利聚合
     * @return 保存成功的返利订单号集合；没有订单时返回空集合
     */
    BehaviorRebateOrderResult saveUserBehaviorRebateAggregate(UserBehaviorRebateAggregate aggregate);
}
