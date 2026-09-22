package com.lavyoung.marketforge.infrastructure.persistent.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lavyoung.marketforge.infrastructure.persistent.po.UserAwardRecordPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

/**
 * 用户中奖记录数据访问接口。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@Mapper
public interface IUserAwardRecordDao extends BaseMapper<UserAwardRecordPO> {

    /**
     * 按抽奖订单标识查询用户中奖记录。
     *
     * @param orderId 抽奖订单标识
     * @return 匹配的用户中奖记录；不存在时返回 {@link Optional#empty()}
     */
    Optional<UserAwardRecordPO> queryByOrderId(@Param("orderId") String orderId);
}
