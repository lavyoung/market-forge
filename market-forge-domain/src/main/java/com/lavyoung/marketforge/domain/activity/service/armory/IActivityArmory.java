package com.lavyoung.marketforge.domain.activity.service.armory;

/**
 * 活动装配服务。
 * <p>
 * 负责把活动参与所需的运行时数据预热到缓存，包括活动 SKU 库存、活动详情和次数配置。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/21
 */
public interface IActivityArmory {


    /**
     * 装配单个活动 SKU 的运行时缓存。
     *
     * @param sku 活动 SKU
     * @return 装配成功返回 {@code true}
     * @throws com.lavyoung.marketforge.types.exception.BusinessException SKU 不存在或活动配置缺失时抛出
     */
    boolean assembleActivitySku(Long sku);

    /**
     * 装配指定活动下全部 SKU 的运行时缓存。
     *
     * @param activityId 抽奖活动标识
     * @return 装配成功返回 {@code true}
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 活动不存在或未配置 SKU 时抛出
     */
    boolean assembleActivitySkuByActivityId(Long activityId);
}
