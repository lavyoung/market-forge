package com.lavyoung.marketforge.trigger.assembler;

import com.lavyoung.marketforge.api.strategy.request.*;
import com.lavyoung.marketforge.api.strategy.response.StrategyDefinitionResponse;
import com.lavyoung.marketforge.api.strategy.response.StrategyDefinitionSummaryResponse;
import com.lavyoung.marketforge.application.strategy.model.*;
import com.lavyoung.marketforge.types.model.PageResult;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 策略定义 API 装配器测试。
 *
 * <p>验证嵌套请求到应用命令的转换、服务端控制字段组合，以及策略和奖品
 * 长整型标识在响应边界转换为字符串。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
class StrategyDefinitionApiAssemblerTest {

    private static final Long STRATEGY_ID = 9_007_199_254_740_993L;
    private static final Long AWARD_ID = 9_007_199_254_740_991L;
    private static final String OPERATOR = "admin";
    private static final StrategyDefinitionApiAssembler ASSEMBLER = Mappers.getMapper(StrategyDefinitionApiAssembler.class);

    /**
     * Given 包含奖品和规则的创建请求，When 转换，Then 应保留嵌套配置并注入认证操作人。
     */
    @Test
    void shouldMapCreateRequestWithNestedConfiguration() {
        // Given
        CreateStrategyDefinitionRequest request = createRequest();

        // When
        CreateStrategyDefinitionCommand command = ASSEMBLER.toCreateCommand(request, OPERATOR);

        // Then
        assertAll(
                () -> assertEquals("九宫格策略", command.strategyDesc()),
                () -> assertEquals("rule_weight", command.ruleModels()),
                () -> assertEquals(AWARD_ID, command.awards().get(0).awardId()),
                () -> assertEquals("rule_weight", command.rules().get(0).ruleModel()),
                () -> assertEquals(OPERATOR, command.operator())
        );
    }

    /**
     * Given HTTP 分页查询参数，When 转换，Then 应生成规范化前的应用查询结构和共享分页对象。
     */
    @Test
    void shouldMapSearchRequestToApplicationQuery() {
        // Given
        StrategyDefinitionSearchRequest request = new StrategyDefinitionSearchRequest(STRATEGY_ID, "九宫格", "active", 2, 20);

        // When
        StrategyDefinitionSearchQuery query = ASSEMBLER.toSearchQuery(request);

        // Then
        assertAll(
                () -> assertEquals(STRATEGY_ID, query.strategyId()),
                () -> assertEquals("九宫格", query.strategyDesc()),
                () -> assertEquals("active", query.state()),
                () -> assertEquals(2, query.page().pageNumber()),
                () -> assertEquals(20, query.page().pageSize())
        );
    }

    /**
     * Given 含超出 JavaScript 安全整数范围标识的分页结果，When 转换，Then 标识应完整字符串化且分页元数据不变。
     */
    @Test
    void shouldMapSearchPageAndPreserveLongIdentifier() {
        // Given
        LocalDateTime now = LocalDateTime.of(2026, 10, 25, 16, 0);
        StrategyDefinitionSearchItem item = new StrategyDefinitionSearchItem(STRATEGY_ID, "九宫格策略", "rule_weight", "active", 2, OPERATOR, OPERATOR, now, now);
        PageResult<StrategyDefinitionSearchItem> source = new PageResult<>(List.of(item), 21L, 2, 20);

        // When
        PageResult<StrategyDefinitionSummaryResponse> response = ASSEMBLER.toPageResponse(source);

        // Then
        assertAll(
                () -> assertEquals(STRATEGY_ID.toString(), response.records().get(0).strategyId()),
                () -> assertEquals("九宫格策略", response.records().get(0).strategyDesc()),
                () -> assertEquals(21L, response.total()),
                () -> assertEquals(2, response.pageNumber()),
                () -> assertEquals(20, response.pageSize())
        );
    }

