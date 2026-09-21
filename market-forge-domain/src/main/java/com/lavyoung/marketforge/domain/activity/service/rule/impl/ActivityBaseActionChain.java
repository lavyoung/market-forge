package com.lavyoung.marketforge.domain.activity.service.rule.impl;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivityCountEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivitySkuEntity;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivityStateVO;
import com.lavyoung.marketforge.domain.activity.service.rule.AbstractActionChain;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 *
 * 活动商品基础信息处理
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/20
 */
@Slf4j
@Component("activity_base_action")
public class ActivityBaseActionChain extends AbstractActionChain {

    @Override
    public boolean action(ActivitySkuEntity activitySku, ActivityEntity activity, ActivityCountEntity activityCount) {
        log.info("活动责任链-商品基础信息处理【有效期&状态】开始-sku={}, activityId={}, activityCountId={}", activitySku.sku(),
                activity.activityId(), activityCount.activityCountId());

        // 活动状态
        if (!ActivityStateVO.OPEN.toString().equals(activity.state())) {
            throw new BusinessException(BusinessResponseCode.ACTIVITY_STATE_ERROR);
        }
        LocalDateTime now = LocalDateTime.now();
        if (activity.beginDateTime().isAfter(now) || activity.endDateTime().isBefore(now)) {
            throw new BusinessException(BusinessResponseCode.ACTIVITY_TIME_RANGE_ERROR);
        }

        if (activitySku.stockCount() <= 0) {
            throw new BusinessException(BusinessResponseCode.ACTIVITY_SKU_STOCK_NOT_ENOUGH);
        }
        return next().action(activitySku, activity, activityCount);
    }
}
