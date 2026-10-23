package com.lavyoung.marketforge.domain.award.event;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 奖品履约快照测试。
 *
 * <p>验证中奖事件携带的履约信息具备完整且稳定的结构，避免缺少处理器类型、
 * 配置内容或配置版本的消息进入可靠投递链路后反复重试。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/23
 */
class AwardFulfillmentSnapshotTest {

    /**
     * Given 平台内部积分奖品配置，When 创建履约快照，Then 应完整保留处理器路由和版本信息。
     */
    @Test
    void shouldCreateInternalCreditSnapshot() {
        // Given / When
        AwardFulfillmentSnapshot snapshot = new AwardFulfillmentSnapshot(
                "credit_100", "internal_credit", "automatic", null, "{\"amount\":100}", 1);

        // Then
        assertAll(
                () -> assertEquals("credit_100", snapshot.awardCode()),
                () -> assertEquals("internal_credit", snapshot.awardType()),
                () -> assertEquals("automatic", snapshot.claimMode()),
                () -> assertNull(snapshot.providerCode()),
                () -> assertEquals("{\"amount\":100}", snapshot.configContent()),
                () -> assertEquals(1, snapshot.configVersion())
        );
    }

    /**
     * Given 第三方自动履约奖品，When 创建履约快照，Then 应保留供应商编码供处理器选择适配器。
     */
    @Test
    void shouldPreserveProviderCode() {
        // Given / When
        AwardFulfillmentSnapshot snapshot = new AwardFulfillmentSnapshot(
                "course_coupon_50", "third_party_coupon", "automatic", "course_center",
                "{\"templateCode\":\"course_discount_50\"}", 1);

        // Then
        assertEquals("course_center", snapshot.providerCode());
    }

    /**
     * Given 必填文本字段为空，When 创建履约快照，Then 应立即拒绝不完整的可靠消息数据。
     */
    @Test
    void shouldRejectMissingRequiredText() {
        // Given / When / Then
        assertAll(
                () -> assertThrows(NullPointerException.class,
                        () -> new AwardFulfillmentSnapshot(null, "internal_credit", "automatic", null, "{}", 1)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new AwardFulfillmentSnapshot("credit_100", " ", "automatic", null, "{}", 1)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new AwardFulfillmentSnapshot("credit_100", "internal_credit", " ", null, "{}", 1)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new AwardFulfillmentSnapshot("credit_100", "internal_credit", "automatic", null, " ", 1))
        );
    }

    /**
     * Given 供应商编码为空白，When 创建履约快照，Then 应拒绝含糊的供应商配置。
     */
    @Test
    void shouldRejectBlankProviderCode() {
        // Given / When / Then
        assertThrows(IllegalArgumentException.class,
                () -> new AwardFulfillmentSnapshot(
                        "coupon_100", "third_party_coupon", "automatic", " ", "{}", 1));
    }

    /**
     * Given 配置版本小于一，When 创建履约快照，Then 应拒绝无法选择解析规则的消息。
     */
    @Test
    void shouldRejectInvalidConfigVersion() {
        // Given / When / Then
        assertThrows(IllegalArgumentException.class,
                () -> new AwardFulfillmentSnapshot(
                        "credit_100", "internal_credit", "automatic", null, "{\"amount\":100}", 0));
    }
}
