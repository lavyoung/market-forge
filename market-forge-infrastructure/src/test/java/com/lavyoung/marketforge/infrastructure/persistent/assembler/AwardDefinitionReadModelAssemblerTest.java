package com.lavyoung.marketforge.infrastructure.persistent.assembler;

import com.lavyoung.marketforge.application.award.model.AwardDefinitionResult;
import com.lavyoung.marketforge.infrastructure.persistent.po.AwardPO;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 奖品定义运营读模型装配器测试。
 *
 * <p>验证数据库字段能够完整投影到 Application 查询结果，尤其保证历史字段
 * {@code awardKey} 会转换为对外统一使用的 {@code awardCode}。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
class AwardDefinitionReadModelAssemblerTest {

    private final AwardDefinitionReadModelAssembler assembler = Mappers.getMapper(AwardDefinitionReadModelAssembler.class);

    /**
     * Given 完整的奖品持久化对象，When 转换为应用读模型，Then 运营展示字段应完整保留。
     */
    @Test
    void shouldConvertAwardDefinitionToReadModel() {
        // Given
        AwardPO source = awardPO(1001L, "credit_100");

        // When
        AwardDefinitionResult result = assembler.toResult(source);

        // Then
        assertAll(
                () -> assertEquals(1001L, result.awardId()),
                () -> assertEquals("credit_100", result.awardCode()),
                () -> assertEquals("100积分", result.awardName()),
                () -> assertEquals("internal_credit", result.awardType()),
                () -> assertEquals("automatic", result.claimMode()),
                () -> assertNull(result.providerCode()),
                () -> assertEquals("{\"amount\":100}", result.awardConfig()),
                () -> assertEquals(1, result.configVersion()),
                () -> assertEquals("抽奖后自动发放100积分", result.awardDesc()),
                () -> assertEquals("active", result.state()),
                () -> assertEquals(2, result.version()),
                () -> assertEquals("operator-001", result.createdBy()),
                () -> assertEquals("operator-002", result.updatedBy()),
                () -> assertEquals(LocalDateTime.of(2026, 10, 22, 10, 0), result.createTime()),
                () -> assertEquals(LocalDateTime.of(2026, 10, 22, 11, 0), result.updateTime())
        );
    }

    /**
     * Given 多条奖品持久化对象，When 批量转换，Then 结果数量和业务顺序应保持不变。
     */
    @Test
    void shouldConvertAwardDefinitionListInOriginalOrder() {
        // Given
        List<AwardPO> sources = List.of(awardPO(1001L, "credit_100"), awardPO(1002L, "coupon_20"));

        // When
        List<AwardDefinitionResult> results = assembler.toResults(sources);

        // Then
        assertAll(
                () -> assertEquals(2, results.size()),
                () -> assertEquals(1001L, results.get(0).awardId()),
                () -> assertEquals("credit_100", results.get(0).awardCode()),
                () -> assertEquals(1002L, results.get(1).awardId()),
                () -> assertEquals("coupon_20", results.get(1).awardCode())
        );
    }

    private AwardPO awardPO(Long awardId, String awardKey) {
        AwardPO po = new AwardPO();
        po.setAwardId(awardId);
        po.setAwardKey(awardKey);
        po.setAwardName("100积分");
        po.setAwardType("internal_credit");
        po.setClaimMode("automatic");
        po.setAwardConfig("{\"amount\":100}");
        po.setConfigVersion(1);
        po.setAwardDesc("抽奖后自动发放100积分");
        po.setState("active");
        po.setVersion(2);
        po.setCreatedBy("operator-001");
        po.setUpdatedBy("operator-002");
        po.setCreateTime(LocalDateTime.of(2026, 10, 22, 10, 0));
        po.setUpdateTime(LocalDateTime.of(2026, 10, 22, 11, 0));
        return po;
    }
}
