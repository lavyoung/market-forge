package com.lavyoung.marketforge.domain.credit.service;

import com.lavyoung.marketforge.domain.credit.model.vo.CreditAccountBusinessTypeVO;

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
    void grantCredit(String userId, String bizId, Long amount, CreditAccountBusinessTypeVO businessType);
}
