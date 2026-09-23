package com.lavyoung.marketforge.domain.activity.service.partake;

import com.lavyoung.marketforge.domain.activity.model.aggregate.CreatePartakeOrderAggregate;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityOrderEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivitySkuEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.PartakeRaffleActivityEntity;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivityStateVO;
import com.lavyoung.marketforge.domain.activity.repository.IActivityRepository;
import com.lavyoung.marketforge.domain.activity.service.IRaffleActivityPartakeService;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import com.lavyoung.marketforge.types.model.CommonResponseCode;
import com.lavyoung.marketforge.types.utils.DateUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

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
        Long sku = partakeRaffleActivity.sku();
        if (ObjectUtils.anyNull(userId, activityId, sku)) {
            log.error("创建抽奖参与订单：入参错误 partakeRaffleActivity={}", partakeRaffleActivity);
            throw BusinessException.of(CommonResponseCode.PARAM_INVALID, partakeRaffleActivity);
        }
        LocalDateTime now = DateUtil.now();
        // 查询SKU信息
        ActivitySkuEntity activitySkuEntity = activityRepository.queryActivitySku(sku);
        if (activitySkuEntity == null) {
            log.error("创建抽奖参与订单：SKU商品不存在 userId={} activityId={} sku={}", userId, activityId, sku);
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_SKU_NOT_FOUND, userId, activityId, sku);
        }
        if (!activityId.equals(activitySkuEntity.activityId())) {
            log.error("创建抽奖参与订单：SKU与活动不匹配 userId={} requestActivityId={} sku={} skuActivityId={}",
                    userId, activityId, sku, activitySkuEntity.activityId());
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_SKU_NOT_CONFIGURED, userId, activityId, sku, activitySkuEntity.activityId());
        }
        // 查询活动信息
        ActivityEntity activityEntity = activityRepository.getActivityEntityByIdActivityId(activityId);
        if (activityEntity == null) {
            log.error("创建抽奖参与订单：活动不存在 activityId={}", activityId);
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_NOT_FOUND, activityId);
        }
        if (!ActivityStateVO.OPEN.getCode().equals(activityEntity.state())) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_STATE_ERROR, userId, activityId, activityEntity.state());
        }
        // 活动时间范围
        if (activityEntity.beginDateTime().isAfter(now) || activityEntity.endDateTime().isBefore(now)) {
            log.error("创建抽奖参与订单：活动时间范围无效，date={}", activityEntity);
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_TIME_RANGE_ERROR,
                    userId, activityId, now, activityEntity.beginDateTime(), activityEntity.endDateTime());
        }
        // 查询未使用的活动订单
        ActivityOrderEntity activityOrder = activityRepository.queryNotUsedRaffleOrder(partakeRaffleActivity);
        if (activityOrder != null) {
            log.info("创建抽奖参与订单【已存在未消费】 userId={} activityId={} activityOrder={}", userId, activityId, activityOrder);
            return activityOrder;
        }
        // 不存在则需要创建并返回
        // 1. 查询账户额度过滤 返回账户构建对象
        CreatePartakeOrderAggregate createPartakeOrderAggregate = this.doFilterAccount(userId, activityId, now);
        // 构建订单
        ActivityOrderEntity activityOrderEntity = this.buildActivityRaffleOrder(partakeRaffleActivity, activityEntity, now);
        // 保存订单
        activityRepository.saveCreatePartakeOrderAggregate(createPartakeOrderAggregate, activityOrderEntity);
        return activityOrderEntity;
    }

    /**
     * 构建新的抽奖参与订单。
     * <p>
     * 该扩展点负责创建“消费用户活动账户次数后可用于抽奖”的参与订单实体。实现方应填充用户标识、
     * 活动标识、活动名称快照、抽奖策略、订单号、订单时间和初始订单状态；如果参与订单需要追溯
     * 来源 SKU，也应在此处明确写入，避免后续落库时出现非空字段缺失或权益来源不可追踪。
     *
     * @param partakeRaffleActivity 用户参与活动入参，包含用户、活动和 SKU
     * @param now                   当前业务时间
     * @return 抽奖参与订单实体，用于后续保存并交给抽奖流程消费
     * @throws BusinessException 当活动配置缺失、订单号生成失败或必要字段无法构建时抛出
     */
    protected abstract ActivityOrderEntity buildActivityRaffleOrder(PartakeRaffleActivityEntity partakeRaffleActivity, ActivityEntity activityEntity, LocalDateTime now);

    /**
     * 校验账户额度并组装创建抽奖参与订单所需的账户聚合。
     * <p>
     * 该扩展点负责完成“抽奖次数出账”前的账户检查：校验用户活动总账户、月账户和日账户的剩余额度，
     * 并在当月或当日账户不存在时基于总账户额度构建待创建的账户实体。实现方只应组装聚合和抛出
     * 额度不足异常，实际扣减账户额度和保存参与订单应交由仓储在同一事务内完成。
     *
     * @param userId     用户标识
     * @param activityId 活动标识
     * @param now        当前业务时间
     * @return 创建抽奖参与订单聚合，包含总/月/日账户和账户是否已存在的状态
     * @throws BusinessException 当账户不存在、总/月/日剩余额度不足或账户状态不满足参与条件时抛出
     */
    protected abstract CreatePartakeOrderAggregate doFilterAccount(String userId, Long activityId, LocalDateTime now);
}
