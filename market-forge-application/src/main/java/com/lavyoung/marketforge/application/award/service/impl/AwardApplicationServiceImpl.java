package com.lavyoung.marketforge.application.award.service.impl;

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
import com.lavyoung.marketforge.domain.credit.model.vo.CreditAccountBusinessTypeVO;
import com.lavyoung.marketforge.domain.credit.service.ICreditAccountService;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
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
    /**
     * 积分奖品业务键。
     * <p>
     * 与行为返利中的积分类型保持一致，表示该奖品需要进入积分账户发放链路。
     */
    private static final String INTEGRAL_AWARD_KEY = "integral";


    private final IAwardRepository awardRepository;
    private final IAwardStateService awardStateService;
    private final ICreditAccountService creditAccountService;

    private final ApplicationEventPublisher eventPublisher;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveUserAwardRecord(SaveUserAwardRecordCommand command) {
        UserAwardRecordEntity record = toUserAwardRecord(command);
        SendAwardRecordEvent event = new SendAwardRecordEvent(record.userId(),
                record.orderId(), record.awardId(), command.awardKey(), command.awardConfig(), record.awardTitle());

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grantAward(GrantAwardCommand command) {
        if (!INTEGRAL_AWARD_KEY.equals(command.awardKey())) {
            throw BusinessException.of(BusinessResponseCode.AWARD_TYPE_NOT_SUPPORTED, command.awardKey(), command.awardId());
        }
        Long amount = parseIntegralAmount(command.awardConfig());
        try {
            creditAccountService.grantCredit(command.userId(), command.orderId(), amount, CreditAccountBusinessTypeVO.AWARD_GRANT);
        } catch (BusinessException e) {
            // 重复处理幂等成功
            if (e.getResponseCode() != BusinessResponseCode.POINTS_TRANSACTION_DUPLICATED) {
                throw e;
            }
        }
        awardStateService.completeGrant(command.userId(), command.orderId());
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

    private Long parseIntegralAmount(String awardConfig) {
        try {
            long amount = Long.parseLong(awardConfig);
            if (amount <= 0) {
                throw BusinessException.of(BusinessResponseCode.USER_AWARD_POINTS_AMOUNT_INVALID, awardConfig);
            }
            return amount;
        } catch (NumberFormatException exception) {
            throw BusinessException.of(BusinessResponseCode.USER_AWARD_POINTS_AMOUNT_INVALID, awardConfig);
        }
    }
}
