package com.lavyoung.marketforge.infrastructure.persistent.dao.activity;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivitySkuPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

/**
 * 抽奖活动 SKU 数据访问接口。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/20
 */
@Mapper
public interface IActivitySkuDao extends BaseMapper<ActivitySkuPO> {

    /**
     * 根据商品 SKU 查询活动商品配置。
     *
     * @param sku 商品 SKU
     * @return 活动 SKU 持久化对象；不存在时返回 {@link Optional#empty()}
     */
    Optional<ActivitySkuPO> queryBySku(@Param("sku") Long sku);

    /**
     * 原子扣减指定 SKU 的库存。
     *
     * @param sku             商品 SKU
     * @param activityId      活动标识
     * @param activityCountId 活动次数配置标识
     * @return 成功扣减时为 {@code 1}，库存不足或 SKU 不存在时为 {@code 0}
     */
    int decrementStockCount(
            @Param("sku") Long sku,
            @Param("activityId") Long activityId,
            @Param("activityCountId") Long activityCountId);
}
