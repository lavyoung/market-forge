package com.lavyoung.marketforge.infrastructure.persistent.dao.credit;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lavyoung.marketforge.infrastructure.persistent.po.credit.UserCreditAccountOrderPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户积分账户订单数据访问接口。
 * <p>
 * 积分账户订单承担幂等控制和流水追踪职责。业务侧可以通过 {@code bizId}
 * 判断某个抽奖订单或返利订单是否已经处理过。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/11
 */
@Mapper
public interface IUserCreditAccountOrderDao extends BaseMapper<UserCreditAccountOrderPO> {

    /**
     * 查询业务幂等号是否已经处理。
     *
     * @param bizId 业务幂等号
     * @return 已存在返回 {@code 1}，不存在返回 {@code 0}
     */
    @Select("""
            SELECT COUNT(1)
            FROM user_credit_account_order
            WHERE biz_id = #{bizId}
            LIMIT 1
            """)
    int existsByBizId(@Param("bizId") String bizId);
}
