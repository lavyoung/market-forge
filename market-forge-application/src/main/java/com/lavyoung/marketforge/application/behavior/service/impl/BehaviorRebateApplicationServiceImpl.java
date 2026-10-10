package com.lavyoung.marketforge.application.behavior.service.impl;

import com.lavyoung.marketforge.application.behavior.model.BehaviorRebateOrderCreateResult;
import com.lavyoung.marketforge.application.behavior.model.CreateBehaviorRebateOrderCommand;
import com.lavyoung.marketforge.application.behavior.service.IBehaviorRebateApplicationService;
import com.lavyoung.marketforge.application.message.TaskCreatedEvent;
import com.lavyoung.marketforge.domain.behavior.model.entity.BehaviorEntity;
import com.lavyoung.marketforge.domain.behavior.model.entity.BehaviorRebateOrderResult;
import com.lavyoung.marketforge.domain.behavior.model.vo.BehaviorTypeVO;
import com.lavyoung.marketforge.domain.behavior.service.IBehaviorRebateService;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Objects;


/**
 * 用户行为返利应用服务默认实现。
 *
 * <p>本类只做用例编排：将应用命令转换为领域行为实体，调用领域服务完成返利订单和 task 落库，
 * 再发布 {@link TaskCreatedEvent} 触发事务提交后的即时 MQ 投递。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BehaviorRebateApplicationServiceImpl implements IBehaviorRebateApplicationService {

    private final IBehaviorRebateService behaviorRebateService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public BehaviorRebateOrderCreateResult createOrder(CreateBehaviorRebateOrderCommand command) {
        CreateBehaviorRebateOrderCommand validCommand = Objects.requireNonNull(command, "command must not be null");

        if (command.behaviorType() == null || command.behaviorType().isBlank()) {
            throw BusinessException.of(BusinessResponseCode.BEHAVIOR_REBATE_CONFIG_INVALID, command.behaviorType());
        }

        BehaviorTypeVO behaviorType = BehaviorTypeVO.fromCode(validCommand.behaviorType())
                .orElseThrow(() -> BusinessException.of(BusinessResponseCode.BEHAVIOR_REBATE_CONFIG_INVALID, command.behaviorType()));

        BehaviorRebateOrderResult result = behaviorRebateService.createOrder(new BehaviorEntity(
                validCommand.userId(),
                behaviorType,
                validCommand.outBusinessNo()
        ));
        result.eventIds().forEach(eventId -> {
            eventPublisher.publishEvent(new TaskCreatedEvent(validCommand.userId(), eventId));
        });
        return new BehaviorRebateOrderCreateResult(result.orderIds(), result.eventIds());
    }
}
