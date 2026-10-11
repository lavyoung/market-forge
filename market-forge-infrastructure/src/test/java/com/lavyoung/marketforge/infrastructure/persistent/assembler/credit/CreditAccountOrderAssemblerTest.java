package com.lavyoung.marketforge.infrastructure.persistent.assembler.credit;

import com.lavyoung.marketforge.domain.credit.model.entity.CreditAccountOrderEntity;
import com.lavyoung.marketforge.domain.credit.model.vo.CreditAccountBusinessTypeVO;
import com.lavyoung.marketforge.domain.credit.model.vo.CreditAccountOrderStateVO;
import com.lavyoung.marketforge.domain.credit.model.vo.CreditAccountTradeTypeVO;
import com.lavyoung.marketforge.infrastructure.persistent.po.credit.UserCreditAccountOrderPO;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 积分账户订单转换器测试。
 * <p>
 * 验证领域枚举写入持久化对象时使用稳定编码，而不是枚举常量名。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
class CreditAccountOrderAssemblerTest {

    /**
     * Given 积分账户订单领域实体，When 转换为 PO，Then 应保留业务字段并把枚举转为编码。
     */
    @Test
    void shouldConvertOrderEntityToPoWithEnumCodes() {
        // Given
        CreditAccountOrderAssembler assembler = Mappers.getMapper(CreditAccountOrderAssembler.class);
        CreditAccountOrderEntity source = new CreditAccountOrderEntity(
                "user-001",
                "credit-order-001",
                "raffle-order-001",
                CreditAccountTradeTypeVO.INCOME,
                CreditAccountBusinessTypeVO.AWARD_GRANT,
                10L,
                CreditAccountOrderStateVO.COMPLETED,
                LocalDateTime.of(2026, 10, 11, 20, 0)
        );

        // When
        UserCreditAccountOrderPO actual = assembler.toPO(source);

        // Then
        assertAll(
                () -> assertNull(actual.getId()),
                () -> assertNull(actual.getCreateTime()),
                () -> assertNull(actual.getUpdateTime()),
                () -> assertEquals(source.userId(), actual.getUserId()),
                () -> assertEquals(source.orderId(), actual.getOrderId()),
                () -> assertEquals(source.bizId(), actual.getBizId()),
                () -> assertEquals("income", actual.getTradeType()),
                () -> assertEquals("award_grant", actual.getBusinessType()),
                () -> assertEquals(source.amount(), actual.getAmount()),
                () -> assertEquals("completed", actual.getState()),
                () -> assertEquals(source.orderTime(), actual.getOrderTime())
        );
    }
}
