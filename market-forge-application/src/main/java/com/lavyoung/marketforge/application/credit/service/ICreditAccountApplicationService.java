package com.lavyoung.marketforge.application.credit.service;

import com.lavyoung.marketforge.application.credit.model.CreditAccountResult;

/**
 * 用户积分账户查询应用服务。
 *
 * <p>该服务面向积分兑换页，负责查询用户当前可用积分余额。
 * 它只做只读查询编排，不创建积分流水、不调整余额，也不承担兑换扣减。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/12
 */
public interface ICreditAccountApplicationService {

    /**
     * 查询用户积分账户余额。
     *
     * @param userId 用户标识
     * @return 用户积分账户查询结果
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 当用户积分账户不存在时抛出
     */
    CreditAccountResult queryCreditAccount(String userId);
}
