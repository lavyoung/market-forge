package com.lavyoung.marketforge.infrastructure.persistent.dao.credit;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lavyoung.marketforge.infrastructure.persistent.po.credit.UserCreditAccountPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;


/**
 * 用户积分账户数据访问接口。
 * <p>
 * 提供积分账户余额的原子累加能力，避免并发发奖时发生余额覆盖。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/11
 */
@Mapper
public interface IUserCreditAccountDao extends BaseMapper<UserCreditAccountPO> {

    /**
     * 原子增加用户积分账户余额。
     *
     * @param userId 用户标识
     * @param amount 本次增加的积分数量
     * @return 成功更新时返回 {@code 1}，账户不存在时返回 {@code 0}
     */
    @Update("""
            UPDATE user_credit_account
            SET total_amount = total_amount + #{amount},
                available_amount = available_amount + #{amount},
                update_time = NOW()
            WHERE user_id = #{userId}
            """)
    int incrementCreditAmount(@Param("userId") String userId, @Param("amount") Long amount);

    /**
     * 原子扣减用户积分账户可用余额。
     *
     * <p>支出场景必须在 SQL 层判断余额是否充足，避免并发扣减时把账户扣成负数。
     * 该方法只扣减可用余额，不减少累计获得积分总额。</p>
     *
     * @param userId 用户标识
     * @param amount 本次扣减的积分数量
     * @return 成功扣减时返回 {@code 1}；账户不存在或余额不足时返回 {@code 0}
     */
    @Update("""
            UPDATE user_credit_account
            SET available_amount = available_amount - #{amount},
                update_time = NOW()
            WHERE user_id = #{userId}
              AND available_amount >= #{amount}
            """)
    int decrementCreditAmount(@Param("userId") String userId, @Param("amount") Long amount);
}
