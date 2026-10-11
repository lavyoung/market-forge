package com.lavyoung.marketforge.infrastructure.persistent.po.credit;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lavyoung.marketforge.infrastructure.persistent.po.BasePO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 用户积分账户持久化对象。
 * <p>
 * 保存用户当前积分余额快照。每次积分发放或扣减时，应配合积分账户订单表一起更新，
 * 避免只有余额变化而缺少业务来源记录。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/11
 */

@Getter
@Setter
@NoArgsConstructor
@TableName("user_credit_account")
public class UserCreditAccountPO extends BasePO {
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
     * 累计获得积分总额。
     */
    private Long totalAmount;

    /**
     * 当前可用积分余额。
     */
    private Long availableAmount;

}
