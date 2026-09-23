package com.lavyoung.marketforge.domain.award.service.impl;

import com.lavyoung.marketforge.domain.award.event.SendAwardRecordEvent;
import com.lavyoung.marketforge.domain.award.model.aggreate.UserAwardRecordAggregate;
import com.lavyoung.marketforge.domain.award.model.entity.TaskEntity;
import com.lavyoung.marketforge.domain.award.model.entity.UserAwardRecordEntity;
import com.lavyoung.marketforge.domain.award.model.valobj.TaskStateVO;
import com.lavyoung.marketforge.domain.award.repository.IAwardRepository;
import com.lavyoung.marketforge.domain.award.service.IAwardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 *
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AwardServiceImpl implements IAwardService {

    private final IAwardRepository awardRepository;

    @Override
    public void saveUserAwardRecord(UserAwardRecordEntity userAwardRecordEntity) {
        // 构建MQ消息对象
        SendAwardRecordEvent event = new SendAwardRecordEvent(
                userAwardRecordEntity.userId(),
                userAwardRecordEntity.awardId(),
                userAwardRecordEntity.awardTitle()
        );
        // 构建Task任务对象
        TaskEntity task = TaskEntity.builder()
                .eventId(event.eventId())
                .topic(event.exchange())
                .occurredAt(LocalDateTime.ofInstant(event.occurredAt(), ZoneId.systemDefault()))
                .state(TaskStateVO.CREATE.getCode())
                .eventType(event.eventType())
                .messageBody(event)
                .build();

        // 构建聚合对象
        UserAwardRecordAggregate aggregate = UserAwardRecordAggregate.builder()
                .userAwardRecordEntity(userAwardRecordEntity)
                .taskEntity(task)
                .build();

        awardRepository.saveUserAwardRecord(aggregate);
    }
}
