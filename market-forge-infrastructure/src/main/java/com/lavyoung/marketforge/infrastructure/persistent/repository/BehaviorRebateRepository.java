package com.lavyoung.marketforge.infrastructure.persistent.repository;

import com.lavyoung.marketforge.domain.behavior.event.SendRebateEvent;
import com.lavyoung.marketforge.domain.behavior.model.aggregate.UserBehaviorRebateAggregate;
import com.lavyoung.marketforge.domain.behavior.model.entity.BehaviorRebateConfigEntity;
import com.lavyoung.marketforge.domain.behavior.model.entity.BehaviorRebateOrderResult;
import com.lavyoung.marketforge.domain.behavior.model.entity.UserBehaviorRebateOrderEntity;
import com.lavyoung.marketforge.domain.behavior.model.vo.BehaviorTypeVO;
import com.lavyoung.marketforge.domain.behavior.repository.IBehaviorRebateRepository;
import com.lavyoung.marketforge.domain.message.model.entity.TaskEntity;
import com.lavyoung.marketforge.domain.message.model.valobj.TaskStateVO;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.TaskAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.behavior.BehaviorRebateConfigAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.behavior.UserBehaviorRebateOrderAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.dao.behavior.IBehaviorRebateConfigDao;
import com.lavyoung.marketforge.infrastructure.persistent.dao.behavior.IUserBehaviorRebateOrderDao;
import com.lavyoung.marketforge.infrastructure.persistent.dao.mq.ITaskDao;
import com.lavyoung.marketforge.infrastructure.persistent.po.TaskPO;
import com.lavyoung.marketforge.infrastructure.persistent.po.behavior.UserBehaviorRebateOrderPO;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.messaging.IntegrationEventCodec;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

/**
 * 用户行为返利仓储实现。
 *
 * <p>负责读取行为返利配置，并将领域层生成的用户返利订单写入数据库。
 * 本类只处理持久化细节，不承载返利订单生成规则。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/08
 */
@Repository
@RequiredArgsConstructor
public class BehaviorRebateRepository implements IBehaviorRebateRepository {

    private final IBehaviorRebateConfigDao behaviorRebateConfigDao;
    private final IUserBehaviorRebateOrderDao userBehaviorRebateOrderDao;

    private final BehaviorRebateConfigAssembler behaviorRebateConfigAssembler;
    private final UserBehaviorRebateOrderAssembler userBehaviorRebateOrderAssembler;

    private final ITaskDao taskDao;
    private final TaskAssembler taskAssembler;
    private final IntegrationEventCodec eventCodec;

    @Override
    public List<BehaviorRebateConfigEntity> queryBehaviorRebateConfig(BehaviorTypeVO behaviorType) {
        Objects.requireNonNull(behaviorType, "behaviorType must not be null");
        return behaviorRebateConfigAssembler.toEntities(behaviorRebateConfigDao.queryByBehaviorType(behaviorType.getCode()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BehaviorRebateOrderResult saveUserBehaviorRebateAggregate(UserBehaviorRebateAggregate aggregate) {
        UserBehaviorRebateAggregate validAggregate = Objects.requireNonNull(aggregate, "aggregate must not be null");
        List<UserBehaviorRebateOrderEntity> rebateOrders = validAggregate.orders();
        if (rebateOrders == null || rebateOrders.isEmpty()) {
            return new BehaviorRebateOrderResult(List.of(), List.of());
        }
        rebateOrders.forEach(order -> {
            UserBehaviorRebateOrderPO orderPO = userBehaviorRebateOrderAssembler.toPO(order);
            int rows = userBehaviorRebateOrderDao.insert(orderPO);
            if (rows < 1) {
                throw BusinessException.of(BusinessResponseCode.BEHAVIOR_REBATE_ORDER_CREATE_FAILED, order.bizId());
            }
        });

        if (validAggregate.events() == null || validAggregate.events().isEmpty()) {
            throw BusinessException.of(BusinessResponseCode.BEHAVIOR_REBATE_MESSAGE_SEND_FAILED);
        }

        validAggregate.events().forEach(this::saveTask);

        return new BehaviorRebateOrderResult(
                rebateOrders.stream()
                        .map(UserBehaviorRebateOrderEntity::orderId)
                        .toList(),
                validAggregate.events().stream()
                        .map(SendRebateEvent::eventId)
                        .toList()
        );
    }

    /**
     * 保存用户行为返利事件对应的本地消息任务。
     *
     * <p>任务初始状态为 {@code create}，事务提交后由应用事件触发即时投递，
     * 定时补偿任务也会扫描该状态进行兜底投递。</p>
     *
     * <p>TODO 后续将 {@code IntegrationEvent -> TaskEntity} 的转换抽取为可靠消息任务组件，
     * 避免不同仓储重复组装 task。</p>
     *
     * @param event 用户行为返利发放事件
     */
    private void saveTask(SendRebateEvent event) {
        TaskEntity taskEntity = TaskEntity.builder()
                .userId(event.userId())
                .eventId(event.eventId())
                .topic(event.exchange())
                .eventType(event.routingKey())
                .messageBody(eventCodec.serialize(event))
                .occurredAt(LocalDateTime.ofInstant(event.occurredAt(), ZoneId.systemDefault()))
                .state(TaskStateVO.CREATE.getCode())
                .retryCount(0)
                .build();

        TaskPO taskPO = taskAssembler.toPO(taskEntity);
        int rows = taskDao.insert(taskPO);
        if (rows < 1) {
            throw BusinessException.of(BusinessResponseCode.TASK_CREATE_FAILED);
        }
    }
}
