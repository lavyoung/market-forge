package com.lavyoung.marketforge.infrastructure.persistent.dao.activity;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lavyoung.marketforge.infrastructure.persistent.model.ActivitySkuDefinitionSearchParam;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivitySkuPO;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivitySkuStockAdjustmentPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
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
     * 统计满足运营查询条件的活动 SKU 数量。
     *
     * @param searchParam 查询条件和分页窗口
     * @return 匹配记录总数
     */
    long countActivitySkuDefinitions(ActivitySkuDefinitionSearchParam searchParam);

    /**
     * 分页查询活动 SKU 定义。
     *
     * @param searchParam 查询条件和分页窗口
     * @return 按更新时间和主键倒序排列的 SKU 记录
     */
    List<ActivitySkuPO> queryActivitySkuDefinitionPage(ActivitySkuDefinitionSearchParam searchParam);

    /**
     * 根据商品 SKU 查询活动商品配置。
     *
     * @param sku 商品 SKU
     * @return 活动 SKU 持久化对象；不存在时返回 {@link Optional#empty()}
     */
    Optional<ActivitySkuPO> queryBySku(@Param("sku") Long sku);

    /**
     * 按乐观锁版本更新 SKU 普通配置。
     *
     * <p>该操作禁止修改库存。若活动或次数配置发生变化，SQL 必须同时确认 SKU
     * 尚未产生库存消费。</p>
     *
     * @param definition 修改后的 SKU 持久化对象
     * @return 成功更新返回 {@code 1}，版本冲突、SKU 不存在或业务归属不允许修改时返回 {@code 0}
     */
    int updateConfiguration(@Param("definition") ActivitySkuPO definition);

    /**
     * 根据库存调整快照原子修改库存。
     *
     * @param adjustment 库存调整流水持久化对象
     * @return 成功更新返回 {@code 1}，版本或库存快照冲突时返回 {@code 0}
     */
    int adjustStock(@Param("adjustment") ActivitySkuStockAdjustmentPO adjustment);

    /**
     * 原子扣减指定 SKU 的库存。
     *
     * @param sku             商品 SKU
     * @param activityId      活动标识
     * @param activityCountId 活动次数配置标识
     * @return 成功扣减时为 {@code 1}，库存不足或 SKU 不存在时为 {@code 0}
     */
    int decrementStockCount(@Param("sku") Long sku, @Param("activityId") Long activityId, @Param("activityCountId") Long activityCountId);

    /**
     * 将指定 SKU 的剩余库存原子清零。
     *
     * <p>只清空剩余库存，不修改累计配置的库存总量。</p>
     *
     * @param sku 商品 SKU
     * @return 成功清零返回 {@code 1}；SKU 不存在或库存已经为零时返回 {@code 0}
     */
    int clearStockCountSurplus(@Param("sku") Long sku);
}
