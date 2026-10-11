package com.lavyoung.marketforge.domain.credit.service;

import com.lavyoung.marketforge.domain.credit.model.entity.CreditAdjustEntity;
import com.lavyoung.marketforge.domain.credit.model.vo.CreditAccountBusinessTypeVO;
import com.lavyoung.marketforge.domain.credit.model.vo.CreditAccountTradeTypeVO;

/**
 * 用户积分账户领域服务。
 * <p>
 * 负责校验积分入账参数并构建积分账户调整聚合。该服务不直接访问数据库，
 * 持久化细节由积分账户仓储端口完成。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/11
 */
public interface ICreditAccountService {

    /**
     * 给用户发放积分。
     *
     * @param userId       用户标识
     * @param bizId        业务幂等号，例如抽奖订单号
     * @param amount       发放积分数量，必须为正数
     * @param businessType 积分来源
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 参数非法或积分入账失败时抛出
     */
    default void grantCredit(String userId, String bizId, Long amount, CreditAccountBusinessTypeVO businessType) {
        adjustCredit(new CreditAdjustEntity(userId, bizId, amount, CreditAccountTradeTypeVO.INCOME, businessType));
    }

    /**
     * 调整用户积分账户额度。
     *
     * <p>该方法是积分账户领域的通用调额入口。调用方需要明确传入交易方向：
     * 收入场景会增加账户累计积分和可用积分；支出场景会扣减可用积分，并要求余额充足。</p>
     *
     * @param creditAdjust 额度调整
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 参数非法、余额不足或调额失败时抛出
     */
    void adjustCredit(CreditAdjustEntity creditAdjust);
}
