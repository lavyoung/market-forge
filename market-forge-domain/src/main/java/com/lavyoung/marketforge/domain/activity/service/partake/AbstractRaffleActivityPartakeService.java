package com.lavyoung.marketforge.domain.activity.service.partake;

import com.lavyoung.marketforge.domain.activity.model.aggregate.CreatePartakeOrderAggregate;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityOrderEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.PartakeRaffleActivityEntity;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivityStateVO;
import com.lavyoung.marketforge.domain.activity.repository.IActivityRepository;
import com.lavyoung.marketforge.domain.activity.service.IRaffleActivityPartakeService;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import com.lavyoung.marketforge.types.utils.DateUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * 抽奖参与流程模板。
 * <p>
 * 统一处理活动状态、活动时间和未使用订单检查，具体账户过滤和订单构建由子类完成。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/22
 */
@Slf4j
@AllArgsConstructor
public abstract class AbstractRaffleActivityPartakeService implements IRaffleActivityPartakeService {

    protected final IActivityRepository activityRepository;

    /**
     * 创建抽奖参与订单。
     *
     * @param partakeRaffleActivity 用户参与活动入参
     * @return 可用于抽奖的活动订单
     * @throws BusinessException 当活动未开放、时间不合法或账户额度不足时抛出
     */
    @Override
    public ActivityOrderEntity createRaffleOrder(PartakeRaffleActivityEntity partakeRaffleActivity) {
        String userId = partakeRaffleActivity.userId();
        Long activityId = partakeRaffleActivity.activityId();
        LocalDateTime now = DateUtil.now();
        // 查询活动信息
        ActivityEntity activityEntity = activityRepository.getActivityEntityByIdActivityId(activityId);
        if (!ActivityStateVO.OPEN.getCode().equals(activityEntity.state())) {
            throw new BusinessException(BusinessResponseCode.ACTIVITY_STATE_ERROR);
        }
        // 活动时间范围
        if (activityEntity.beginDateTime().isAfter(now) || activityEntity.endDateTime().isBefore(now)) {
            throw new BusinessException(BusinessResponseCode.ACTIVITY_TIME_RANGE_ERROR);
        }
        // 查询未使用的活动订单
        ActivityOrderEntity activityOrder = activityRepository.queryNotUsedRaffleOrder(partakeRaffleActivity);
        if (activityOrder != null) {
            log.info("创建参与活动订单【已存在未消费】 userId={} activityId={} activityOrder={}", userId, activityId, activityOrder);
            return activityOrder;
        }
        // 不存在则需要创建并返回
        // 1. 查询账户额度过滤 返回账户构建对象
        CreatePartakeOrderAggregate createPartakeOrderAggregate = this.doFilterAccount(userId, activityId, now);
        // 构建订单
        ActivityOrderEntity activityOrderEntity = this.buildActivityRaffleOrder(userId, activityId, now);
        // 保存订单
        activityRepository.saveCreatePartakeOrderAggregate(createPartakeOrderAggregate, activityOrderEntity);
        return activityOrderEntity;
    }

    /**
     * 构建新的抽奖参与订单。
     *
     * @param userId     用户标识
     * @param activityId 活动标识
     * @param now        当前业务时间
     * @return 抽奖参与订单实体
     */
    protected abstract ActivityOrderEntity buildActivityRaffleOrder(String userId, Long activityId, LocalDateTime now);

    /**
     * 校验并组装创建抽奖参与订单所需的账户聚合。
     *
     * @param userId     用户标识
     * @param activityId 活动标识
     * @param now        当前业务时间
     * @return 创建抽奖参与订单聚合
     */
    protected abstract CreatePartakeOrderAggregate doFilterAccount(String userId, Long activityId, LocalDateTime now);
}
