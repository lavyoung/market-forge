package com.lavyoung.marketforge.application.strategy.model;

import com.lavyoung.marketforge.domain.activity.model.vo.AwardUnlockScopeVO;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 抽奖应用命令活动上下文校验测试。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/17
 */
class RaffleCommandTest {

    /**
     * Given 兼容策略抽奖入口，When 只提供用户和策略，Then 命令保持合法且不包含活动上下文。
     */
    @Test
    void shouldSupportLegacyCommandWithoutActivityContext() {
        RaffleCommand command = new RaffleCommand("user-001", 100_001L);

        assertFalse(command.hasActivityContext());
    }

    /**
     * Given 完整活动配置，When 创建抽奖命令，Then 命令识别为活动抽奖上下文。
     */
    @Test
    void shouldAcceptCompleteActivityContext() {
        RaffleCommand command = new RaffleCommand(
                "user-001",
                100_001L,
                100_301L,
                AwardUnlockScopeVO.DAY,
                LocalDate.of(2026, 10, 17),
                null
        );

        assertTrue(command.hasActivityContext());
    }

    /**
     * Given 活动上下文字段不完整，When 创建抽奖命令，Then 在进入领域层前快速失败。
     */
    @Test
    void shouldRejectIncompleteActivityContext() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new RaffleCommand(
                        "user-001",
                        100_001L,
                        100_301L,
                        null,
                        LocalDate.of(2026, 10, 17),
                        null
                )
        );
    }
}
