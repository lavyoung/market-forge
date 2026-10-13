package com.lavyoung.marketforge.app.config.dcc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ActivityAccessPolicy} 单元测试。
 *
 * <p>本测试类用于验证活动访问控制策略是否正确消费 DCC 动态配置中的黑名单、
 * 白名单，并按照“黑名单优先、白名单为空则放行、白名单非空则只放行白名单用户”
 * 的业务规则完成判断。</p>
 *
 * <p>测试覆盖以下核心场景：</p>
 *
 * <ul>
 *     <li>黑白名单均为空时，普通用户允许参与活动；</li>
 *     <li>白名单存在时，仅白名单用户允许参与活动；</li>
 *     <li>黑名单存在时，黑名单用户被拒绝参与活动；</li>
 *     <li>同一用户同时命中黑名单和白名单时，黑名单优先拒绝；</li>
 *     <li>DCC 配置中的空白字符和空元素会被清洗；</li>
 *     <li>用户 ID 为空或空白时抛出参数异常。</li>
 * </ul>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/13
 */
class ActivityAccessPolicyTest {

    private ActivityDccConfig activityDccConfig;

    private ActivityAccessPolicy activityAccessPolicy;

    /**
     * 初始化每个测试用例独享的 DCC 配置对象和访问策略对象。
     *
     * <p>这里不启动 Spring 容器，直接构造对象，是为了让测试聚焦在
     * {@link ActivityAccessPolicy} 的业务判断规则上，避免配置装配、Zookeeper
     * 连接等外部因素影响单元测试稳定性。</p>
     */
    @BeforeEach
    void setUp() {
        activityDccConfig = new ActivityDccConfig();
        activityAccessPolicy = new ActivityAccessPolicy(activityDccConfig);
    }

    /**
     * 验证黑白名单均为空时，普通用户允许参与活动。
     *
     * <p>目的：保证默认配置不会误伤正常活动流量。动态配置刚接入时，
     * 如果运营没有配置任何名单，系统应该保持原有放行行为。</p>
     */
    @Test
    void shouldAllowUserWhenBothListsAreEmpty() {
        // Given
        activityDccConfig.setActivityBlacklist("");
        activityDccConfig.setActivityWhitelist("");

        // When
        boolean allowed = activityAccessPolicy.isAllowed("user-001");

        // Then
        assertTrue(allowed);
    }

    /**
     * 验证白名单存在时，命中白名单的用户允许参与活动。
     *
     * <p>目的：支撑灰度放量场景。运营可以只允许一批指定用户参与活动，
     * 用于小范围验证活动链路是否稳定。</p>
     */
    @Test
    void shouldAllowUserWhenUserInWhitelist() {
        // Given
        activityDccConfig.setActivityBlacklist("");
        activityDccConfig.setActivityWhitelist("user-001,user-002");

        // When
        boolean allowed = activityAccessPolicy.isAllowed("user-001");

        // Then
        assertTrue(allowed);
    }

    /**
     * 验证白名单存在时，未命中白名单的用户不允许参与活动。
     *
     * <p>目的：保证白名单配置一旦启用，就具备收口活动流量的能力，
     * 避免非灰度用户提前进入活动。</p>
     */
    @Test
    void shouldRejectUserWhenWhitelistExistsAndUserNotInWhitelist() {
        // Given
        activityDccConfig.setActivityBlacklist("");
        activityDccConfig.setActivityWhitelist("user-001,user-002");

        // When
        boolean allowed = activityAccessPolicy.isAllowed("user-003");

        // Then
        assertFalse(allowed);
    }

    /**
     * 验证黑名单存在时，命中黑名单的用户不允许参与活动。
     *
     * <p>目的：支撑风控或运营封禁场景。即使没有配置白名单，
     * 黑名单也应该可以单独生效并阻断指定用户。</p>
     */
    @Test
    void shouldRejectUserWhenUserInBlacklist() {
        // Given
        activityDccConfig.setActivityBlacklist("user-001,user-002");
        activityDccConfig.setActivityWhitelist("");

        // When
        boolean allowed = activityAccessPolicy.isAllowed("user-001");

        // Then
        assertFalse(allowed);
    }

    /**
     * 验证黑名单用户以外的用户，在白名单为空时允许参与活动。
     *
     * <p>目的：保证单独启用黑名单时只拦截命中用户，不影响其他正常用户流量。</p>
     */
    @Test
    void shouldAllowUserWhenBlacklistExistsAndUserNotInBlacklist() {
        // Given
        activityDccConfig.setActivityBlacklist("user-001,user-002");
        activityDccConfig.setActivityWhitelist("");

        // When
        boolean allowed = activityAccessPolicy.isAllowed("user-003");

        // Then
        assertTrue(allowed);
    }

    /**
     * 验证同一用户同时命中黑名单和白名单时，黑名单优先拒绝。
     *
     * <p>目的：明确黑白名单冲突时的裁决规则。黑名单通常代表风控、
     * 封禁或异常用户，优先级必须高于白名单放行。</p>
     */
    @Test
    void shouldRejectUserWhenUserInBothBlacklistAndWhitelist() {
        // Given
        activityDccConfig.setActivityBlacklist("user-001");
        activityDccConfig.setActivityWhitelist("user-001,user-002");

        // When
        boolean allowed = activityAccessPolicy.isAllowed("user-001");

        // Then
        assertFalse(allowed);
    }

    /**
     * 验证名单配置中的空白字符和空元素会被清洗后再参与判断。
     *
     * <p>目的：提升运营配置的容错性。DCC 配置由人工维护时，
     * 很容易出现多余空格、连续逗号或尾逗号，策略解析时应该忽略这些噪音。</p>
     */
    @Test
    void shouldTrimBlankAndIgnoreEmptyTokensWhenParsingLists() {
        // Given
        activityDccConfig.setActivityBlacklist(" user-001, ,");
        activityDccConfig.setActivityWhitelist(" user-002, , user-003 ");

        // When
        boolean blacklistUserAllowed = activityAccessPolicy.isAllowed("user-001");
        boolean whitelistUserAllowed = activityAccessPolicy.isAllowed("user-002");
        boolean otherUserAllowed = activityAccessPolicy.isAllowed("user-004");

        // Then
        assertFalse(blacklistUserAllowed);
        assertTrue(whitelistUserAllowed);
        assertFalse(otherUserAllowed);
    }

    /**
     * 验证用户 ID 为 {@code null} 时抛出参数异常。
     *
     * <p>目的：避免上游传入非法用户标识时继续执行名单判断，
     * 从而让问题在入口附近暴露。</p>
     */
    @Test
    void shouldThrowExceptionWhenUserIdIsNull() {
        // Given
        activityDccConfig.setActivityBlacklist("");
        activityDccConfig.setActivityWhitelist("");

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> activityAccessPolicy.isAllowed(null));
    }

    /**
     * 验证用户 ID 为空白字符串时抛出参数异常。
     *
     * <p>目的：防止空白用户标识绕过名单判断，保证调用方必须传入有效用户 ID。</p>
     */
    @Test
    void shouldThrowExceptionWhenUserIdIsBlank() {
        // Given
        activityDccConfig.setActivityBlacklist("");
        activityDccConfig.setActivityWhitelist("");

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> activityAccessPolicy.isAllowed("   "));
    }
}
