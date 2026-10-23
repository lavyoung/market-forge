package com.lavyoung.marketforge.application.award.service.impl;

import com.lavyoung.marketforge.application.award.grant.AwardGrantHandler;
import com.lavyoung.marketforge.application.award.grant.AwardGrantHandlerRegistry;
import com.lavyoung.marketforge.application.award.model.GrantAwardCommand;
import com.lavyoung.marketforge.application.award.model.SaveUserAwardRecordCommand;
import com.lavyoung.marketforge.application.award.service.IAwardApplicationService;
import com.lavyoung.marketforge.application.message.TaskCreatedEvent;
import com.lavyoung.marketforge.domain.award.event.SendAwardRecordEvent;
import com.lavyoung.marketforge.domain.award.model.aggreate.UserAwardRecordAggregate;
import com.lavyoung.marketforge.domain.award.model.entity.UserAwardRecordEntity;
import com.lavyoung.marketforge.domain.award.model.vo.AwardStateVO;
import com.lavyoung.marketforge.domain.award.repository.IAwardRepository;
import com.lavyoung.marketforge.domain.award.service.IAwardStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * 奖品应用服务默认实现。
 *
 * <p>负责中奖记录持久化、可靠消息创建以及发奖状态流转等应用编排。
 * 具体奖品如何履约由 {@link AwardGrantHandler} 实现，本服务不解析奖品配置，
 * 也不依赖积分、优惠券或第三方供应商等具体发奖能力。</p>
 *
 * <p>发奖处理器成功返回后，本服务才会将中奖记录推进到完成状态。
 * 处理器抛出异常时事务回滚，异常继续交给消息消费重试和死信机制处理。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
public class AwardApplicationServiceImpl implements IAwardApplicationService {

    private final IAwardRepository awardRepository;
    private final IAwardStateService awardStateService;
    private final AwardGrantHandlerRegistry awardGrantHandlerRegistry;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveUserAwardRecord(SaveUserAwardRecordCommand command) {
        UserAwardRecordEntity record = toUserAwardRecord(command);
        SendAwardRecordEvent event = new SendAwardRecordEvent(
                record.userId(),
                record.orderId(),
                record.awardId(),
                command.fulfillmentSnapshot(),
                record.awardTitle()
        );

        awardRepository.saveUserAwardRecord(new UserAwardRecordAggregate(record, event));
        eventPublisher.publishEvent(new TaskCreatedEvent(record.userId(), event.eventId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void prepareAwardClaim(String userId, String orderId) {
        awardStateService.prepareClaim(userId, orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grantAward(GrantAwardCommand command) {
        GrantAwardCommand validCommand = Objects.requireNonNull(command, "command must not be null");
        String awardType = validCommand.fulfillmentSnapshot().awardType();

        AwardGrantHandler handler = awardGrantHandlerRegistry.getRequired(awardType);
        handler.grant(validCommand);

        awardStateService.completeGrant(validCommand.userId(), validCommand.orderId());
    }

    /**
     * 将保存中奖记录命令转换为中奖记录领域实体。
     *
     * <p>新中奖记录统一从 {@link AwardStateVO#CREATE} 状态开始。
     * 履约快照不写入中奖记录主表，而是保存在同事务创建的可靠消息中，
     * 供后续异步发奖使用。</p>
     *
     * @param command 保存中奖记录命令
     * @return 初始状态为创建中的中奖记录实体
     * @throws NullPointerException 命令为空时抛出
     */
    private UserAwardRecordEntity toUserAwardRecord(SaveUserAwardRecordCommand command) {
        SaveUserAwardRecordCommand validCommand =
                Objects.requireNonNull(command, "command must not be null");

        return UserAwardRecordEntity.builder()
                .userId(validCommand.userId())
                .activityId(validCommand.activityId())
                .strategyId(validCommand.strategyId())
                .orderId(validCommand.orderId())
                .awardId(validCommand.awardId())
                .awardTitle(validCommand.awardTitle())
                .awardTime(validCommand.awardTime())
                .awardState(AwardStateVO.CREATE)
                .build();
    }
}