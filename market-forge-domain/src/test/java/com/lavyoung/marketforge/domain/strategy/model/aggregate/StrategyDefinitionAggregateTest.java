package com.lavyoung.marketforge.domain.strategy.model.aggregate;

import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyAwardEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyRuleEntity;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyDefinitionStateVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 策略定义聚合测试。
 *
 * <p>验证聚合内子配置归属、草稿修改、历史策略复制、
 * 库存隔离和不可变集合等领域不变量。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
class StrategyDefinitionAggregateTest {

    private static final Long STRATEGY_ID = 100_001L;
    private static final Long NEW_STRATEGY_ID = 100_002L;
    private static final LocalDateTime CREATE_TIME = LocalDateTime.of(2026, 10, 25, 10, 0);
    private static final LocalDateTime UPDATE_TIME = LocalDateTime.of(2026, 10, 25, 11, 0);

    /**
     * Given 合法的草稿及可变入参集合，When 创建聚合，Then 应复制子配置并保持草稿能力。
     */
    @Test
    void shouldCreateImmutableDraftAggregate() {
        // Given
        List<StrategyAwardEntity> awards = new ArrayList<>(List.of(award(STRATEGY_ID, 200_001L, 80)));
        List<StrategyRuleEntity> rules = new ArrayList<>(List.of(rule(STRATEGY_ID, 200_001L)));

        // When
        StrategyDefinitionAggregate aggregate = aggregate(StrategyDefinitionStateVO.DRAFT, awards, rules);
        awards.add(award(STRATEGY_ID, 200_002L, 20));
        rules.clear();

        // Then
        assertAll(
                () -> assertTrue(aggregate.canEdit()),
                () -> assertFalse(aggregate.canReference()),
                () -> assertEquals(1, aggregate.awards().size()),
                () -> assertEquals(1, aggregate.rules().size()),
                () -> assertThrows(UnsupportedOperationException.class,
                        () -> aggregate.awards().add(award(STRATEGY_ID, 200_003L, 10))),
                () -> assertThrows(UnsupportedOperationException.class, aggregate.rules()::clear)
        );
    }

