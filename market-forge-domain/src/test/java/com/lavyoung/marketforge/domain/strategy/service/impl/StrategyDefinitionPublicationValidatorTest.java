package com.lavyoung.marketforge.domain.strategy.service.impl;

import com.lavyoung.marketforge.domain.award.model.entity.AwardDefinitionEntity;
import com.lavyoung.marketforge.domain.award.model.vo.AwardClaimModeVO;
import com.lavyoung.marketforge.domain.award.model.vo.AwardConfigSnapshot;
import com.lavyoung.marketforge.domain.award.model.vo.AwardDefinitionStateVO;
import com.lavyoung.marketforge.domain.award.model.vo.AwardTypeVO;
import com.lavyoung.marketforge.domain.award.repository.IAwardDefinitionRepository;
import com.lavyoung.marketforge.domain.strategy.model.aggregate.StrategyDefinitionAggregate;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyAwardEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyRuleEntity;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyDefinitionStateVO;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.ProbabilitySpaceFactory;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 策略定义发布就绪校验器测试。
 *
 * <p>验证完整奖池能够发布，并覆盖空奖池、库存、排序、概率、规则归属、权重引用、
 * 奖品缺失和奖品未启用等关键失败边界。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class StrategyDefinitionPublicationValidatorTest {

    private static final Long STRATEGY_ID = 100001L;
    private static final Long FIRST_AWARD_ID = 200001L;
    private static final Long SECOND_AWARD_ID = 200002L;

    @Mock
    private IAwardDefinitionRepository awardDefinitionRepository;

    private StrategyDefinitionPublicationValidator validator;

    @BeforeEach
    void setUp() {
        validator = new StrategyDefinitionPublicationValidator(awardDefinitionRepository, new ProbabilitySpaceFactory());
    }

    /**
     * Given 结构完整且引用奖品全部启用，When 校验发布条件，Then 应正常通过并只执行一次批量查询。
     */
    @Test
    void shouldAcceptPublishableStrategyDefinition() {
        // Given
        StrategyDefinitionAggregate aggregate = validAggregate();
        when(awardDefinitionRepository.queryByAwardIds(List.of(FIRST_AWARD_ID, SECOND_AWARD_ID)))
                .thenReturn(List.of(awardDefinition(FIRST_AWARD_ID, AwardDefinitionStateVO.ACTIVE),
                        awardDefinition(SECOND_AWARD_ID, AwardDefinitionStateVO.ACTIVE)));

        // When & Then
        assertDoesNotThrow(() -> validator.validate(aggregate));
        verify(awardDefinitionRepository).queryByAwardIds(List.of(FIRST_AWARD_ID, SECOND_AWARD_ID));
    }

    /**
     * Given 策略没有配置奖品，When 校验发布条件，Then 应在访问数据库前拒绝空奖池。
     */
    @Test
    void shouldRejectEmptyAwardPoolBeforeQueryingDefinitions() {
        // Given
        StrategyDefinitionAggregate aggregate = aggregate(List.of(), List.of(), null);

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> validator.validate(aggregate));

        // Then
        assertEquals(BusinessResponseCode.STRATEGY_AWARD_NOT_CONFIGURED, exception.getResponseCode());
        verifyNoInteractions(awardDefinitionRepository);
    }

    /**
     * Given 两个奖品使用相同排序，When 校验发布条件，Then 应拒绝不确定的展示顺序。
     */
    @Test
    void shouldRejectDuplicatedAwardSort() {
        // Given
        List<StrategyAwardEntity> awards = List.of(
                award(FIRST_AWARD_ID, 1, new BigDecimal("30"), "rule_stock"),
                award(SECOND_AWARD_ID, 1, new BigDecimal("70"), "rule_lock/rule_stock")
        );

        // When
        BusinessException exception = assertThrows(BusinessException.class,
                () -> validator.validate(aggregate(awards, validRules(), "rule_weight")));

        // Then
        assertEquals(BusinessResponseCode.STRATEGY_CONFIG_INVALID, exception.getResponseCode());
        verifyNoInteractions(awardDefinitionRepository);
    }

    /**
     * Given 奖品权重为零，When 校验发布条件，Then 应复用概率空间工厂拒绝非法权重。
     */
    @Test
    void shouldRejectInvalidProbabilityWeight() {
        // Given
        List<StrategyAwardEntity> awards = List.of(
                award(FIRST_AWARD_ID, 1, BigDecimal.ZERO, "rule_stock"),
                award(SECOND_AWARD_ID, 2, BigDecimal.ONE, "rule_lock/rule_stock")
        );

        // When
        BusinessException exception = assertThrows(BusinessException.class,
                () -> validator.validate(aggregate(awards, validRules(), "rule_weight")));

        // Then
        assertEquals(BusinessResponseCode.STRATEGY_RATE_INVALID, exception.getResponseCode());
        verifyNoInteractions(awardDefinitionRepository);
    }

    /**
     * Given 奖品级规则引用奖池外奖品，When 校验发布条件，Then 应拒绝错误归属。
     */
    @Test
    void shouldRejectAwardRuleReferencingUnknownAward() {
        // Given
        StrategyRuleEntity invalidRule = rule(999999L, 2, "rule_lock", "1");

        // When
        BusinessException exception = assertThrows(BusinessException.class,
                () -> validator.validate(aggregate(validAwards(), List.of(invalidRule), null)));

        // Then
        assertEquals(BusinessResponseCode.STRATEGY_CONFIG_INVALID, exception.getResponseCode());
        verifyNoInteractions(awardDefinitionRepository);
    }

    /**
     * Given 权重档位引用奖池外奖品，When 校验发布条件，Then 应拒绝会被运行时静默过滤的配置。
     */
    @Test
    void shouldRejectWeightRuleReferencingUnknownAward() {
        // Given
        StrategyRuleEntity invalidWeightRule = rule(null, 1, "rule_weight", "100:200001/999999");

        // When
        BusinessException exception = assertThrows(BusinessException.class,
                () -> validator.validate(aggregate(validAwards(), List.of(invalidWeightRule), "rule_weight")));

        // Then
        assertEquals(BusinessResponseCode.STRATEGY_CONFIG_INVALID, exception.getResponseCode());
        verifyNoInteractions(awardDefinitionRepository);
    }

    /**
     * Given 批量查询没有返回全部奖品，When 校验发布条件，Then 应报告具体缺失奖品。
     */
    @Test
    void shouldRejectMissingAwardDefinition() {
        // Given
        StrategyDefinitionAggregate aggregate = validAggregate();
        when(awardDefinitionRepository.queryByAwardIds(List.of(FIRST_AWARD_ID, SECOND_AWARD_ID)))
                .thenReturn(List.of(awardDefinition(FIRST_AWARD_ID, AwardDefinitionStateVO.ACTIVE)));

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> validator.validate(aggregate));

        // Then
        assertEquals(BusinessResponseCode.AWARD_NOT_FOUND, exception.getResponseCode());
    }

    /**
     * Given 策略引用了草稿奖品，When 校验发布条件，Then 应拒绝不可履约的奖品定义。
     */
    @Test
    void shouldRejectInactiveAwardDefinition() {
        // Given
        StrategyDefinitionAggregate aggregate = validAggregate();
        when(awardDefinitionRepository.queryByAwardIds(List.of(FIRST_AWARD_ID, SECOND_AWARD_ID)))
                .thenReturn(List.of(awardDefinition(FIRST_AWARD_ID, AwardDefinitionStateVO.ACTIVE),
                        awardDefinition(SECOND_AWARD_ID, AwardDefinitionStateVO.DRAFT)));

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> validator.validate(aggregate));

        // Then
        assertEquals(BusinessResponseCode.AWARD_NOT_AVAILABLE, exception.getResponseCode());
    }

    /**
     * Given 校验器收到空聚合，When 执行校验，Then 应立即拒绝且不访问仓储。
     */
    @Test
    void shouldRejectNullAggregate() {
        // When & Then
        assertThrows(NullPointerException.class, () -> validator.validate(null));
        verify(awardDefinitionRepository, never()).queryByAwardIds(org.mockito.ArgumentMatchers.anyList());
    }

    private StrategyDefinitionAggregate validAggregate() {
        return aggregate(validAwards(), validRules(), "rule_weight");
    }

    private StrategyDefinitionAggregate aggregate(List<StrategyAwardEntity> awards, List<StrategyRuleEntity> rules, String ruleModels) {
        return new StrategyDefinitionAggregate(STRATEGY_ID, "发布校验策略", ruleModels,
                StrategyDefinitionStateVO.DRAFT, 1, "operator-001", "operator-001", null, null, awards, rules);
    }

    private List<StrategyAwardEntity> validAwards() {
        return List.of(
                award(FIRST_AWARD_ID, 1, new BigDecimal("30"), "rule_stock"),
                award(SECOND_AWARD_ID, 2, new BigDecimal("70"), "rule_lock/rule_stock")
        );
    }

    private List<StrategyRuleEntity> validRules() {
        return List.of(
                rule(null, 1, "rule_weight", "100:200001/200002"),
                rule(SECOND_AWARD_ID, 2, "rule_lock", "1")
        );
    }

    private StrategyAwardEntity award(Long awardId, int sort, BigDecimal rate, String ruleModels) {
        return new StrategyAwardEntity(STRATEGY_ID, awardId, "测试奖品" + awardId, null, 100, 100,
                rate, ruleModels, sort, null, null);
    }

    private StrategyRuleEntity rule(Long awardId, int ruleType, String ruleModel, String ruleValue) {
        return new StrategyRuleEntity(STRATEGY_ID, awardId, ruleType, ruleModel, ruleValue,
                "发布校验规则", null, null);
    }

    private AwardDefinitionEntity awardDefinition(Long awardId, AwardDefinitionStateVO state) {
        return new AwardDefinitionEntity(awardId, "award_" + awardId, "测试奖品", new AwardTypeVO("internal_credit"),
                AwardClaimModeVO.AUTOMATIC, null, new AwardConfigSnapshot("{\"amount\":100}", 1),
                "测试奖品定义", state, 1, "operator-001", "operator-001", null, null);
    }
}
