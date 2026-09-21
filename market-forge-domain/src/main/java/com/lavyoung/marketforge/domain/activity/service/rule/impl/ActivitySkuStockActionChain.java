package com.lavyoung.marketforge.domain.activity.service.rule.impl;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivityCountEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivitySkuEntity;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivitySkuStockKeyVO;
import com.lavyoung.marketforge.domain.activity.repository.IActivityRepository;
import com.lavyoung.marketforge.domain.activity.service.armory.IActivityDispatch;
import com.lavyoung.marketforge.domain.activity.service.rule.AbstractActionChain;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 *
 * 活动商品库存处理
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/20
 */
@Slf4j
@Component("activity_sku_stock_action")
@RequiredArgsConstructor
public class ActivitySkuStockActionChain extends AbstractActionChain {

    private final IActivityDispatch activityDispatch;
    private final IActivityRepository activityRepository;

    @Override
    public boolean action(ActivitySkuEntity activitySku, ActivityEntity activity, ActivityCountEntity activityCount) {
        log.info("活动责任链-商品库存处理【校验&扣减】开始-sku={}, activityId={}, activityCountId={}", activitySku.sku(),
                activity.activityId(), activityCount.activityCountId());
        // 这里判断库存是否允许扣减 真实的库存扣减再后续逻辑发送延迟队列消息 异步消费处理
        boolean status = activityDispatch.subtractionActivitySkuStock(activitySku.sku(), activity.endDateTime());

        if (status) {
            log.info("活动责任链-商品库存处理【有效期、状态、库存(sku)】成功。sku:{} activityId:{}", activitySku.sku(), activity.activityId());
            // 写入延迟队列
            activityRepository.activitySkuStockConsumeSendQueue(ActivitySkuStockKeyVO.builder()
                    .sku(activitySku.sku())
                    .activityId(activity.activityId())
                    .build()
            );
            return true;
        }
        throw new BusinessException(BusinessResponseCode.ACTIVITY_SKU_STOCK_NOT_ENOUGH);
    }
}
