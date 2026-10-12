package com.lavyoung.marketforge.api.credit.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 用户积分账户查询响应。
 *
 * <p>该对象面向前端积分余额展示区域，只暴露用户当前可消费的积分余额。
 * 积分流水、累计获得积分、账户调整原因等内部信息不通过该接口返回，
 * 避免前端依赖领域账户的完整内部结构。</p>
 *
 * <p>其中 {@code availableAmount} 是积分兑换时可实际扣减的余额。
 * 当余额不足时，兑换用例会由积分领域返回对应业务异常；
 * 本对象本身不承担余额校验或扣减职责。</p>
 *
 * @param userId          用户标识
 * @param availableAmount 当前可用积分余额
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/12
 */
@Schema(description = "用户积分账户余额")
public record UserCreditAccountResponse(
        @Schema(description = "用户标识", example = "user-001")
        String userId,
        @Schema(description = "当前可用积分余额", example = "10000")
        Long availableAmount
) implements Serializable {
}
