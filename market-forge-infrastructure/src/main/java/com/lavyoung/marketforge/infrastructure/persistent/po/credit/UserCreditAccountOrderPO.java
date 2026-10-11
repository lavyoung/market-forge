package com.lavyoung.marketforge.infrastructure.persistent.po.credit;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lavyoung.marketforge.infrastructure.persistent.po.BasePO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 用户积分账户订单持久化对象。
 * <p>
 * 每一条记录表示一次积分账户变动。{@code bizId} 是业务幂等号，
 * 用于防止 MQ 重复消费或接口重复调用导致重复加积分。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/11
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("user_credit_account_order")
public class UserCreditAccountOrderPO extends BasePO {

    /**
     * 数据库自增主键。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户标识。
     */
    private String userId;

    /**
     * 积分账户订单号。
     */
    private String orderId;

    /**
     * 业务幂等号，例如抽奖订单号。
     */
    private String bizId;

    /**
     * 交易方向编码，例如收入或支出。
     */
    private String tradeType;

    /**
     * 业务类型编码，例如中奖发奖、签到返利或积分兑换。
     */
    private String businessType;

    /**
     * 积分变动数量。
     */
    private Long amount;

    /**
     * 订单状态编码。
     */
    private String state;

    /**
     * 订单时间。
     */
    private LocalDateTime orderTime;
}
