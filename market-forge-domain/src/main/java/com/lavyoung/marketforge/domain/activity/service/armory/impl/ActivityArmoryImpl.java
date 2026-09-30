package com.lavyoung.marketforge.domain.activity.service.armory.impl;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivityCountEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivitySkuEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivitySkuStockDeductEntity;
import com.lavyoung.marketforge.domain.activity.repository.IActivityRepository;
import com.lavyoung.marketforge.domain.activity.service.armory.IActivityArmory;
import com.lavyoung.marketforge.domain.activity.service.armory.IActivityDispatch;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 活动装配与库存调度实现。
 * <p>
 * 负责把活动 SKU 的基础库存、活动详情和次数配置预热到缓存，并向活动额度下单责任链提供
 * SKU 缓存库存预扣能力。该类不直接修改数据库库存，数据库侧库存同步由后续异步任务完成。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/21
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityArmoryImpl implements IActivityArmory, IActivityDispatch {

    private final IActivityRepository activityRepository;

    /**
     * 装配活动 SKU 运行时缓存。
     * <p>
     * 按 SKU 查询活动商品配置后，将库存计数写入 Redis，并预热活动详情和次数配置，保证后续
     * 额度充值下单时可以优先走缓存路径。SKU 不存在时抛出业务异常，避免静默装配空配置。
     *
     * @param sku 活动 SKU
     * @return 装配成功返回 {@code true}
     * @throws BusinessException 当活动 SKU 不存在时抛出
     */
    @Override
    public boolean assembleActivitySku(Long sku) {
        // 活动 SKU 基础配置 并缓存
        ActivitySkuEntity activitySkuEntity = activityRepository.queryActivitySku(sku);
        if (activitySkuEntity == null) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_SKU_NOT_FOUND, sku);
        }
        // 活动 SKU 库存计数
        activityRepository.cacheActivitySkuStockCount(sku, activitySkuEntity.stockCount());
        // 活动详情预热获取 保存到缓存中
        activityRepository.getActivityEntityByIdActivityId(activitySkuEntity.activityId());
        // 预热活动次数【查询时预热到缓存】
        ActivityCountEntity activityCountEntity = activityRepository.queryRaffleActivityCountByActivityCountId(activitySkuEntity.activityCountId());
        if (activityCountEntity == null) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_COUNT_NOT_FOUND, activitySkuEntity.activityCountId());
        }
        return true;
    }

    @Override
    public boolean assembleActivitySkuByActivityId(Long activityId) {
        // 装配是活动 ID 发起的，所以需要把活动 ID 对应的 SKU 记录一起查询出来”。只按 sku 装配，不方便活动上线时一键预热
        List<ActivitySkuEntity> skuEntities = activityRepository.queryActivitySkuListByActivityId(activityId);
        if (skuEntities.isEmpty()) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_SKU_NOT_CONFIGURED, activityId);
        }
        List<Boolean> booleans = skuEntities.stream().map(sku -> assembleActivitySku(sku.sku())).distinct().toList();
        // 确保都初始化保存完成
        return booleans.stream().allMatch(res -> res);
    }

    /**
     * 预扣活动 SKU 缓存库存。
     * <p>
     * 通过仓储在 Redis 侧执行库存扣减和锁定，用于额度充值订单创建前的快速库存防超卖校验。
     *
     * @param sku         活动 SKU
     * @param endDateTime 活动结束时间，用于计算库存锁过期时间
     * @return 库存预扣结果，包含是否预扣成功和是否进入零库存状态
     */
    @Override
    public ActivitySkuStockDeductEntity subtractionActivitySkuStock(Long sku, LocalDateTime endDateTime) {
        return activityRepository.subtractionActivitySkuStock(sku, endDateTime);
    }
}
