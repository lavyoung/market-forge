package com.lavyoung.marketforge.trigger.validation;

import com.lavyoung.marketforge.api.activity.request.ActivityDefinitionSearchRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link ActivityDefinitionSearchRequest} Bean Validation 契约测试。
 *
 * <p>固定活动查询接口的标识、状态和分页边界，防止非法条件进入 Application 层。</p>
 */
class ActivityDefinitionSearchRequestValidationTest {

    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = VALIDATOR_FACTORY.getValidator();

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATOR_FACTORY.close();
    }

    @Test
    void shouldAcceptRequestWithoutOptionalFilters() {
        // Given
        ActivityDefinitionSearchRequest request = new ActivityDefinitionSearchRequest(null, null, null, 1, 20);

        // When / Then
        assertEquals(0, VALIDATOR.validate(request).size());
    }

    @Test
    void shouldRejectNonPositiveActivityId() {
        // Given
        ActivityDefinitionSearchRequest request = new ActivityDefinitionSearchRequest(0L, null, null, 1, 20);

        // When / Then
        assertEquals(1, VALIDATOR.validate(request).size());
    }

    @Test
    void shouldRejectUnsupportedState() {
        // Given
        ActivityDefinitionSearchRequest request = new ActivityDefinitionSearchRequest(null, null, "active", 1, 20);

        // When / Then
        assertEquals(1, VALIDATOR.validate(request).size());
    }

    @Test
    void shouldRejectPageSizeAboveLimit() {
        // Given
        ActivityDefinitionSearchRequest request = new ActivityDefinitionSearchRequest(null, null, null, 1, 101);

        // When / Then
        assertEquals(1, VALIDATOR.validate(request).size());
    }
}
