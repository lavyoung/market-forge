package com.lavyoung.marketforge.application.credit.service.impl;

import com.lavyoung.marketforge.application.credit.model.CreditAccountResult;
import com.lavyoung.marketforge.application.credit.service.ICreditAccountApplicationService;
import com.lavyoung.marketforge.domain.credit.model.entity.CreditAccountEntity;
import com.lavyoung.marketforge.domain.credit.repository.ICreditAccountRepository;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;


/**
 * 用户积分账户查询应用服务默认实现。
 *
 * <p>本类负责查询用户积分账户，并把领域账户实体转换为 application 层查询结果。
 * 查询过程是只读操作，不会写入积分流水，不会调整账户余额。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreditAccountApplicationServiceImpl implements ICreditAccountApplicationService {

    private final ICreditAccountRepository creditAccountRepository;

    @Override
    public CreditAccountResult queryCreditAccount(String userId) {
        Objects.requireNonNull(userId, "userId must not be null");

        CreditAccountEntity account = creditAccountRepository.queryCreditAccountByUserId(userId)
                .orElseThrow(() -> BusinessException.of(BusinessResponseCode.POINTS_ACCOUNT_NOT_FOUND, userId));

        return new CreditAccountResult(account.userId(), Objects.requireNonNullElse(account.availableAmount(), 0L));
    }
}
