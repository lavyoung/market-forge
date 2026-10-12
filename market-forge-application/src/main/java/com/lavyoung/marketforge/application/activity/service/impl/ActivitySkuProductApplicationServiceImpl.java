package com.lavyoung.marketforge.application.activity.service.impl;

import com.lavyoung.marketforge.application.activity.model.ActivitySkuProductResult;
import com.lavyoung.marketforge.application.activity.service.IActivitySkuProductApplicationService;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityCountEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivitySkuEntity;
import com.lavyoung.marketforge.domain.activity.repository.IActivityRepository;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 活动 SKU 商品查询应用服务默认实现。
 *
 * <p>本类负责把活动 SKU 配置和活动次数配置组装为前端商品列表。
 * 查询时先读取活动下的 SKU，再批量读取这些 SKU 关联的次数配置，
 * 避免逐个 SKU 查询次数配置造成 N+1 SQL。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivitySkuProductApplicationServiceImpl implements IActivitySkuProductApplicationService {

    private final IActivityRepository activityRepository;

    @Override
    public List<ActivitySkuProductResult> querySkuProducts(Long activityId) {
        Objects.requireNonNull(activityId, "activityId must not be null");

        List<ActivitySkuEntity> skuProducts = activityRepository.queryActivitySkuListByActivityId(activityId);
        if (skuProducts.isEmpty()) {
            return List.of();
        }

        Map<Long, ActivityCountEntity> countMap = queryActivityCountMap(skuProducts);

        return skuProducts.stream()
                .map(skuProduct -> toResult(skuProduct, countMap))
                .toList();
    }

    /**
     * 批量查询 SKU 关联的次数配置。
     *
     * @param skuProducts 活动 SKU 商品列表
     * @return 活动次数配置标识到次数配置实体的映射
     */
    private Map<Long, ActivityCountEntity> queryActivityCountMap(List<ActivitySkuEntity> skuProducts) {
        List<Long> activityCountIds = skuProducts.stream()
                .map(ActivitySkuEntity::activityCountId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return activityRepository.queryRaffleActivityCountListByActivityIds(activityCountIds)
                .stream()
                .collect(Collectors.toMap(ActivityCountEntity::activityCountId, Function.identity()));
    }

    /**
     * 将 SKU 配置和次数配置组装为商品查询结果。
     *
     * @param skuProduct 活动 SKU 商品配置
     * @param countMap   活动次数配置映射
     * @return 活动 SKU 商品查询结果
     * @throws BusinessException 当次数配置缺失或积分价格无效时抛出
     */
    private ActivitySkuProductResult toResult(ActivitySkuEntity skuProduct, Map<Long, ActivityCountEntity> countMap) {
        ActivityCountEntity countEntity = countMap.get(skuProduct.activityCountId());
        if (countEntity == null) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_COUNT_NOT_FOUND, skuProduct.activityCountId());
        }
        if (skuProduct.creditAmount() == null || skuProduct.creditAmount() <= 0) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_SKU_CREDIT_AMOUNT_INVALID, skuProduct.sku(), skuProduct.creditAmount());
        }
        return new ActivitySkuProductResult(
                skuProduct.sku(),
                skuProduct.activityId(),
                Objects.requireNonNullElse(skuProduct.stockCount(), 0),
                Objects.requireNonNullElse(skuProduct.stockCountSurplus(), 0),
                Objects.requireNonNullElse(countEntity.totalCount(), 0),
                Objects.requireNonNullElse(countEntity.dayCount(), 0),
                Objects.requireNonNullElse(countEntity.monthCount(), 0),
                skuProduct.creditAmount()
        );
    }
}
