package com.lavyoung.marketforge.infrastructure.persistent.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lavyoung.marketforge.domain.credit.event.CreditAdjustSuccessEvent;
import com.lavyoung.marketforge.domain.credit.model.aggregate.CreditAccountAdjustAggregate;
import com.lavyoung.marketforge.domain.credit.model.entity.CreditAccountEntity;
import com.lavyoung.marketforge.domain.credit.model.entity.CreditAccountOrderEntity;
import com.lavyoung.marketforge.domain.credit.repository.ICreditAccountRepository;
import com.lavyoung.marketforge.domain.message.model.entity.TaskEntity;
import com.lavyoung.marketforge.domain.message.model.valobj.TaskStateVO;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.TaskAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.credit.CreditAccountAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.credit.CreditAccountOrderAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.dao.credit.IUserCreditAccountDao;
import com.lavyoung.marketforge.infrastructure.persistent.dao.credit.IUserCreditAccountOrderDao;
import com.lavyoung.marketforge.infrastructure.persistent.dao.mq.ITaskDao;
import com.lavyoung.marketforge.infrastructure.persistent.po.TaskPO;
import com.lavyoung.marketforge.infrastructure.persistent.po.credit.UserCreditAccountOrderPO;
import com.lavyoung.marketforge.infrastructure.persistent.po.credit.UserCreditAccountPO;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.messaging.IntegrationEventCodec;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;


/**
 * 用户积分账户仓储实现。
 * <p>
 * 负责将积分账户订单和账户余额变动落到数据库。该实现使用积分订单的
 * {@code bizId} 唯一索引做幂等控制，避免 MQ 重复消费导致重复加积分。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/11
 */
@Repository
@RequiredArgsConstructor
public class CreditAccountRepository implements ICreditAccountRepository {

    private final IUserCreditAccountDao userCreditAccountDao;
    private final IUserCreditAccountOrderDao userCreditAccountOrderDao;
    private final ITaskDao taskDao;

    private final CreditAccountAssembler creditAccountAssembler;
    private final CreditAccountOrderAssembler creditAccountOrderAssembler;
    private final TaskAssembler taskAssembler;

    private final IntegrationEventCodec eventCodec;

    @Override
    public Optional<CreditAccountEntity> queryCreditAccountByUserId(String userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        return Optional.ofNullable(userCreditAccountDao.selectOne(Wrappers.lambdaQuery(UserCreditAccountPO.class)
                .eq(UserCreditAccountPO::getUserId, userId))).map(creditAccountAssembler::toEntity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveCreditAccountAdjust(CreditAccountAdjustAggregate aggregate) {
        CreditAccountOrderEntity order = aggregate.order();

        saveOrderIdempotently(order);
        switch (order.tradeType()) {
            case INCOME -> increaseOrCreateAccount(order.userId(), order.amount());
            case EXPENSE -> {
                decreaseAccount(order.userId(), order.amount());
                saveCreditAdjustSuccessTask(order);
            }
        }
    }

    private void saveOrderIdempotently(CreditAccountOrderEntity order) {
        UserCreditAccountOrderPO orderPO = creditAccountOrderAssembler.toPO(order);
        try {
            int rows = userCreditAccountOrderDao.insert(orderPO);
            if (rows < 1) {
                throw BusinessException.of(BusinessResponseCode.POINTS_GRANT_FAILED, order.userId(), order.bizId());
            }
        } catch (DuplicateKeyException e) {
            throw BusinessException.of(BusinessResponseCode.POINTS_TRANSACTION_DUPLICATED, order.userId(), order.bizId());
        }
    }

    private void decreaseAccount(String userId, Long amount) {
        int rows = userCreditAccountDao.decrementCreditAmount(userId, amount);
        if (rows != 1) {
            throw BusinessException.of(BusinessResponseCode.POINTS_BALANCE_NOT_ENOUGH, userId, amount);
        }
    }

    private void increaseOrCreateAccount(String userId, Long amount) {
        int rows = userCreditAccountDao.incrementCreditAmount(userId, amount);
        if (rows == 1) {
            return;
        }
        UserCreditAccountPO accountPO = new UserCreditAccountPO();
        accountPO.setUserId(userId);
        accountPO.setTotalAmount(amount);
        accountPO.setAvailableAmount(amount);

        try {
            int inserted = userCreditAccountDao.insert(accountPO);
            if (inserted < 1) {
                throw BusinessException.of(BusinessResponseCode.POINTS_GRANT_FAILED, userId, amount);
            }
        } catch (DuplicateKeyException e) {
            int retryUpdated = userCreditAccountDao.incrementCreditAmount(userId, amount);
            if (retryUpdated != 1) {
                throw BusinessException.of(BusinessResponseCode.POINTS_GRANT_FAILED, userId, amount);
            }
        }
    }

    /**
     * 保存积分调额成功可靠消息任务。
     *
     * <p>积分支出成功代表积分支付完成。这里在同一个事务中写入可靠消息任务，
     * 后续由任务投递组件发送 MQ，消费者收到消息后完成活动 SKU 发货。</p>
     *
     * @param order 积分账户流水订单
     * @throws BusinessException 当任务创建失败时抛出
     */
    private void saveCreditAdjustSuccessTask(CreditAccountOrderEntity order) {
        CreditAdjustSuccessEvent event = new CreditAdjustSuccessEvent(
                order.userId(),
                order.orderId(),
                order.bizId(),
                order.amount()
        );

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
