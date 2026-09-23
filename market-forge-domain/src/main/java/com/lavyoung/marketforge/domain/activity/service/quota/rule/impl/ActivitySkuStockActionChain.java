package com.lavyoung.marketforge.domain.activity.service.quota.rule.impl;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivityCountEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivitySkuEntity;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivitySkuStockKeyVO;
import com.lavyoung.marketforge.domain.activity.repository.IActivityRepository;
import com.lavyoung.marketforge.domain.activity.service.armory.IActivityDispatch;
import com.lavyoung.marketforge.domain.activity.service.quota.rule.AbstractActionChain;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 活动 SKU 库存规则节点。
 * <p>
 * 在缓存侧预扣 SKU 库存，并投递后续数据库库存同步消息，防止额度订单创建时超卖。
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

    /**
     * 校验并预扣活动 SKU 库存。
     * <p>
     * 先通过活动调度端口在缓存侧预扣库存，预扣成功后发布库存同步消息，用于后续异步扣减数据库
     * 库存；预扣失败则直接抛出库存不足异常，阻断额度订单创建。
     *
     * @param activitySku   活动 SKU 配置
     * @param activity      活动配置
     * @param activityCount 活动次数配置
     * @return 预扣库存并发布同步消息成功时返回 {@code true}
     * @throws BusinessException 当活动 SKU 库存不足或预扣失败时抛出
     */
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
        throw BusinessException.of(BusinessResponseCode.ACTIVITY_SKU_STOCK_NOT_ENOUGH,
                activitySku.sku(), activity.activityId(), activityCount.activityCountId());
    }
}
