package com.lavyoung.marketforge.application.strategy.service;

import com.lavyoung.marketforge.application.strategy.model.RaffleCommand;
import com.lavyoung.marketforge.application.strategy.model.RaffleResult;
import com.lavyoung.marketforge.application.strategy.model.StrategyAwardResult;
import com.lavyoung.marketforge.application.strategy.service.impl.StrategyStrategyRaffleServiceImpl;
import com.lavyoung.marketforge.domain.activity.model.vo.AwardUnlockScopeVO;
import com.lavyoung.marketforge.domain.activity.repository.IActivityRepository;
import com.lavyoung.marketforge.domain.award.model.entity.AwardDefinitionEntity;
import com.lavyoung.marketforge.domain.award.model.vo.AwardClaimModeVO;
import com.lavyoung.marketforge.domain.award.model.vo.AwardConfigSnapshot;
import com.lavyoung.marketforge.domain.award.model.vo.AwardDefinitionStateVO;
import com.lavyoung.marketforge.domain.award.model.vo.AwardTypeVO;
import com.lavyoung.marketforge.domain.award.repository.IAwardDefinitionRepository;
import com.lavyoung.marketforge.domain.strategy.model.entity.RaffleAwardEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.RaffleFactorEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyAwardEntity;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyRepository;
import com.lavyoung.marketforge.domain.strategy.service.IRaffleAward;
import com.lavyoung.marketforge.domain.strategy.service.IRaffleStrategy;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.IStrategyArmory;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 策略抽奖应用服务测试。
 *
 * <p>验证抽奖命中后的策略奖品查询、完整奖品定义读取、履约快照形成和
 * 奖品生命周期边界，防止草稿或停用奖品继续产生新的中奖记录。</p>
 */
class StrategyRaffleServiceImplTest {

    private static final Long STRATEGY_ID = 100_001L;
    private static final Long ACTIVITY_ID = 100_301L;
    private static final Long AWARD_ID = 100_011L;
    private static final LocalDate STATISTICS_DATE = LocalDate.of(2026, 10, 17);
    private static final LocalDateTime END_DATE_TIME = LocalDateTime.of(2026, 10, 31, 23, 59);

    /**
     * Given 策略命中一个已启用奖品，When 执行抽奖，Then 应形成包含类型和版本的完整履约快照。
     */
    @Test
    void shouldCreateFulfillmentSnapshotForActiveAward() {
        // Given
        RaffleFixture fixture = raffleFixture(AwardDefinitionStateVO.ACTIVE);

        // When
        RaffleResult result = fixture.service().raffle(raffleCommand());

        // Then
        assertAll(
                () -> assertEquals(STRATEGY_ID, result.strategyId()),
                () -> assertEquals(AWARD_ID, result.awardId()),
                () -> assertEquals("随机积分", result.awardTitle()),
                () -> assertEquals("抽奖后自动发放 10 积分", result.awardDesc()),
                () -> assertEquals("credit_10", result.fulfillmentSnapshot().awardCode()),
                () -> assertEquals("internal_credit", result.fulfillmentSnapshot().awardType()),
                () -> assertEquals("automatic", result.fulfillmentSnapshot().claimMode()),
                () -> assertNull(result.fulfillmentSnapshot().providerCode()),
                () -> assertEquals("{\"amount\":10}", result.fulfillmentSnapshot().configContent()),
                () -> assertEquals(1, result.fulfillmentSnapshot().configVersion())
        );
        verify(fixture.raffleStrategy()).performRaffle(raffleFactor());
        verify(fixture.strategyRepository()).getStrategyAwardEntity(STRATEGY_ID, AWARD_ID);
        verify(fixture.awardDefinitionRepository()).queryByAwardId(AWARD_ID);
    }

    /**
     * Given 空命令，When 执行抽奖，Then 应在调用领域服务前快速失败。
     */
    @Test
    void shouldRejectNullCommand() {
        StrategyStrategyRaffleServiceImpl service = service(
                mock(IRaffleStrategy.class), mock(IStrategyRepository.class), mock(IAwardDefinitionRepository.class), mock(IRaffleAward.class));

        assertThrows(NullPointerException.class, () -> service.raffle(null));
    }

    /**
     * Given 命中奖品仍处于草稿状态，When 形成抽奖结果，Then 应拒绝产生新的中奖记录。
     */
    @Test
    void shouldRejectDraftAward() {
        assertAwardNotAvailable(AwardDefinitionStateVO.DRAFT);
    }

    /**
     * Given 命中奖品已经停用，When 形成抽奖结果，Then 应拒绝产生新的中奖记录。
     */
    @Test
    void shouldRejectDisabledAward() {
        assertAwardNotAvailable(AwardDefinitionStateVO.DISABLED);
    }

    /**
     * Given 策略引用的奖品定义不存在，When 形成抽奖结果，Then 应报告奖品不存在。
     */
    @Test
    void shouldRejectMissingAwardDefinition() {
        // Given
        IRaffleStrategy raffleStrategy = mock(IRaffleStrategy.class);
        IStrategyRepository strategyRepository = mock(IStrategyRepository.class);
        IAwardDefinitionRepository awardDefinitionRepository = mock(IAwardDefinitionRepository.class);
        prepareRaffleResult(raffleStrategy, strategyRepository);
        when(awardDefinitionRepository.queryByAwardId(AWARD_ID)).thenReturn(Optional.empty());
        StrategyStrategyRaffleServiceImpl service = service(
                raffleStrategy, strategyRepository, awardDefinitionRepository, mock(IRaffleAward.class));

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> service.raffle(raffleCommand()));

