package com.lavyoung.marketforge.infrastructure.persistent.assembler;

import com.lavyoung.marketforge.domain.strategy.model.aggregate.StrategyDefinitionAggregate;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyAwardEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyRuleEntity;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyDefinitionStateVO;
import com.lavyoung.marketforge.infrastructure.persistent.po.StrategyAwardPO;
import com.lavyoung.marketforge.infrastructure.persistent.po.StrategyPO;
import com.lavyoung.marketforge.infrastructure.persistent.po.StrategyRulePO;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 策略定义持久化装配器测试。
 *
 * <p>验证策略根配置、策略奖品和策略规则在领域模型与持久化模型之间转换时，
 * 生命周期、版本、审计字段及子配置不会丢失，并拒绝数据库中的未知状态。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
class StrategyDefinitionAssemblerTest {

    private final StrategyDefinitionAssembler definitionAssembler = Mappers.getMapper(StrategyDefinitionAssembler.class);
    private final StrategyAwardAssembler awardAssembler = Mappers.getMapper(StrategyAwardAssembler.class);
    private final StrategyRuleAssembler ruleAssembler = Mappers.getMapper(StrategyRuleAssembler.class);

    /**
     * Given 完整策略定义聚合，When 转换策略根 PO，Then 应保留根配置并忽略数据库自增主键。
     */
    @Test
    void shouldConvertAggregateToStrategyPO() {
        // Given
        StrategyDefinitionAggregate aggregate = aggregate();

        // When
        StrategyPO actual = definitionAssembler.toPO(aggregate);

        // Then
        assertAll(
                () -> assertNull(actual.getId()),
                () -> assertEquals(100001L, actual.getStrategyId()),
                () -> assertEquals("九宫格抽奖策略", actual.getStrategyDesc()),
                () -> assertEquals("rule_weight,rule_blacklist", actual.getRuleModels()),
                () -> assertEquals("draft", actual.getState()),
                () -> assertEquals(3, actual.getVersion()),
                () -> assertEquals("operator-001", actual.getCreatedBy()),
                () -> assertEquals("operator-002", actual.getUpdatedBy()),
                () -> assertEquals(LocalDateTime.of(2026, 10, 25, 10, 0), actual.getCreateTime()),
                () -> assertEquals(LocalDateTime.of(2026, 10, 25, 11, 0), actual.getUpdateTime())
        );
    }

    /**
     * Given 三张表的完整读取结果，When 组装聚合，Then 根配置与子配置应共同进入一致性边界。
     */
    @Test
    void shouldAssembleCompleteDefinitionFromThreeTables() {
        // Given
        StrategyDefinitionAggregate expected = aggregate();
        StrategyPO strategy = definitionAssembler.toPO(expected);
        List<StrategyAwardEntity> awards = List.copyOf(expected.awards());
        List<StrategyRuleEntity> rules = List.copyOf(expected.rules());

        // When
        StrategyDefinitionAggregate actual = definitionAssembler.toAggregate(strategy, awards, rules);

        // Then
        assertEquals(expected, actual);
    }

    /**
     * Given 策略奖品与规则领域实体，When 批量转换，Then 应完整映射字段并忽略数据库主键。
     */
    @Test
    void shouldConvertStrategyChildrenToPersistentObjects() {
        // Given
        StrategyDefinitionAggregate aggregate = aggregate();

        // When
        List<StrategyAwardPO> awardPOS = awardAssembler.toPOs(aggregate.awards());
        List<StrategyRulePO> rulePOS = ruleAssembler.toPOs(aggregate.rules());

        // Then
        assertAll(
                () -> assertEquals(1, awardPOS.size()),
                () -> assertNull(awardPOS.get(0).getId()),
                () -> assertEquals(100001L, awardPOS.get(0).getStrategyId()),
                () -> assertEquals(101L, awardPOS.get(0).getAwardId()),
                () -> assertEquals(new BigDecimal("0.1000"), awardPOS.get(0).getAwardRate()),
                () -> assertEquals(1, rulePOS.size()),
                () -> assertNull(rulePOS.get(0).getId()),
                () -> assertEquals("rule_weight", rulePOS.get(0).getRuleModel()),
                () -> assertEquals("100:101", rulePOS.get(0).getRuleValue())
        );
    }

    /**
     * Given 数据库包含未知状态，When 组装领域聚合，Then 应拒绝损坏配置进入运营流程。
     */
    @Test
    void shouldRejectUnknownDefinitionState() {
        // Given
        StrategyPO strategy = definitionAssembler.toPO(aggregate());
        strategy.setState("deleted");

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> definitionAssembler.toAggregate(strategy, List.of(), List.of()));
        assertTrue(exception.getMessage().contains("deleted"));
    }

    /**
     * Given 根记录不存在，When 组装聚合，Then 应保持空值透传且不制造不完整聚合。
     */
    @Test
    void shouldReturnNullWhenStrategyRootIsNull() {
        // When & Then
        assertNull(definitionAssembler.toAggregate(null, List.of(), List.of()));
    }

    private StrategyDefinitionAggregate aggregate() {
        LocalDateTime createTime = LocalDateTime.of(2026, 10, 25, 10, 0);
        LocalDateTime updateTime = LocalDateTime.of(2026, 10, 25, 11, 0);
        StrategyAwardEntity award = new StrategyAwardEntity(100001L, 101L, "100积分", "积分自动到账", 1000, 900,
                new BigDecimal("0.1000"), "rule_lock", 1, createTime, updateTime);
        StrategyRuleEntity rule = new StrategyRuleEntity(100001L, null, 1, "rule_weight", "100:101",
                "累计积分达到100时命中", createTime, updateTime);
        return new StrategyDefinitionAggregate(100001L, "九宫格抽奖策略", "rule_weight,rule_blacklist",
                StrategyDefinitionStateVO.DRAFT, 3, "operator-001", "operator-002", createTime, updateTime,
                List.of(award), List.of(rule));
    }
}
