package com.lavyoung.marketforge.infrastructure.persistent.repository;

import com.lavyoung.marketforge.domain.behavior.model.entity.BehaviorRebateConfigEntity;
import com.lavyoung.marketforge.domain.behavior.model.entity.UserBehaviorRebateOrderEntity;
import com.lavyoung.marketforge.domain.behavior.model.vo.BehaviorTypeVO;
import com.lavyoung.marketforge.domain.behavior.repository.IBehaviorRebateRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 *
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/08
 */
@Repository
public class BehaviorRebateRepository implements IBehaviorRebateRepository {


    @Override
    public List<BehaviorRebateConfigEntity> queryBehaviorRebateConfig(BehaviorTypeVO behaviorType) {
        return List.of();
    }

    @Override
    public List<String> saveUserBehaviorRebateOrders(List<UserBehaviorRebateOrderEntity> rebateOrders) {
        return List.of();
    }
}
