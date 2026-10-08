package com.lavyoung.marketforge.domain.behavior.repository;

import com.lavyoung.marketforge.domain.behavior.model.entity.BehaviorRebateConfigEntity;
import com.lavyoung.marketforge.domain.behavior.model.entity.UserBehaviorRebateOrderEntity;
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
     * 保存用户行为返利订单。
     *
     * <p>实现方应保证同一用户、同一外部业务号、同一返利类型不会重复入账。
     * 后续接入可靠消息时，订单落库和 task 落库也应在同一事务内完成。</p>
     *
     * @param rebateOrders 用户行为返利订单集合
     * @return 保存成功的返利订单号集合；没有订单时返回空集合
     */
    List<String> saveUserBehaviorRebateOrders(List<UserBehaviorRebateOrderEntity> rebateOrders);
}
