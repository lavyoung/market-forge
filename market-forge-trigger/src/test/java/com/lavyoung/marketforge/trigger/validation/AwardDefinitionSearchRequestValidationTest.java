package com.lavyoung.marketforge.trigger.validation;

import com.lavyoung.marketforge.api.award.request.AwardDefinitionSearchRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 奖品定义分页查询请求协议校验测试。
 *
 * <p>验证 HTTP 入站参数在进入 Application 层前能够拒绝非法编码、未知状态和
 * 无边界分页请求，同时允许运营页面组合合法的可选过滤条件。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
class AwardDefinitionSearchRequestValidationTest {

    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = VALIDATOR_FACTORY.getValidator();

    /**
     * 释放 Bean Validation 工厂持有的资源。
     */
    @AfterAll
    static void closeValidatorFactory() {
        VALIDATOR_FACTORY.close();
    }

    /**
     * Given 合法过滤条件和分页参数，When 执行协议校验，Then 不应产生约束错误。
     */
    @Test
    void shouldAcceptValidSearchRequest() {
        // Given
        AwardDefinitionSearchRequest request = new AwardDefinitionSearchRequest("credit_100", "积分", "internal_credit", "active", 1, 20);

        // When
        Set<ConstraintViolation<AwardDefinitionSearchRequest>> violations = VALIDATOR.validate(request);

        // Then
        assertTrue(violations.isEmpty());
    }

    /**
     * Given 不符合命名规则的奖品编码，When 执行协议校验，Then 应拒绝该查询请求。
     */
    @Test
    void shouldRejectInvalidAwardCode() {
        // Given
        AwardDefinitionSearchRequest request = new AwardDefinitionSearchRequest("Credit 100", null, null, null, 1, 20);

        // When
        Set<ConstraintViolation<AwardDefinitionSearchRequest>> violations = VALIDATOR.validate(request);

        // Then
        assertEquals(Set.of("奖品业务编码格式不合法"), messages(violations));
    }

    /**
     * Given 未知生命周期状态，When 执行协议校验，Then 应在进入应用层之前拒绝请求。
     */
    @Test
    void shouldRejectUnknownState() {
        // Given
        AwardDefinitionSearchRequest request = new AwardDefinitionSearchRequest(null, null, null, "deleted", 1, 20);

        // When
        Set<ConstraintViolation<AwardDefinitionSearchRequest>> violations = VALIDATOR.validate(request);

        // Then
        assertEquals(Set.of("奖品生命周期状态不合法"), messages(violations));
    }

    /**
     * Given 非法页码和超限页大小，When 执行协议校验，Then 应同时报告两个分页错误。
     */
    @Test
    void shouldRejectInvalidPagination() {
        // Given
        AwardDefinitionSearchRequest request = new AwardDefinitionSearchRequest(null, null, null, null, 0, 101);

        // When
        Set<ConstraintViolation<AwardDefinitionSearchRequest>> violations = VALIDATOR.validate(request);

        // Then
        assertEquals(Set.of("页码不能小于 1", "每页记录数不能超过 100"), messages(violations));
    }

    private Set<String> messages(Set<ConstraintViolation<AwardDefinitionSearchRequest>> violations) {
        return violations.stream().map(ConstraintViolation::getMessage).collect(java.util.stream.Collectors.toSet());
    }
}
