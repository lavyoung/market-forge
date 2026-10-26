package com.lavyoung.marketforge.infrastructure.persistent.dao.activity;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivitySkuStockAdjustmentPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 活动 SKU 库存调整流水数据访问接口。
 *
 * <p>库存调整流水属于只追加数据，不提供更新和删除业务方法。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/26
 */
@Mapper
public interface IActivitySkuStockAdjustmentDao extends BaseMapper<ActivitySkuStockAdjustmentPO> {

    /**
     * 写入库存调整流水。
     *
     * <p>创建时间由数据库默认值生成，不接收调用方时间，避免客户端或应用节点时钟
     * 影响审计流水排序。</p>
     *
     * @param adjustment 库存调整流水
     * @return 成功写入返回 {@code 1}
     */
    int insertAdjustment(@Param("adjustment") ActivitySkuStockAdjustmentPO adjustment);
}
