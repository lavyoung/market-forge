package com.lavyoung.marketforge.application.award.service.impl;

import com.lavyoung.marketforge.application.award.model.SaveUserAwardRecordCommand;
import com.lavyoung.marketforge.application.award.service.IAwardApplicationService;
import com.lavyoung.marketforge.application.message.TaskCreatedEvent;
import com.lavyoung.marketforge.domain.award.event.SendAwardRecordEvent;
import com.lavyoung.marketforge.domain.award.model.aggreate.UserAwardRecordAggregate;
import com.lavyoung.marketforge.domain.award.model.entity.UserAwardRecordEntity;
import com.lavyoung.marketforge.domain.award.model.valobj.AwardStateVO;
import com.lavyoung.marketforge.domain.award.repository.IAwardRepository;
import com.lavyoung.marketforge.domain.award.service.IAwardStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 奖品应用服务默认实现。
 * <p>
 * 负责应用命令到领域模型的转换、事务控制和可靠消息投递编排，
 * 具体奖品状态规则委托给奖品领域服务处理。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
public class AwardApplicationServiceImpl implements IAwardApplicationService {

    private final IAwardRepository awardRepository;
    private final IAwardStateService awardStateService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveUserAwardRecord(SaveUserAwardRecordCommand command) {
        UserAwardRecordEntity record = toUserAwardRecord(command);
        SendAwardRecordEvent event = new SendAwardRecordEvent(
                record.userId(), record.orderId(), record.awardId(), record.awardTitle());

        awardRepository.saveUserAwardRecord(new UserAwardRecordAggregate(record, event));
        eventPublisher.publishEvent(new TaskCreatedEvent(record.userId(), event.eventId()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void prepareAwardClaim(String userId, String orderId) {
        awardStateService.prepareClaim(userId, orderId);
    }

    private UserAwardRecordEntity toUserAwardRecord(SaveUserAwardRecordCommand command) {
        return UserAwardRecordEntity.builder()
                .userId(command.userId())
                .activityId(command.activityId())
                .strategyId(command.strategyId())
                .orderId(command.orderId())
                .awardId(command.awardId())
                .awardTitle(command.awardTitle())
                .awardTime(command.awardTime())
                .awardState(AwardStateVO.CREATE)
                .build();
    }
}
