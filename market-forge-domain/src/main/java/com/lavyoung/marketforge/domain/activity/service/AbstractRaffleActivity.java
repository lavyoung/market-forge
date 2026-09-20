package com.lavyoung.marketforge.domain.activity.service;

import com.lavyoung.marketforge.domain.activity.model.aggregate.CreateOrderAggregate;
import com.lavyoung.marketforge.domain.activity.model.entity.*;
import com.lavyoung.marketforge.domain.activity.repository.IActivityRepository;
import com.lavyoung.marketforge.domain.activity.service.rule.IActionChain;
import com.lavyoung.marketforge.domain.activity.service.rule.factory.DefaultActivityChainFactory;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import com.lavyoung.marketforge.types.model.CommonResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 *
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/18
 */
@Slf4j
public abstract class AbstractRaffleActivity extends RaffleActivitySupport implements IRaffleOrder {

    public AbstractRaffleActivity(IActivityRepository activityRepository, DefaultActivityChainFactory activityChainFactory) {
        super(activityRepository, activityChainFactory);
    }

    @Override
    public ActivityOrderEntity createRaffleActivityOrder(ActivityShopCartEntity activityShopCart) {

        // 通过sku查询活动信息
        ActivitySkuEntity activitySkuEntity = activityRepository.queryActivitySku(activityShopCart.sku());
        // 查询活动信息
        ActivityEntity activityEntity = activityRepository.getActivityEntityByIdActivityId(activitySkuEntity.activityId());
        // 查询次数信息（用户在活动上可参与的次数）
        ActivityCountEntity activityCountEntity = activityRepository.queryRaffleActivityCountByActivityCountId(activitySkuEntity.activityCountId());
        log.info("");

        return ActivityOrderEntity.builder().build();
    }

    @Override
    public String createSkuRechargeOrder(SkuRechargeEntity skuRechargeEntity) {
        // 参数校验
        String userId = skuRechargeEntity.userId();
        Long sku = skuRechargeEntity.sku();
        String outBusinessNo = skuRechargeEntity.outBusinessNo();
        if (sku == null || StringUtils.isAnyBlank(userId, outBusinessNo)) {
            throw new BusinessException(CommonResponseCode.PARAM_INVALID);
        }
        // 查询基本信息
        // 通过sku查询活动信息
        ActivitySkuEntity activitySkuEntity = activityRepository.queryActivitySku(sku);
        // 查询活动信息
        ActivityEntity activityEntity = activityRepository.getActivityEntityByIdActivityId(activitySkuEntity.activityId());
        // 查询次数信息（用户在活动上可参与的次数）
        ActivityCountEntity activityCountEntity = activityRepository.queryRaffleActivityCountByActivityCountId(activitySkuEntity.activityCountId());
        // 活动规则校验
        IActionChain chain = activityChainFactory.openActionChain();
        boolean success = chain.action(activitySkuEntity, activityEntity, activityCountEntity);
        if (!success) {
            // todo
            throw new BusinessException(BusinessResponseCode.POINTS_TRANSACTION_DUPLICATED);
        }
        // 构建订单对象
        CreateOrderAggregate createOrderAggregate = buildOrderAggregate(skuRechargeEntity, activitySkuEntity, activityEntity, activityCountEntity);
        // 保存订单
        doSaveOrder(createOrderAggregate);
        // 返回单号
        return createOrderAggregate.activityOrder().orderId();
    }

    protected abstract void doSaveOrder(CreateOrderAggregate createOrderAggregate);

    protected abstract CreateOrderAggregate buildOrderAggregate(SkuRechargeEntity skuRechargeEntity, ActivitySkuEntity activitySkuEntity, ActivityEntity activityEntity, ActivityCountEntity activityCountEntity);
}
