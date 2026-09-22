package com.lavyoung.marketforge.domain.activity.service.partake;

import com.lavyoung.marketforge.domain.activity.model.aggregate.CreatePartakeOrderAggregate;
import com.lavyoung.marketforge.domain.activity.model.entity.*;
import com.lavyoung.marketforge.domain.activity.model.vo.UserRaffleOrderStateVO;
import com.lavyoung.marketforge.domain.activity.repository.IActivityRepository;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;

/**
 * 默认抽奖参与服务实现。
 * <p>
 * 根据用户活动账户的总、月、日额度生成抽奖参与订单，并在额度不足时阻断参与流程。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/22
 */
@Slf4j
@Service

public class RaffleActivityPartakeService extends AbstractRaffleActivityPartakeService {

    public RaffleActivityPartakeService(IActivityRepository activityRepository) {
        super(activityRepository);
    }

    @Override
    protected ActivityOrderEntity buildActivityRaffleOrder(String userId, Long activityId, LocalDateTime now) {
        ActivityEntity activityEntity = activityRepository.getActivityEntityByIdActivityId(activityId);
        return ActivityOrderEntity.builder()
                .userId(userId)
                .activityId(activityId)
                .activityName(activityEntity.activityName())
                .strategyId(activityEntity.strategyId())
                .orderId(RandomStringUtils.randomNumeric(12)) // 随机12位
                .orderTime(now)
                .state(UserRaffleOrderStateVO.CREATE.getCode())
                .build();
    }

    @Override
    protected CreatePartakeOrderAggregate doFilterAccount(String userId, Long activityId, LocalDateTime now) {
        ActivityAccountEntity activityAccountEntity = activityRepository.queryActivityAccountByUserId(userId, activityId);
        if (activityAccountEntity == null || activityAccountEntity.totalCount() <= 0) {
            throw new BusinessException(BusinessResponseCode.ACTIVITY_ACCOUNT_QUOTA_NOT_ENOUGH);
        }
        // 月账户
        YearMonth yearMonth = YearMonth.of(now.getYear(), now.getMonth());
        ActivityAccountMonthEntity activityAccountMonthEntity = activityRepository.queryActivityAccountMonthByUserId(userId, activityId, yearMonth);
        if (activityAccountMonthEntity != null && activityAccountMonthEntity.monthCount() <= 0) {
            throw new BusinessException(BusinessResponseCode.ACTIVITY_ACCOUNT_QUOTA_MONTH_NOT_ENOUGH);
        }

        boolean isExistAccountMonth = null != activityAccountMonthEntity;
        if (!isExistAccountMonth) {
            activityAccountMonthEntity = new ActivityAccountMonthEntity(
                    userId,
                    activityId,
                    yearMonth,
                    activityAccountEntity.monthCount(),
                    activityAccountEntity.monthCountSurplus()
            );
        }

        // 日账户
        ActivityAccountDayEntity activityAccountDayEntity = activityRepository.queryActivityAccountDayByUserId(userId, activityId, now.toLocalDate());
        if (activityAccountDayEntity != null && activityAccountDayEntity.dayCount() <= 0) {
            throw new BusinessException(BusinessResponseCode.ACTIVITY_ACCOUNT_QUOTA_DAY_NOT_ENOUGH);
        }

        boolean isExistAccountDay = null != activityAccountDayEntity;
        if (!isExistAccountDay) {
            activityAccountDayEntity = new ActivityAccountDayEntity(
                    userId,
                    activityId,
                    now.toLocalDate(),
                    activityAccountEntity.dayCount(),
                    activityAccountEntity.dayCountSurplus()
            );
        }

        //

        return new CreatePartakeOrderAggregate(
                userId,
                activityId,
                activityAccountEntity,
                isExistAccountDay,
                activityAccountDayEntity,
                isExistAccountMonth,
                activityAccountMonthEntity
        );
    }
}