    /**
     * Given 路径标识、修改请求和认证身份，When 转换，Then 应组合为完整修改命令。
     */
    @Test
    void shouldMapUpdateRequestWithPathAndOperator() {
        // Given
        UpdateStrategyDefinitionRequest request = new UpdateStrategyDefinitionRequest(
                "修改策略", "rule_weight", createRequest().awards(), createRequest().rules(), 3);

        // When
        UpdateStrategyDefinitionCommand command = ASSEMBLER.toUpdateCommand(STRATEGY_ID, request, OPERATOR);

        // Then
        assertAll(
                () -> assertEquals(STRATEGY_ID, command.strategyId()),
                () -> assertEquals(3, command.expectedVersion()),
                () -> assertEquals(AWARD_ID, command.awards().get(0).awardId()),
                () -> assertEquals(OPERATOR, command.operator())
        );
    }

    /**
     * Given 历史策略路径标识，When 创建复制命令，Then 操作人必须来自认证会话。
     */
    @Test
    void shouldMapCopyCommandFromServerControlledFields() {
        // When
        CopyStrategyDefinitionCommand command = ASSEMBLER.toCopyCommand(STRATEGY_ID, OPERATOR);

        // Then
        assertAll(
                () -> assertEquals(STRATEGY_ID, command.sourceStrategyId()),
                () -> assertEquals(OPERATOR, command.operator())
        );
    }

    /**
     * Given 状态请求，When 转换，Then 应组合路径标识、版本和认证操作人。
     */
    @Test
    void shouldMapStateCommand() {
        // Given
        ChangeStrategyDefinitionStateRequest request = new ChangeStrategyDefinitionStateRequest("active", 3);

        // When
        ChangeStrategyDefinitionStateCommand command = ASSEMBLER.toStateCommand(STRATEGY_ID, request, OPERATOR);

        // Then
        assertAll(
                () -> assertEquals(STRATEGY_ID, command.strategyId()),
                () -> assertEquals("active", command.targetState()),
                () -> assertEquals(3, command.expectedVersion()),
                () -> assertEquals(OPERATOR, command.operator())
        );
    }

    /**
     * Given 包含十八位策略和奖品标识的应用结果，When 转换响应，Then 标识应完整字符串化。
     */
    @Test
    void shouldMapCompleteResultAndPreserveLongIdentifiers() {
        // Given
        LocalDateTime now = LocalDateTime.of(2026, 10, 25, 16, 0);
        StrategyAwardConfigResult award = new StrategyAwardConfigResult(
                AWARD_ID, "100积分", null, 100, 90, new BigDecimal("30"), "rule_stock", 1, now, now);
        StrategyRuleConfigResult strategyRule = new StrategyRuleConfigResult(
                null, 1, "rule_weight", "100:" + AWARD_ID, "权重奖池", now, now);
        StrategyDefinitionResult result = new StrategyDefinitionResult(
                STRATEGY_ID, "九宫格策略", "rule_weight", "active", 4,
                OPERATOR, OPERATOR, now, now, List.of(award), List.of(strategyRule));

        // When
        StrategyDefinitionResponse response = ASSEMBLER.toResponse(result);

        // Then
        assertAll(
                () -> assertEquals(STRATEGY_ID.toString(), response.strategyId()),
                () -> assertEquals(AWARD_ID.toString(), response.awards().get(0).awardId()),
                () -> assertNull(response.rules().get(0).awardId()),
                () -> assertEquals("active", response.state()),
                () -> assertEquals(4, response.version()),
                () -> assertEquals(now, response.updateTime())
        );
    }

    private CreateStrategyDefinitionRequest createRequest() {
        StrategyAwardConfigRequest award = new StrategyAwardConfigRequest(
                AWARD_ID, "100积分", null, 100, new BigDecimal("30"), "rule_stock", 1);
        StrategyRuleConfigRequest rule = new StrategyRuleConfigRequest(
                null, 1, "rule_weight", "100:" + AWARD_ID, "权重奖池");
        return new CreateStrategyDefinitionRequest("九宫格策略", "rule_weight", List.of(award), List.of(rule));
    }
}
