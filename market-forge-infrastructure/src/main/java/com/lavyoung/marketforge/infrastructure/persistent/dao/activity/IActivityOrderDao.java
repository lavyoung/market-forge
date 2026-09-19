package com.lavyoung.marketforge.infrastructure.persistent.dao.activity;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivityOrderPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

/**
 *
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/18
 */
@Mapper
public interface IActivityOrderDao extends BaseMapper<ActivityOrderPO> {

    /**
     * 根据业务订单号查询活动订单。
     *
     * @param orderId 业务订单号
     * @return 活动订单持久化对象；不存在时返回 {@link Optional#empty()}
     */
    Optional<ActivityOrderPO> queryByOrderId(@Param("orderId") String orderId);
}
