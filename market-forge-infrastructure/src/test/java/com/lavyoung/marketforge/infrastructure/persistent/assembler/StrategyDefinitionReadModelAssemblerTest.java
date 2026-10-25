package com.lavyoung.marketforge.infrastructure.persistent.assembler;

import com.lavyoung.marketforge.application.strategy.model.StrategyDefinitionSearchItem;
import com.lavyoung.marketforge.infrastructure.persistent.po.StrategyPO;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link StrategyDefinitionReadModelAssembler} 映射契约测试。
 *
 * <p>验证策略基础配置、生命周期、乐观锁和审计字段均能完整投影到列表读模型。</p>
 */
class StrategyDefinitionReadModelAssemblerTest {

    private static final StrategyDefinitionReadModelAssembler ASSEMBLER = Mappers.getMapper(StrategyDefinitionReadModelAssembler.class);

    @Test
    void shouldMapAllStrategyDefinitionFields() {
        // Given
        StrategyPO source = strategyPO();

        // When
        List<StrategyDefinitionSearchItem> items = ASSEMBLER.toItems(List.of(source));

        // Then
        StrategyDefinitionSearchItem item = items.get(0);
        assertAll(
                () -> assertEquals(source.getStrategyId(), item.strategyId()),
                () -> assertEquals(source.getStrategyDesc(), item.strategyDesc()),
                () -> assertEquals(source.getRuleModels(), item.ruleModels()),
                () -> assertEquals(source.getState(), item.state()),
                () -> assertEquals(source.getVersion(), item.version()),
                () -> assertEquals(source.getCreatedBy(), item.createdBy()),
                () -> assertEquals(source.getUpdatedBy(), item.updatedBy()),
                () -> assertEquals(source.getCreateTime(), item.createTime()),
                () -> assertEquals(source.getUpdateTime(), item.updateTime())
        );
    }

    private StrategyPO strategyPO() {
        StrategyPO po = new StrategyPO();
        po.setStrategyId(100001L);
        po.setStrategyDesc("九宫格抽奖策略");
        po.setRuleModels("rule_blacklist,rule_weight");
        po.setState("active");
        po.setVersion(2);
        po.setCreatedBy("admin");
        po.setUpdatedBy("operator");
        po.setCreateTime(LocalDateTime.of(2026, 10, 25, 10, 0));
        po.setUpdateTime(LocalDateTime.of(2026, 10, 25, 11, 0));
        return po;
    }
}