    /**
     * Given 子配置属于其他策略或奖品重复，When 创建聚合，Then 应在进入仓储前拒绝。
     */
    @Test
    void shouldRejectInvalidChildOwnershipAndDuplicateAwards() {
        // Given
        StrategyAwardEntity foreignAward = award(999_999L, 200_001L, 80);
        StrategyRuleEntity foreignRule = rule(999_999L, 200_001L);
        StrategyAwardEntity duplicatedAward = award(STRATEGY_ID, 200_001L, 20);

        // When & Then
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> aggregate(StrategyDefinitionStateVO.DRAFT, List.of(foreignAward), List.of())),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> aggregate(StrategyDefinitionStateVO.DRAFT, List.of(), List.of(foreignRule))),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> aggregate(StrategyDefinitionStateVO.DRAFT,
                                List.of(award(STRATEGY_ID, 200_001L, 80), duplicatedAward), List.of()))
        );
    }

    /**
     * Given 草稿策略，When 修改基础信息，Then 应保留标识、版本和子配置并更新操作人。
     */
    @Test
    void shouldUpdateDraftBasicInformation() {
        // Given
        StrategyDefinitionAggregate draft = aggregate(
                StrategyDefinitionStateVO.DRAFT,
                List.of(award(STRATEGY_ID, 200_001L, 80)),
                List.of(rule(STRATEGY_ID, 200_001L))
        );

        // When
        StrategyDefinitionAggregate updated = draft.updateBasicInfo(
                "新的策略描述", "rule_blacklist,rule_weight", "operator-002");

        // Then
        assertAll(
                () -> assertEquals(STRATEGY_ID, updated.strategyId()),
                () -> assertEquals("新的策略描述", updated.strategyDesc()),
                () -> assertEquals("rule_blacklist,rule_weight", updated.ruleModels()),
                () -> assertEquals(draft.version(), updated.version()),
                () -> assertEquals("operator-001", updated.createdBy()),
                () -> assertEquals("operator-002", updated.updatedBy()),
                () -> assertEquals(draft.awards(), updated.awards()),
                () -> assertEquals(draft.rules(), updated.rules())
        );
    }

    /**
     * Given 已生效策略且库存已消耗，When 复制为新草稿，Then 应重建子配置并重置剩余库存。
     */
    @Test
    void shouldCopyActiveStrategyAsIndependentDraft() {
        // Given
        StrategyDefinitionAggregate active = aggregate(
                StrategyDefinitionStateVO.ACTIVE,
                List.of(award(STRATEGY_ID, 200_001L, 35)),
                List.of(rule(STRATEGY_ID, 200_001L))
        );

        // When
        StrategyDefinitionAggregate copied = active.copyAsDraft(NEW_STRATEGY_ID, "operator-002");

        // Then
        StrategyAwardEntity copiedAward = copied.awards().get(0);
        StrategyRuleEntity copiedRule = copied.rules().get(0);
        assertAll(
                () -> assertEquals(NEW_STRATEGY_ID, copied.strategyId()),
                () -> assertEquals(StrategyDefinitionStateVO.DRAFT, copied.state()),
                () -> assertEquals(1, copied.version()),
                () -> assertEquals("operator-002", copied.createdBy()),
                () -> assertEquals("operator-002", copied.updatedBy()),
                () -> assertNull(copied.createTime()),
                () -> assertNull(copied.updateTime()),
                () -> assertEquals(NEW_STRATEGY_ID, copiedAward.strategyId()),
                () -> assertEquals(copiedAward.awardCount(), copiedAward.awardCountSurplus()),
                () -> assertNull(copiedAward.createTime()),
                () -> assertNull(copiedAward.updateTime()),
                () -> assertEquals(NEW_STRATEGY_ID, copiedRule.strategyId()),
                () -> assertNull(copiedRule.createTime()),
                () -> assertNull(copiedRule.updateTime())
        );
    }

    /**
     * Given 草稿或非法新标识，When 请求复制策略，Then 应拒绝无意义的版本创建。
     */
    @Test
    void shouldRejectInvalidStrategyCopy() {
        // Given
        StrategyDefinitionAggregate draft = aggregate(StrategyDefinitionStateVO.DRAFT, List.of(), List.of());
        StrategyDefinitionAggregate active = aggregate(StrategyDefinitionStateVO.ACTIVE, List.of(), List.of());

        // When & Then
        assertAll(
                () -> assertThrows(IllegalStateException.class,
                        () -> draft.copyAsDraft(NEW_STRATEGY_ID, "operator-002")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> active.copyAsDraft(STRATEGY_ID, "operator-002")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> active.copyAsDraft(NEW_STRATEGY_ID, " "))
        );
    }

    /**
     * Given 已生效策略，When 尝试原地修改基础信息，Then 应要求通过复制新草稿变更。
     */
    @Test
    void shouldRejectInPlaceUpdateForActiveStrategy() {
        // Given
        StrategyDefinitionAggregate active = aggregate(StrategyDefinitionStateVO.ACTIVE, List.of(), List.of());

        // When & Then
        assertThrows(IllegalStateException.class,
                () -> active.updateBasicInfo("不允许的修改", null, "operator-002"));
    }

    private StrategyDefinitionAggregate aggregate(StrategyDefinitionStateVO state,
                                                  List<StrategyAwardEntity> awards,
                                                  List<StrategyRuleEntity> rules) {
        return new StrategyDefinitionAggregate(
                STRATEGY_ID, "测试抽奖策略", "rule_blacklist",
                state, 3, "operator-001", "operator-001",
                CREATE_TIME, UPDATE_TIME, awards, rules
        );
    }

    private StrategyAwardEntity award(Long strategyId, Long awardId, int surplus) {
        return new StrategyAwardEntity(
                strategyId, awardId, "100积分", "自动入账", 100, surplus,
                new BigDecimal("0.50000"), "rule_stock", 1, CREATE_TIME, UPDATE_TIME
        );
    }

    private StrategyRuleEntity rule(Long strategyId, Long awardId) {
        return new StrategyRuleEntity(
                strategyId, awardId, 2, "rule_lock", "1", "累计抽奖1次解锁",
                CREATE_TIME, UPDATE_TIME
        );
    }
}