        // Then
        assertEquals(BusinessResponseCode.AWARD_NOT_FOUND, exception.getResponseCode());
    }

    /**
     * Given 领域服务返回奖品实体，When 查询策略奖品，Then 应只输出应用层契约字段。
     */
    @Test
    void shouldMapDomainAwardsToApplicationResults() {
        // Given
        IRaffleAward raffleAward = mock(IRaffleAward.class);
        StrategyAwardEntity entity = strategyAward();
        when(raffleAward.queryRaffleStrategyAwardList(STRATEGY_ID)).thenReturn(List.of(entity));
        StrategyStrategyRaffleServiceImpl service = service(
                mock(IRaffleStrategy.class), mock(IStrategyRepository.class), mock(IAwardDefinitionRepository.class), raffleAward);

        // When
        List<StrategyAwardResult> results = service.queryRaffleStrategyAwardList(STRATEGY_ID);

        // Then
        StrategyAwardResult result = results.get(0);
        assertAll(
                () -> assertEquals(1, results.size()),
                () -> assertEquals(entity.strategyId(), result.strategyId()),
                () -> assertEquals(entity.awardId(), result.awardId()),
                () -> assertEquals(entity.awardTitle(), result.awardTitle()),
                () -> assertEquals(entity.awardRate(), result.awardRate())
        );
        verify(raffleAward).queryRaffleStrategyAwardList(STRATEGY_ID);
    }

    private void assertAwardNotAvailable(AwardDefinitionStateVO state) {
        RaffleFixture fixture = raffleFixture(state);
        BusinessException exception = assertThrows(
                BusinessException.class, () -> fixture.service().raffle(raffleCommand()));
        assertEquals(BusinessResponseCode.AWARD_NOT_AVAILABLE, exception.getResponseCode());
    }

    private RaffleFixture raffleFixture(AwardDefinitionStateVO state) {
        IRaffleStrategy raffleStrategy = mock(IRaffleStrategy.class);
        IStrategyRepository strategyRepository = mock(IStrategyRepository.class);
        IAwardDefinitionRepository awardDefinitionRepository = mock(IAwardDefinitionRepository.class);
        prepareRaffleResult(raffleStrategy, strategyRepository);
        when(awardDefinitionRepository.queryByAwardId(AWARD_ID)).thenReturn(Optional.of(awardDefinition(state)));
        return new RaffleFixture(service(raffleStrategy, strategyRepository, awardDefinitionRepository, mock(IRaffleAward.class)),
                raffleStrategy, strategyRepository, awardDefinitionRepository);
    }

    private void prepareRaffleResult(IRaffleStrategy raffleStrategy, IStrategyRepository strategyRepository) {
        when(raffleStrategy.performRaffle(raffleFactor())).thenReturn(
                RaffleAwardEntity.builder().strategyId(STRATEGY_ID).awardId(AWARD_ID).build());
        when(strategyRepository.getStrategyAwardEntity(STRATEGY_ID, AWARD_ID)).thenReturn(Optional.of(strategyAward()));
    }

    private StrategyStrategyRaffleServiceImpl service(IRaffleStrategy raffleStrategy, IStrategyRepository strategyRepository,
                                                      IAwardDefinitionRepository awardDefinitionRepository, IRaffleAward raffleAward) {
        return new StrategyStrategyRaffleServiceImpl(raffleStrategy, mock(IStrategyArmory.class),
                mock(IActivityRepository.class), strategyRepository, awardDefinitionRepository, raffleAward);
    }

    private RaffleCommand raffleCommand() {
        return new RaffleCommand(
                "user-001", STRATEGY_ID, ACTIVITY_ID, AwardUnlockScopeVO.TOTAL, STATISTICS_DATE, END_DATE_TIME);
    }

    private RaffleFactorEntity raffleFactor() {
        return RaffleFactorEntity.builder()
                .userId("user-001")
                .strategyId(STRATEGY_ID)
                .activityId(ACTIVITY_ID)
                .awardUnlockScope(AwardUnlockScopeVO.TOTAL)
                .statisticsDate(STATISTICS_DATE)
                .endDateTime(END_DATE_TIME)
                .build();
    }

    private StrategyAwardEntity strategyAward() {
        return StrategyAwardEntity.builder()
                .strategyId(STRATEGY_ID)
                .awardId(AWARD_ID)
                .awardTitle("随机积分")
                .awardCount(100)
                .awardCountSurplus(80)
                .awardRate(new BigDecimal("0.1000"))
                .sort(1)
                .build();
    }

    private AwardDefinitionEntity awardDefinition(AwardDefinitionStateVO state) {
        return new AwardDefinitionEntity(AWARD_ID, "credit_10", "10 积分", AwardTypeVO.INTERNAL_CREDIT,
                AwardClaimModeVO.AUTOMATIC, null, new AwardConfigSnapshot("{\"amount\":10}", 1),
                "抽奖后自动发放 10 积分", state, 1, "operator-001", "operator-001",
                LocalDateTime.of(2026, 10, 1, 10, 0), LocalDateTime.of(2026, 10, 1, 10, 0));
    }

    private record RaffleFixture(
            StrategyStrategyRaffleServiceImpl service,
            IRaffleStrategy raffleStrategy,
            IStrategyRepository strategyRepository,
            IAwardDefinitionRepository awardDefinitionRepository
    ) {
    }
}
