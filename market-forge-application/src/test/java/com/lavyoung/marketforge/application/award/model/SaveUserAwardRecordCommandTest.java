package com.lavyoung.marketforge.application.award.model;

import com.lavyoung.marketforge.domain.award.event.AwardFulfillmentSnapshot;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 保存用户中奖记录命令测试。
 *
 * <p>验证结算边界必须携带完整履约快照和合法业务标识，避免不完整命令进入
 * “中奖记录与可靠消息任务同事务落库”的关键链路。</p>
 */
class SaveUserAwardRecordCommandTest {

    private static final AwardFulfillmentSnapshot SNAPSHOT = new AwardFulfillmentSnapshot(
            "credit_10", "internal_credit", "automatic", null, "{\"amount\":10}", 1);
    private static final LocalDateTime AWARD_TIME = LocalDateTime.of(2026, 10, 23, 11, 0);

    /**
     * Given 完整中奖结算数据，When 创建保存命令，Then 应原样保留履约快照和业务事实。
     */
    @Test
    void shouldCreateCommandWithFulfillmentSnapshot() {
        // Given / When
        SaveUserAwardRecordCommand command = command(SNAPSHOT);

        // Then
        assertAll(
                () -> assertEquals("user-001", command.userId()),
                () -> assertEquals(100_301L, command.activityId()),
                () -> assertEquals(100_001L, command.strategyId()),
                () -> assertEquals("order-001", command.orderId()),
                () -> assertEquals(100_011L, command.awardId()),
                () -> assertSame(SNAPSHOT, command.fulfillmentSnapshot()),
                () -> assertEquals("10 积分", command.awardTitle()),
                () -> assertEquals(AWARD_TIME, command.awardTime())
        );
    }

    /**
     * Given 履约快照为空，When 创建保存命令，Then 应在进入持久化事务前快速失败。
     */
    @Test
    void shouldRejectMissingFulfillmentSnapshot() {
        // Given / When / Then
        assertThrows(NullPointerException.class, () -> command(null));
    }

    /**
     * Given 业务标识非法，When 创建保存命令，Then 应拒绝不可关联的中奖记录。
     */
    @Test
    void shouldRejectInvalidBusinessIdentifiers() {
        // Given / When / Then
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new SaveUserAwardRecordCommand(
                                "user-001", 0L, 100_001L, "order-001", 100_011L,
                                SNAPSHOT, "10 积分", AWARD_TIME)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new SaveUserAwardRecordCommand(
                                "user-001", 100_301L, -1L, "order-001", 100_011L,
                                SNAPSHOT, "10 积分", AWARD_TIME)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new SaveUserAwardRecordCommand(
                                "user-001", 100_301L, 100_001L, "order-001", 0L,
                                SNAPSHOT, "10 积分", AWARD_TIME))
        );
    }

    private SaveUserAwardRecordCommand command(AwardFulfillmentSnapshot snapshot) {
        return new SaveUserAwardRecordCommand(
                "user-001", 100_301L, 100_001L, "order-001", 100_011L,
                snapshot, "10 积分", AWARD_TIME);
    }
}
