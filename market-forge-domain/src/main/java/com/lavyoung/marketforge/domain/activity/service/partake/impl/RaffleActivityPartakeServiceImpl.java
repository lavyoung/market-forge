package com.lavyoung.marketforge.domain.activity.service.partake.impl;

import com.lavyoung.marketforge.domain.activity.model.aggregate.CreatePartakeOrderAggregate;
import com.lavyoung.marketforge.domain.activity.model.entity.*;
import com.lavyoung.marketforge.domain.activity.model.vo.UserRaffleOrderStateVO;
import com.lavyoung.marketforge.domain.activity.repository.IActivityRepository;
import com.lavyoung.marketforge.domain.activity.service.partake.AbstractRaffleActivityPartakeService;
import com.lavyoung.marketforge.types.common.Constants;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import com.lavyoung.marketforge.types.utils.IdGenerator;
import lombok.extern.slf4j.Slf4j;
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
public class RaffleActivityPartakeServiceImpl extends AbstractRaffleActivityPartakeService {

    public RaffleActivityPartakeServiceImpl(IActivityRepository activityRepository) {
        super(activityRepository);
    }

    /**
     * 构建抽奖参与订单。
     * <p>
     * 参与订单代表用户已经通过活动账户额度校验并准备进入抽奖流程的一次资格。订单号使用抽奖订单
     * 前缀加通用 ID 生成器生成，便于从日志和数据库中区分该订单来自活动抽奖参与链路。
     *
     * @param partakeRaffleActivity 用户参与活动入参
     * @param activityEntity        已校验通过的活动实体
     * @param now                   当前业务时间
     * @return 待保存的抽奖参与订单
     */
    @Override
    protected ActivityOrderEntity buildActivityRaffleOrder(PartakeRaffleActivityEntity partakeRaffleActivity, ActivityEntity activityEntity, LocalDateTime now) {
        String userId = partakeRaffleActivity.userId();
        Long activityId = partakeRaffleActivity.activityId();
        return ActivityOrderEntity.builder()
                .userId(userId)
                .sku(partakeRaffleActivity.sku())
                .activityId(activityId)
                .activityName(activityEntity.activityName())
                .strategyId(activityEntity.strategyId())
                .orderId(Constants.BusinessNoPrefix.RAFFLE_ORDER_PREFIX + IdGenerator.nextId())
                .orderTime(now)
                .state(UserRaffleOrderStateVO.CREATE.getCode())
                .build();
    }

    /**
     * 校验用户活动账户额度并构建扣减聚合。
     * <p>
     * 先校验总账户剩余额度，再分别校验月账户和日账户；当月账户或日账户尚未初始化时，基于总账户
     * 的月/日额度配置创建待落库实体。真正的扣减和订单保存由仓储在事务中完成。
     *
     * @param userId     用户标识
     * @param activityId 活动标识
     * @param now        当前业务时间
     * @return 创建参与订单所需的账户聚合
     * @throws BusinessException 当总、月或日额度不足时抛出
     */
    @Override
    protected CreatePartakeOrderAggregate doFilterAccount(String userId, Long activityId, LocalDateTime now) {
        ActivityAccountEntity activityAccountEntity = activityRepository.queryActivityAccountByUserId(userId, activityId);
        if (activityAccountEntity == null || activityAccountEntity.totalCountSurplus() <= 0) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_ACCOUNT_QUOTA_NOT_ENOUGH, userId, activityId);
        }
        // 月账户
        YearMonth yearMonth = YearMonth.of(now.getYear(), now.getMonth());
        ActivityAccountMonthEntity activityAccountMonthEntity = activityRepository.queryActivityAccountMonthByUserId(userId, activityId, yearMonth);
        if (activityAccountMonthEntity != null && activityAccountMonthEntity.monthCountSurplus() <= 0) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_ACCOUNT_QUOTA_MONTH_NOT_ENOUGH, userId, activityId, yearMonth);
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
        if (activityAccountDayEntity != null && activityAccountDayEntity.dayCountSurplus() <= 0) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_ACCOUNT_QUOTA_DAY_NOT_ENOUGH, userId, activityId, now.toLocalDate());
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
