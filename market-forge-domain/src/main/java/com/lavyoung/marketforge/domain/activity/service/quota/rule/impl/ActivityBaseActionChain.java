package com.lavyoung.marketforge.domain.activity.service.quota.rule.impl;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivityCountEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivitySkuEntity;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivityStateVO;
import com.lavyoung.marketforge.domain.activity.service.quota.rule.AbstractActionChain;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 活动商品基础信息规则节点。
 * <p>
 * 校验活动开放状态、活动有效期和 SKU 基础库存，过滤明显不可创建额度订单的请求。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/20
 */
@Slf4j
@Component("activity_base_action")
public class ActivityBaseActionChain extends AbstractActionChain {

    /**
     * 校验活动基础状态。
     * <p>
     * 依次校验活动开放状态、活动时间窗口以及 SKU 基础库存配置。当前节点校验通过后继续执行后继
     * 节点；如果没有后继节点，则表示责任链已经执行完成。
     *
     * @param activitySku   活动 SKU 配置
     * @param activity      活动配置
     * @param activityCount 活动次数配置
     * @return 当前节点及后继节点全部通过时返回 {@code true}
     * @throws BusinessException 当活动未开放、时间无效或基础库存不足时抛出
     */
    @Override
    public boolean action(ActivitySkuEntity activitySku, ActivityEntity activity, ActivityCountEntity activityCount) {
        log.info("活动责任链-商品基础信息处理【有效期&状态】开始-sku={}, activityId={}, activityCountId={}", activitySku.sku(),
                activity.activityId(), activityCount.activityCountId());

        // 活动状态
        if (!ActivityStateVO.OPEN.getCode().equals(activity.state())) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_STATE_ERROR,
                    activitySku.sku(), activity.activityId(), activity.state());
        }
        LocalDateTime now = LocalDateTime.now();
        if (activity.beginDateTime().isAfter(now) || activity.endDateTime().isBefore(now)) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_TIME_RANGE_ERROR,
                    activitySku.sku(), activity.activityId(), now, activity.beginDateTime(), activity.endDateTime());
        }

        if (activitySku.stockCount() <= 0) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_SKU_STOCK_NOT_ENOUGH,
                    activitySku.sku(), activity.activityId(), activitySku.stockCount());
        }
        if (next() == null) {
            log.debug("活动责任链：到达尾部节点，当前链 name={}", this.getClass().getName());
            return true;
        }
        return next().action(activitySku, activity, activityCount);
    }
}
