package com.lavyoung.marketforge.domain.strategy.model.vo;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 黑名单规则配置值对象测试。
 *
 * <p>验证黑名单持久化格式能够稳定转换为领域语义，并拒绝空用户、重复用户、
 * 非法兜底奖品等不能安全进入抽奖责任链的配置。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
class BlacklistRuleValueVOTest {

    /**
     * Given 带有可清理空格的合法配置，When 解析并重新序列化，Then 应得到标准配置且支持用户命中判断。
     */
    @Test
    void shouldParseNormalizeAndSerializeBlacklistRuleValue() {
        // Given & When
        BlacklistRuleValueVO ruleValue = BlacklistRuleValueVO.parse(" 200001 : user-001 / user-002 ");

        // Then
        assertAll(
                () -> assertEquals(200001L, ruleValue.fallbackAwardId()),
                () -> assertEquals(List.of("user-001", "user-002"), ruleValue.userIds()),
                () -> assertTrue(ruleValue.contains("user-001")),
                () -> assertFalse(ruleValue.contains("user-003")),
                () -> assertFalse(ruleValue.contains(null)),
                () -> assertEquals("200001:user-001/user-002", ruleValue.toConfigValue())
        );
    }

    /**
     * Given 黑名单用户重复，When 创建值对象，Then 应拒绝语义重复的配置。
     */
    @Test
    void shouldRejectDuplicateBlacklistUsers() {
        assertThrows(IllegalArgumentException.class,
                () -> BlacklistRuleValueVO.parse("200001:user-001/user-001"));
    }

    /**
     * Given 黑名单配置缺少奖品或用户部分，When 解析，Then 应拒绝不完整格式。
     */
    @Test
    void shouldRejectMalformedBlacklistRuleValue() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> BlacklistRuleValueVO.parse(null)),
                () -> assertThrows(IllegalArgumentException.class, () -> BlacklistRuleValueVO.parse("")),
                () -> assertThrows(IllegalArgumentException.class, () -> BlacklistRuleValueVO.parse("200001")),
                () -> assertThrows(IllegalArgumentException.class, () -> BlacklistRuleValueVO.parse("award:user-001")),
                () -> assertThrows(IllegalArgumentException.class, () -> BlacklistRuleValueVO.parse("-1:user-001")),
                () -> assertThrows(IllegalArgumentException.class, () -> BlacklistRuleValueVO.parse("200001:"))
        );
    }
}
