package com.lavyoung.marketforge.application.award.metadata;

import com.lavyoung.marketforge.application.award.grant.AwardConfigValidator;
import com.lavyoung.marketforge.application.award.grant.AwardConfigValidatorRegistry;
import com.lavyoung.marketforge.application.award.grant.AwardGrantHandler;
import com.lavyoung.marketforge.application.award.grant.AwardGrantHandlerRegistry;
import com.lavyoung.marketforge.application.award.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link AwardTypeMetadataRegistry} 单元测试。
 *
 * <p>覆盖元数据稳定排序、不可变结果、重复注册，以及处理器、领取模式、
 * 配置协议和字段定义不一致时的启动失败行为。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
class AwardTypeMetadataRegistryTest {

    @Test
    void shouldRegisterSortAndQuerySupportedMetadata() {
        // Given
        AwardTypeMetadata credit = metadata("internal_credit", 1, List.of(automaticClaimMode()), List.of(amountField()));
        AwardTypeMetadata coupon = metadata("coupon", 2, List.of(automaticClaimMode()), List.of(codeField()));
        AwardGrantHandlerRegistry handlerRegistry = handlerRegistry(
                new TestGrantHandler("coupon", Set.of("automatic")),
                new TestGrantHandler("internal_credit", Set.of("automatic")));
        AwardConfigValidatorRegistry validatorRegistry = validatorRegistry(
                new TestConfigValidator("coupon", 2), new TestConfigValidator("internal_credit", 1));

        // When
        AwardTypeMetadataRegistry registry = new AwardTypeMetadataRegistry(
                List.of(() -> credit, () -> coupon), handlerRegistry, validatorRegistry);

        // Then
        assertEquals(List.of("coupon", "internal_credit"),
                registry.listAll().stream().map(AwardTypeMetadata::awardType).toList());
        assertEquals(credit, registry.findByAwardType("internal_credit").orElseThrow());
        assertTrue(registry.findByAwardType("missing").isEmpty());
        assertTrue(registry.findByAwardType(" ").isEmpty());
        assertTrue(registry.findByAwardType(null).isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> registry.listAll().clear());
    }

    @Test
    void shouldRejectMissingAndDuplicateContributors() {
        // Given
        AwardTypeMetadata metadata = metadata("internal_credit", 1, List.of(automaticClaimMode()), List.of(amountField()));
        AwardGrantHandlerRegistry handlerRegistry = handlerRegistry(
                new TestGrantHandler("internal_credit", Set.of("automatic")));
        AwardConfigValidatorRegistry validatorRegistry = validatorRegistry(
                new TestConfigValidator("internal_credit", 1));

        // When & Then
        assertThrows(IllegalStateException.class,
                () -> new AwardTypeMetadataRegistry(List.of(), handlerRegistry, validatorRegistry));
        assertThrows(IllegalStateException.class,
                () -> new AwardTypeMetadataRegistry(List.of(() -> metadata, () -> metadata),
                        handlerRegistry, validatorRegistry));
    }

    @Test
    void shouldRejectMissingGrantHandler() {
        // Given
        AwardTypeMetadata metadata = metadata("internal_credit", 1, List.of(automaticClaimMode()), List.of(amountField()));

        // When & Then
        assertThrows(IllegalStateException.class, () -> new AwardTypeMetadataRegistry(
                List.of(() -> metadata), handlerRegistry(), validatorRegistry(new TestConfigValidator("internal_credit", 1))));
    }

    @Test
    void shouldRejectUnsupportedClaimMode() {
        // Given
        AwardTypeMetadata metadata = metadata(
                "internal_credit", 1, List.of(new AwardClaimModeMetadata("manual", "人工领取")), List.of(amountField()));
        AwardGrantHandlerRegistry handlerRegistry = handlerRegistry(
                new TestGrantHandler("internal_credit", Set.of("automatic")));

        // When & Then
        assertThrows(IllegalStateException.class, () -> new AwardTypeMetadataRegistry(
                List.of(() -> metadata), handlerRegistry, validatorRegistry(new TestConfigValidator("internal_credit", 1))));
    }

