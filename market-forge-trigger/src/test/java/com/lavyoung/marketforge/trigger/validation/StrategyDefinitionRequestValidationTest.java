package com.lavyoung.marketforge.trigger.validation;

import com.lavyoung.marketforge.api.strategy.request.*;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 策略定义请求 Bean Validation 契约测试。
 *
 * <p>固定策略根字段、嵌套奖品、嵌套规则和状态变更的 HTTP 结构边界，
 * 确保明显非法输入不会进入 Application 层。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
class StrategyDefinitionRequestValidationTest {

    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = VALIDATOR_FACTORY.getValidator();

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATOR_FACTORY.close();
    }

    /**
     * Given 完整合法的策略请求，When 执行协议校验，Then 不应产生约束错误。
     */
    @Test
    void shouldAcceptCompleteStrategyDefinitionRequest() {
        // Given
        CreateStrategyDefinitionRequest request = validRequest();

        // When & Then
        assertEquals(0, VALIDATOR.validate(request).size());
    }

    /**
     * Given 奖品标识为空且概率为零，When 校验创建请求，Then 应级联检查嵌套奖品字段。
     */
    @Test
    void shouldCascadeValidationToAwardConfiguration() {
        // Given
        StrategyAwardConfigRequest invalidAward = new StrategyAwardConfigRequest(
                null, "100积分", null, 100, BigDecimal.ZERO, "rule_stock", 1);
        CreateStrategyDefinitionRequest request = new CreateStrategyDefinitionRequest(
                "测试策略", null, List.of(invalidAward), List.of());

        // When
        Set<ConstraintViolation<CreateStrategyDefinitionRequest>> violations = VALIDATOR.validate(request);

        // Then
        assertTrue(violations.stream().anyMatch(violation -> violation.getPropertyPath().toString().contains("awards[0].awardId")));
        assertTrue(violations.stream().anyMatch(violation -> violation.getPropertyPath().toString().contains("awards[0].awardRate")));
    }

    /**
     * Given 规则模型不受支持且没有结构化配置，When 校验创建请求，Then 应级联拒绝嵌套规则。
     */
    @Test
    void shouldCascadeValidationToRuleConfiguration() {
        // Given
        StrategyRuleConfigRequest invalidRule = new StrategyRuleConfigRequest(null, "LOCK", null, null, null, "规则说明");
        CreateStrategyDefinitionRequest request = new CreateStrategyDefinitionRequest(
                "测试策略", null, List.of(), List.of(invalidRule));

        // When
        Set<ConstraintViolation<CreateStrategyDefinitionRequest>> violations = VALIDATOR.validate(request);

        // Then
        assertTrue(violations.stream().anyMatch(violation -> violation.getPropertyPath().toString().contains("rules[0].ruleModel")));
        assertTrue(violations.stream().anyMatch(violation -> violation.getPropertyPath().toString().contains("rules[0].configurationValid")));
    }

    /**
     * Given 权重模型错误携带黑名单配置，When 校验创建请求，Then 应拒绝互相矛盾的规则结构。
     */
    @Test
    void shouldRejectRuleModelAndConfigurationMismatch() {
        // Given
        BlacklistRuleConfigRequest blacklist = new BlacklistRuleConfigRequest(200001L, List.of("user-001"));
        StrategyRuleConfigRequest invalidRule = new StrategyRuleConfigRequest(
                null, "rule_weight", blacklist, null, null, "权重奖池");

        // When
        Set<ConstraintViolation<StrategyRuleConfigRequest>> violations = VALIDATOR.validate(invalidRule);

        // Then
        assertTrue(violations.stream().anyMatch(violation -> violation.getPropertyPath().toString().equals("configurationValid")));
    }

    /**
     * Given 权重档位包含负门槛和空奖品集合，When 校验请求，Then 应级联校验档位字段。
     */
    @Test
    void shouldCascadeValidationToWeightTier() {
        // Given
        WeightTierConfigRequest invalidTier = new WeightTierConfigRequest(-1L, List.of());
        StrategyRuleConfigRequest invalidRule = new StrategyRuleConfigRequest(
                null, "rule_weight", null, new WeightRuleConfigRequest(List.of(invalidTier)), null, "权重奖池");

        // When
        Set<ConstraintViolation<StrategyRuleConfigRequest>> violations = VALIDATOR.validate(invalidRule);

        // Then
        assertTrue(violations.stream().anyMatch(violation -> violation.getPropertyPath().toString().contains("weight.tiers[0].threshold")));
        assertTrue(violations.stream().anyMatch(violation -> violation.getPropertyPath().toString().contains("weight.tiers[0].awardIds")));
    }

    /**
     * Given 次数锁没有绑定奖品，When 校验请求，Then 应拒绝缺少奖品作用域的配置。
     */
    @Test
    void shouldRejectLockRuleWithoutAward() {
        // Given
        StrategyRuleConfigRequest invalidRule = new StrategyRuleConfigRequest(
                null, "rule_lock", null, null, new LockRuleConfigRequest(1), "一次后解锁");

        // When & Then
        assertTrue(VALIDATOR.validate(invalidRule).stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("configurationValid")));
    }

    /**
     * Given 不受支持的目标状态和零版本，When 校验状态请求，Then 应同时拒绝两个字段。
     */
    @Test
    void shouldRejectUnsupportedStateAndVersion() {
        // Given
        ChangeStrategyDefinitionStateRequest request = new ChangeStrategyDefinitionStateRequest("archived", 0);

        // When & Then
        assertEquals(2, VALIDATOR.validate(request).size());
    }

    /**
     * Given 合法筛选条件和分页窗口，When 校验查询请求，Then 请求应通过协议校验。
     */
    @Test
    void shouldAcceptValidSearchRequest() {
        // Given
        StrategyDefinitionSearchRequest request = new StrategyDefinitionSearchRequest(100001L, "九宫格", "active", 1, 20);

        // When & Then
        assertEquals(0, VALIDATOR.validate(request).size());
    }

    /**
     * Given 未知状态、零页码和超大页大小，When 校验查询请求，Then 应拒绝全部非法边界。
     */
    @Test
    void shouldRejectInvalidSearchStateAndPageWindow() {
        // Given
        StrategyDefinitionSearchRequest request = new StrategyDefinitionSearchRequest(null, null, "archived", 0, 101);

        // When
        Set<ConstraintViolation<StrategyDefinitionSearchRequest>> violations = VALIDATOR.validate(request);

        // Then
        assertEquals(3, violations.size());
    }

    private CreateStrategyDefinitionRequest validRequest() {
        StrategyAwardConfigRequest award = new StrategyAwardConfigRequest(
                200001L, "100积分", "自动到账", 100, new BigDecimal("30.00000"), "rule_stock", 1);
        WeightRuleConfigRequest weight = new WeightRuleConfigRequest(
                List.of(new WeightTierConfigRequest(100L, List.of(200001L))));
        StrategyRuleConfigRequest rule = new StrategyRuleConfigRequest(
                null, "rule_weight", null, weight, null, "权重奖池");
        return new CreateStrategyDefinitionRequest("测试策略", "rule_weight", List.of(award), List.of(rule));
    }
}
