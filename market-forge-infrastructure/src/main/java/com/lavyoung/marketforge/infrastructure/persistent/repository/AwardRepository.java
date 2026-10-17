package com.lavyoung.marketforge.infrastructure.persistent.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lavyoung.marketforge.domain.award.event.SendAwardRecordEvent;
import com.lavyoung.marketforge.domain.award.model.aggreate.UserAwardRecordAggregate;
import com.lavyoung.marketforge.domain.award.model.entity.UserAwardRecordEntity;
import com.lavyoung.marketforge.domain.award.model.vo.AwardStateVO;
import com.lavyoung.marketforge.domain.award.model.vo.AwardUnlockCountQueryVO;
import com.lavyoung.marketforge.domain.award.repository.IAwardRepository;
import com.lavyoung.marketforge.domain.message.model.entity.TaskEntity;
import com.lavyoung.marketforge.domain.message.model.valobj.TaskStateVO;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.TaskAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.UserAwardRecordAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.dao.IUserAwardRecordDao;
import com.lavyoung.marketforge.infrastructure.persistent.dao.mq.ITaskDao;
import com.lavyoung.marketforge.infrastructure.persistent.po.TaskPO;
import com.lavyoung.marketforge.infrastructure.persistent.po.UserAwardRecordPO;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.messaging.IntegrationEventCodec;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;

/**
 * 用户中奖记录仓储实现。
 * <p>
 * 负责将中奖记录写入用户分片表，并在同一事务上下文中写入可靠消息任务表。
 * 该仓储只处理持久化和状态条件更新，不直接发布 MQ。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/23
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class AwardRepository implements IAwardRepository {

    private final IUserAwardRecordDao userAwardRecordDao;
    private final ITaskDao taskDao;

    private final UserAwardRecordAssembler userAwardRecordAssembler;
    private final TaskAssembler taskAssembler;
    private final IntegrationEventCodec eventCodec;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveUserAwardRecord(UserAwardRecordAggregate aggregate) {
        UserAwardRecordEntity userAwardRecordEntity = aggregate.userAwardRecordEntity();
        SendAwardRecordEvent event = aggregate.sendAwardRecordEvent();

        // 保存发奖记录
        UserAwardRecordPO po = userAwardRecordAssembler.toPO(userAwardRecordEntity);
        int recordRows = userAwardRecordDao.insert(po);
        if (recordRows < 1) {
            throw BusinessException.of(BusinessResponseCode.USER_AWARD_RECORD_CREATE_FAILED);
        }

        TaskEntity taskEntity = TaskEntity.builder()
                .userId(userAwardRecordEntity.userId())
                .eventId(event.eventId())
                .topic(event.exchange())
                .eventType(event.routingKey())
                .messageBody(eventCodec.serialize(event))
                .occurredAt(LocalDateTime.ofInstant(event.occurredAt(), ZoneId.systemDefault()))
                .state(TaskStateVO.CREATE.getCode())
                .retryCount(0)
                .build();

        // 发出mq消息记录
        TaskPO taskPO = taskAssembler.toPO(taskEntity);
        int res = taskDao.insert(taskPO);
        if (res < 1) {
            throw BusinessException.of(BusinessResponseCode.TASK_CREATE_FAILED);
        }
    }

    @Override
    public Optional<UserAwardRecordEntity> queryUserAwardRecord(String userId, String orderId) {
        QueryWrapper<UserAwardRecordPO> queryWrapper = new QueryWrapper<UserAwardRecordPO>()
                .eq("user_id", userId)
                .eq("order_id", orderId)
                .last("LIMIT 1");
        return Optional.ofNullable(userAwardRecordDao.selectOne(queryWrapper))
                .map(userAwardRecordAssembler::toEntity);
    }

    @Override
    public boolean updateAwardState(String userId, String orderId, AwardStateVO currentState, AwardStateVO targetState) {
        UpdateWrapper<UserAwardRecordPO> updateWrapper = new UpdateWrapper<UserAwardRecordPO>()
                .eq("user_id", userId)
                .eq("order_id", orderId)
                .eq("award_state", currentState.getCode())
                .set("award_state", targetState.getCode());
        return userAwardRecordDao.update(null, updateWrapper) == 1;
    }

    @Override
    public long countCompletedRaffles(AwardUnlockCountQueryVO queryVO) {
        // 统计已完成抽奖记录次数
        Objects.requireNonNull(queryVO, "query must not be null");
        LambdaQueryWrapper<UserAwardRecordPO> queryWrapper = Wrappers.lambdaQuery(UserAwardRecordPO.class)
                .eq(UserAwardRecordPO::getUserId, queryVO.userId())
                .eq(UserAwardRecordPO::getActivityId, queryVO.activityId());
        switch (queryVO.unlockScope()) {
            case DAY -> {
                // award_time >= 当天 00:00:00
                // AND award_time < 下一天 00:00:00
                LocalDateTime rangeStart = queryVO.statisticsDate().atStartOfDay();
                LocalDateTime rangeEnd = queryVO.statisticsDate().plusDays(1).atStartOfDay();
                queryWrapper.ge(UserAwardRecordPO::getAwardTime, rangeStart)
                        .lt(UserAwardRecordPO::getAwardTime, rangeEnd);
            }
            case TOTAL -> {
                // 活动累计模式只按用户和活动过滤，不添加时间条件。
            }
        }
        return userAwardRecordDao.selectCount(queryWrapper);
    }
}
