package com.lavyoung.marketforge.trigger.validation;

import com.lavyoung.marketforge.api.strategy.request.ChangeStrategyDefinitionStateRequest;
import com.lavyoung.marketforge.api.strategy.request.CreateStrategyDefinitionRequest;
import com.lavyoung.marketforge.api.strategy.request.StrategyAwardConfigRequest;
import com.lavyoung.marketforge.api.strategy.request.StrategyRuleConfigRequest;
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
     * Given 规则类型和模型格式非法，When 校验创建请求，Then 应级联拒绝嵌套规则。
     */
    @Test
    void shouldCascadeValidationToRuleConfiguration() {
        // Given
        StrategyRuleConfigRequest invalidRule = new StrategyRuleConfigRequest(null, 3, "LOCK", "1", "规则说明");
        CreateStrategyDefinitionRequest request = new CreateStrategyDefinitionRequest(
                "测试策略", null, List.of(), List.of(invalidRule));

        // When
        Set<ConstraintViolation<CreateStrategyDefinitionRequest>> violations = VALIDATOR.validate(request);

        // Then
        assertTrue(violations.stream().anyMatch(violation -> violation.getPropertyPath().toString().contains("rules[0].ruleType")));
        assertTrue(violations.stream().anyMatch(violation -> violation.getPropertyPath().toString().contains("rules[0].ruleModel")));
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

    private CreateStrategyDefinitionRequest validRequest() {
        StrategyAwardConfigRequest award = new StrategyAwardConfigRequest(
                200001L, "100积分", "自动到账", 100, new BigDecimal("30.00000"), "rule_stock", 1);
        StrategyRuleConfigRequest rule = new StrategyRuleConfigRequest(
                null, 1, "rule_weight", "100:200001", "权重奖池");
        return new CreateStrategyDefinitionRequest("测试策略", "rule_weight", List.of(award), List.of(rule));
    }
}
