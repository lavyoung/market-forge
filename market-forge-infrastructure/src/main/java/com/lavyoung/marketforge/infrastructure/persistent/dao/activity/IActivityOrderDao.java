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

    /**
     * 根据用户编号和外部业务幂等号查询活动订单。
     *
     * @param userId        用户编号
     * @param outBusinessNo 外部业务幂等号
     * @return 活动订单；不存在时返回空
     */
    Optional<ActivityOrderPO> queryByUserIdAndOutBusinessNo(@Param("userId") String userId,
                                                            @Param("outBusinessNo") String outBusinessNo);

    /**
     * 按用户、外部业务号和当前状态条件更新活动订单状态。
     *
     * <p>该方法用于支付成功后的发货幂等控制。只有订单仍处于期望状态时才允许更新，
     * 避免 MQ 重复消费导致重复给活动账户充值。</p>
     *
     * @param userId        用户编号
     * @param outBusinessNo 外部业务幂等号
     * @param currentState  当前期望状态
     * @param targetState   目标状态
     * @return 更新成功返回 {@code 1}，订单不存在或状态不匹配返回 {@code 0}
     */
    int updateStateByOutBusinessNo(@Param("userId") String userId,
                                   @Param("outBusinessNo") String outBusinessNo,
                                   @Param("currentState") String currentState,
                                   @Param("targetState") String targetState);
}
