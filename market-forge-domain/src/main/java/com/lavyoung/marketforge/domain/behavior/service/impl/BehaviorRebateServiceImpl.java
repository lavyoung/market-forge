package com.lavyoung.marketforge.domain.behavior.service.impl;

import com.lavyoung.marketforge.domain.behavior.event.SendRebateEvent;
import com.lavyoung.marketforge.domain.behavior.model.aggregate.UserBehaviorRebateAggregate;
import com.lavyoung.marketforge.domain.behavior.model.entity.BehaviorEntity;
import com.lavyoung.marketforge.domain.behavior.model.entity.BehaviorRebateConfigEntity;
import com.lavyoung.marketforge.domain.behavior.model.entity.BehaviorRebateOrderResult;
import com.lavyoung.marketforge.domain.behavior.model.entity.UserBehaviorRebateOrderEntity;
import com.lavyoung.marketforge.domain.behavior.repository.IBehaviorRebateRepository;
import com.lavyoung.marketforge.domain.behavior.service.IBehaviorRebateService;
import com.lavyoung.marketforge.types.common.Constants;
import com.lavyoung.marketforge.types.utils.DateUtil;
import com.lavyoung.marketforge.types.utils.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;


/**
 * 用户行为返利领域服务默认实现。
 *
 * <p>负责根据用户行为和返利配置生成用户返利订单。
 * 本实现只处理领域规则，不直接访问数据库，也不直接发送 MQ。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BehaviorRebateServiceImpl implements IBehaviorRebateService {

    private final IBehaviorRebateRepository behaviorRebateRepository;

    @Override
    public BehaviorRebateOrderResult createOrder(BehaviorEntity behavior) {
        BehaviorEntity validBehavior = Objects.requireNonNull(behavior, "behavior must not be null");
        List<BehaviorRebateConfigEntity> configs = behaviorRebateRepository.queryBehaviorRebateConfig(validBehavior.behaviorType());

        if (configs.isEmpty()) {
            return new BehaviorRebateOrderResult(List.of(), List.of());
        }

        List<UserBehaviorRebateOrderEntity> rebateOrders = configs.stream()
                .map(config -> buildRebateOrder(validBehavior, config))
                .toList();

        List<SendRebateEvent> events = rebateOrders.stream().map(order -> new SendRebateEvent(
                order.userId(),
                order.bizId(),
                order.rebateType(),
                order.rebateConfig()
        )).toList();

        return behaviorRebateRepository.saveUserBehaviorRebateAggregate(new UserBehaviorRebateAggregate(
                validBehavior.userId(),
                rebateOrders,
                events
        ));
    }

    private UserBehaviorRebateOrderEntity buildRebateOrder(BehaviorEntity behavior, BehaviorRebateConfigEntity config) {
        return new UserBehaviorRebateOrderEntity(
                behavior.userId(),
                IdGenerator.nextId(Constants.BusinessNoPrefix.REBATE_ORDER_PREFIX),
                behavior.behaviorType(),
                config.rebateType(),
                config.rebateConfig(),
                behavior.userId() + Constants.UNDERLINE + config.rebateType().getCode() + Constants.UNDERLINE + behavior.outBusinessNo(),
                behavior.outBusinessNo(),
                DateUtil.now()
        );
    }
}
