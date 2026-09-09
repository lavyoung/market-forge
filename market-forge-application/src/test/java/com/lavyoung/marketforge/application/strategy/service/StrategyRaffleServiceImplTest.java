package com.lavyoung.marketforge.application.strategy.service;

import com.lavyoung.marketforge.application.strategy.model.RaffleCommand;
import com.lavyoung.marketforge.application.strategy.model.RaffleResult;
import com.lavyoung.marketforge.application.strategy.model.StrategyAwardResult;
import com.lavyoung.marketforge.application.strategy.service.impl.StrategyStrategyRaffleServiceImpl;
import com.lavyoung.marketforge.domain.strategy.model.entity.RaffleAwardEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.RaffleFactorEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyAwardEntity;
import com.lavyoung.marketforge.domain.strategy.service.IRaffleAward;
import com.lavyoung.marketforge.domain.strategy.service.IRaffleStrategy;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.IStrategyArmory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 验证抽奖应用服务的命令映射、领域调用与异常边界。
 */
class StrategyRaffleServiceImplTest {

    /**
     * Given 合法抽奖命令与领域结果，When 执行用例，Then 返回隔离后的应用结果。
     */
    @Test
    void shouldExecuteRaffleUseCase() {
        // Given
        IRaffleStrategy raffleStrategy = mock(IRaffleStrategy.class);
        IStrategyArmory strategyArmory = mock(IStrategyArmory.class);
        RaffleFactorEntity factor = RaffleFactorEntity.builder()
                .userId("user-001")
                .strategyId(100_001L)
                .build();
        when(raffleStrategy.performRaffle(factor)).thenReturn(RaffleAwardEntity.builder()
                .strategyId(100_001L)
                .awardId(100_011L)
                .awardKey("random_ore")
                .awardConfig("quantity=1")
                .awardDesc("随机矿石")
                .build());
        StrategyStrategyRaffleServiceImpl service = new StrategyStrategyRaffleServiceImpl(
                raffleStrategy, strategyArmory, mock(IRaffleAward.class));

        // When
        RaffleResult result = service.raffle(new RaffleCommand("user-001", 100_001L));

        // Then
        assertAll(
                () -> assertEquals(100_001L, result.strategyId()),
                () -> assertEquals(100_011L, result.awardId()),
                () -> assertEquals("random_ore", result.awardKey())
        );
        verify(raffleStrategy).performRaffle(factor);
    }

    /**
     * Given 空命令，When 执行抽奖用例，Then 在进入领域层前快速失败。
     */
    @Test
    void shouldRejectNullCommand() {
        // Given
        StrategyStrategyRaffleServiceImpl service = new StrategyStrategyRaffleServiceImpl(
                mock(IRaffleStrategy.class), mock(IStrategyArmory.class), mock(IRaffleAward.class));

        // When & Then
        assertThrows(NullPointerException.class, () -> service.raffle(null));
    }

    /**
     * Given 领域服务返回奖品实体，When 查询策略奖品，Then 仅输出应用层契约字段。
     */
    @Test
    void shouldMapDomainAwardsToApplicationResults() {
        // Given
        IRaffleAward raffleAward = mock(IRaffleAward.class);
        StrategyAwardEntity entity = StrategyAwardEntity.builder()
                .strategyId(100_001L)
                .awardId(100_011L)
                .awardTitle("随机矿石")
                .awardCount(100)
                .awardCountSurplus(80)
                .awardRate(new BigDecimal("0.1000"))
                .sort(1)
                .ruleModels("rule_lock")
                .build();
        when(raffleAward.queryRaffleStrategyAwardList(100_001L)).thenReturn(List.of(entity));
        StrategyStrategyRaffleServiceImpl service = new StrategyStrategyRaffleServiceImpl(
                mock(IRaffleStrategy.class), mock(IStrategyArmory.class), raffleAward);

        // When
        List<StrategyAwardResult> results = service.queryRaffleStrategyAwardList(100_001L);

        // Then
        StrategyAwardResult result = results.get(0);
        assertAll(
                () -> assertEquals(1, results.size()),
                () -> assertEquals(entity.strategyId(), result.strategyId()),
                () -> assertEquals(entity.awardId(), result.awardId()),
                () -> assertEquals(entity.awardTitle(), result.awardTitle()),
                () -> assertEquals(entity.awardRate(), result.awardRate())
        );
        verify(raffleAward).queryRaffleStrategyAwardList(100_001L);
    }
}
