package com.lavyoung.marketforge.application.credit.service;

import com.lavyoung.marketforge.application.credit.model.CreditAccountResult;
import com.lavyoung.marketforge.application.credit.service.impl.CreditAccountApplicationServiceImpl;
import com.lavyoung.marketforge.domain.credit.model.entity.CreditAccountEntity;
import com.lavyoung.marketforge.domain.credit.repository.ICreditAccountRepository;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * 用户积分账户查询应用服务测试。
 *
 * <p>测试只关注查询用例的边界：账户存在时返回可用余额，账户不存在时返回明确业务异常，
 * 空用户标识则在进入仓储前快速失败。</p>
 */
@ExtendWith(MockitoExtension.class)
class CreditAccountApplicationServiceImplTest {

    private static final String USER_ID = "user-001";

    @Mock
    private ICreditAccountRepository creditAccountRepository;

    /**
     * Given 用户积分账户存在，When 查询账户，Then 应返回用户标识和可用积分余额。
     */
    @Test
    void shouldReturnCreditAccountBalance() {
        // Given
        when(creditAccountRepository.queryCreditAccountByUserId(USER_ID))
                .thenReturn(Optional.of(new CreditAccountEntity(USER_ID, 1000L, 800L)));
        CreditAccountApplicationServiceImpl service = new CreditAccountApplicationServiceImpl(creditAccountRepository);

        // When
        CreditAccountResult result = service.queryCreditAccount(USER_ID);

        // Then
        assertEquals(USER_ID, result.userId());
        assertEquals(800L, result.availableAmount());
        verify(creditAccountRepository).queryCreditAccountByUserId(USER_ID);
    }

    /**
     * Given 用户积分账户不存在，When 查询账户，Then 应返回账户不存在业务异常。
     */
    @Test
    void shouldRejectWhenCreditAccountDoesNotExist() {
        // Given
        when(creditAccountRepository.queryCreditAccountByUserId(USER_ID)).thenReturn(Optional.empty());
        CreditAccountApplicationServiceImpl service = new CreditAccountApplicationServiceImpl(creditAccountRepository);

        // When
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.queryCreditAccount(USER_ID));

        // Then
        assertEquals(BusinessResponseCode.POINTS_ACCOUNT_NOT_FOUND, exception.getResponseCode());
    }

    /**
     * Given 用户标识为空，When 查询账户，Then 应在访问仓储前拒绝请求。
     */
    @Test
    void shouldRejectNullUserIdBeforeRepositoryCall() {
        // Given
        CreditAccountApplicationServiceImpl service = new CreditAccountApplicationServiceImpl(creditAccountRepository);

        // When & Then
        assertThrows(NullPointerException.class, () -> service.queryCreditAccount(null));
        verifyNoInteractions(creditAccountRepository);
    }
}
