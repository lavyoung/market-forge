package com.lavyoung.marketforge.infrastructure.persistent.assembler;

import com.lavyoung.marketforge.domain.award.model.entity.AwardDefinitionEntity;
import com.lavyoung.marketforge.domain.award.model.vo.AwardClaimModeVO;
import com.lavyoung.marketforge.domain.award.model.vo.AwardConfigSnapshot;
import com.lavyoung.marketforge.domain.award.model.vo.AwardDefinitionStateVO;
import com.lavyoung.marketforge.domain.award.model.vo.AwardTypeVO;
import com.lavyoung.marketforge.infrastructure.persistent.po.AwardPO;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 奖品定义持久化转换器测试。
 *
 * <p>验证运营奖品定义经过数据库模型往返转换后不会丢失业务字段，并确保数据库中的
 * 未知领取模式和生命周期状态无法进入领域层。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
class AwardDefinitionAssemblerTest {

    private final AwardDefinitionAssembler assembler = Mappers.getMapper(AwardDefinitionAssembler.class);

    /**
     * Given 完整的奖品定义，When 转换为 PO 后再还原，Then 所有领域字段应保持一致。
     */
    @Test
    void shouldConvertAwardDefinitionInBothDirections() {
        // Given
        AwardDefinitionEntity expected = new AwardDefinitionEntity(
                1001L,
                "coupon_1001",
                "满100减20优惠券",
                AwardTypeVO.THIRD_PARTY_COUPON,
                AwardClaimModeVO.AUTOMATIC,
                "coupon_center",
                new AwardConfigSnapshot("{\"templateId\":1001}", 2),
                "第三方优惠券",
                AwardDefinitionStateVO.ACTIVE,
                3,
                "operator-001",
                "operator-002",
                LocalDateTime.of(2026, 10, 21, 10, 0),
                LocalDateTime.of(2026, 10, 21, 11, 0)
        );

        // When
        AwardPO po = assembler.toPO(expected);
        AwardDefinitionEntity actual = assembler.toEntity(po);

        // Then
        assertAll(
                () -> assertNull(po.getId()),
                () -> assertEquals("coupon_1001", po.getAwardKey()),
                () -> assertEquals("third_party_coupon", po.getAwardType()),
                () -> assertEquals("automatic", po.getClaimMode()),
                () -> assertEquals("coupon_center", po.getProviderCode()),
                () -> assertEquals("{\"templateId\":1001}", po.getAwardConfig()),
                () -> assertEquals(2, po.getConfigVersion()),
                () -> assertEquals("active", po.getState()),
                () -> assertEquals(expected, actual)
        );
    }

    /**
     * Given 转换输入为空，When 执行双向转换，Then 应保持 MapStruct 的空值透传语义。
     */
    @Test
    void shouldReturnNullWhenSourceIsNull() {
        // When & Then
        assertAll(
                () -> assertNull(assembler.toPO(null)),
                () -> assertNull(assembler.toEntity(null))
        );
    }

    /**
     * Given 多条完整奖品记录，When 批量转换，Then 应复用单条映射并保留输入顺序。
     */
    @Test
    void shouldConvertAwardDefinitionsInBatch() {
        // Given
        AwardPO first = validAwardPO();
        AwardPO second = validAwardPO();
        second.setAwardId(1002L);
        second.setAwardKey("credit_200");

        // When
        List<AwardDefinitionEntity> actual = assembler.toEntities(List.of(first, second));

        // Then
        assertAll(
                () -> assertEquals(2, actual.size()),
                () -> assertEquals(1001L, actual.get(0).awardId()),
                () -> assertEquals("credit_100", actual.get(0).awardCode()),
                () -> assertEquals(1002L, actual.get(1).awardId()),
                () -> assertEquals("credit_200", actual.get(1).awardCode())
        );
    }

    /**
     * Given 数据库包含未知领取模式，When 转换为领域实体，Then 应立即拒绝损坏的配置。
     */
    @Test
    void shouldRejectUnknownClaimMode() {
        // Given
        AwardPO po = validAwardPO();
        po.setClaimMode("scheduled");

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> assembler.toEntity(po));
    }

    /**
     * Given 数据库包含未知生命周期状态，When 转换为领域实体，Then 应立即拒绝损坏的配置。
     */
    @Test
    void shouldRejectUnknownDefinitionState() {
        // Given
        AwardPO po = validAwardPO();
        po.setState("deleted");

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> assembler.toEntity(po));
    }

    private AwardPO validAwardPO() {
        AwardPO po = new AwardPO();
        po.setAwardId(1001L);
        po.setAwardKey("credit_100");
        po.setAwardName("100积分");
        po.setAwardType("internal_credit");
        po.setClaimMode("automatic");
        po.setAwardConfig("{\"amount\":100}");
        po.setConfigVersion(1);
        po.setAwardDesc("抽奖后发放100积分");
        po.setState("draft");
        po.setVersion(1);
        po.setCreatedBy("operator-001");
        po.setUpdatedBy("operator-001");
        po.setCreateTime(LocalDateTime.of(2026, 10, 21, 10, 0));
        po.setUpdateTime(LocalDateTime.of(2026, 10, 21, 10, 0));
        return po;
    }
}