    @Test
    void shouldRejectMissingConfigValidator() {
        // Given
        AwardTypeMetadata metadata = metadata("internal_credit", 1, List.of(automaticClaimMode()), List.of(amountField()));
        AwardGrantHandlerRegistry handlerRegistry = handlerRegistry(
                new TestGrantHandler("internal_credit", Set.of("automatic")));

        // When & Then
        assertThrows(IllegalStateException.class, () -> new AwardTypeMetadataRegistry(
                List.of(() -> metadata), handlerRegistry, validatorRegistry()));
    }

    @Test
    void shouldRejectDuplicateConfigFieldNames() {
        // Given
        AwardConfigFieldMetadata duplicateAmount = new AwardConfigFieldMetadata(
                "amount", "备用积分数量", AwardConfigFieldType.INTEGER, false, "不应重复定义的字段", Map.of());
        AwardTypeMetadata metadata = metadata(
                "internal_credit", 1, List.of(automaticClaimMode()), List.of(amountField(), duplicateAmount));
        AwardGrantHandlerRegistry handlerRegistry = handlerRegistry(
                new TestGrantHandler("internal_credit", Set.of("automatic")));
        AwardConfigValidatorRegistry validatorRegistry = validatorRegistry(
                new TestConfigValidator("internal_credit", 1));

        // When & Then
        assertThrows(IllegalStateException.class, () -> new AwardTypeMetadataRegistry(
                List.of(() -> metadata), handlerRegistry, validatorRegistry));
    }

    @Test
    void shouldRejectNullDependenciesAndContributions() {
        // Given
        AwardGrantHandlerRegistry handlerRegistry = handlerRegistry(
                new TestGrantHandler("internal_credit", Set.of("automatic")));
        AwardConfigValidatorRegistry validatorRegistry = validatorRegistry(
                new TestConfigValidator("internal_credit", 1));

        // When & Then
        assertThrows(NullPointerException.class,
                () -> new AwardTypeMetadataRegistry(null, handlerRegistry, validatorRegistry));
        assertThrows(NullPointerException.class,
                () -> new AwardTypeMetadataRegistry(List.of(() -> null), handlerRegistry, validatorRegistry));
        assertThrows(NullPointerException.class,
                () -> new AwardTypeMetadataRegistry(List.of(() -> metadata(
                        "internal_credit", 1, List.of(automaticClaimMode()), List.of(amountField()))), null, validatorRegistry));
        assertThrows(NullPointerException.class,
                () -> new AwardTypeMetadataRegistry(List.of(() -> metadata(
                        "internal_credit", 1, List.of(automaticClaimMode()), List.of(amountField()))), handlerRegistry, null));
    }

    private AwardTypeMetadata metadata(String awardType, int configVersion, List<AwardClaimModeMetadata> claimModes,
                                       List<AwardConfigFieldMetadata> fields) {
        return new AwardTypeMetadata(awardType, awardType, claimModes, configVersion, fields, awardType + " description");
    }

    private AwardConfigFieldMetadata amountField() {
        return new AwardConfigFieldMetadata("amount", "积分数量", AwardConfigFieldType.INTEGER, true,
                "中奖后发放的积分数量", Map.of("minimum", "1"));
    }

    private AwardConfigFieldMetadata codeField() {
        return new AwardConfigFieldMetadata(
                "templateCode", "模板编码", AwardConfigFieldType.TEXT, true, "第三方券模板编码", Map.of());
    }

    private AwardClaimModeMetadata automaticClaimMode() {
        return new AwardClaimModeMetadata("automatic", "自动发放");
    }

    private AwardGrantHandlerRegistry handlerRegistry(AwardGrantHandler... handlers) {
        return new AwardGrantHandlerRegistry(List.of(handlers));
    }

    private AwardConfigValidatorRegistry validatorRegistry(AwardConfigValidator... validators) {
        return new AwardConfigValidatorRegistry(List.of(validators));
    }

    private record TestGrantHandler(String awardType, Set<String> supportedClaimModes) implements AwardGrantHandler {

        @Override
        public boolean supportsClaimMode(String claimMode) {
            return supportedClaimModes.contains(claimMode);
        }

        @Override
        public void grant(GrantAwardCommand command) {
            // 注册表测试不执行奖品发放。
        }
    }

    private record TestConfigValidator(String awardType, int configVersion) implements AwardConfigValidator {

        @Override
        public void validate(String configContent) {
            // 注册表测试不解析奖品配置。
        }
    }
}
