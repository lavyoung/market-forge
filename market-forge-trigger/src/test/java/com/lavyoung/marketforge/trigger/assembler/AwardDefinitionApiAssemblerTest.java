package com.lavyoung.marketforge.trigger.assembler;

import com.lavyoung.marketforge.api.award.request.AwardDefinitionSearchRequest;
import com.lavyoung.marketforge.api.award.request.ChangeAwardDefinitionStateRequest;
import com.lavyoung.marketforge.api.award.request.CreateAwardDefinitionRequest;
import com.lavyoung.marketforge.api.award.request.UpdateAwardDefinitionRequest;
import com.lavyoung.marketforge.api.award.response.AwardDefinitionResponse;
import com.lavyoung.marketforge.application.award.model.*;
import com.lavyoung.marketforge.types.model.PageResult;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link AwardDefinitionApiAssembler} 单元测试。
 *
 * <p>验证奖品定义 API 模型与 Application 模型之间的转换契约，重点保护路径参数、
 * 服务端认证操作人以及超过 JavaScript 安全整数范围的奖品标识。</p>
 */
class AwardDefinitionApiAssemblerTest {

    private static final Long AWARD_ID = 9_007_199_254_740_993L;
    private static final String OPERATOR = "admin";
    private static final AwardDefinitionApiAssembler ASSEMBLER = Mappers.getMapper(AwardDefinitionApiAssembler.class);

    /**
     * 验证 HTTP 查询字段进入应用查询对象，并把分页字段组合为共享分页值对象。
     */
    @Test
    void shouldMapSearchRequestToApplicationQuery() {
        // Given
        AwardDefinitionSearchRequest request = new AwardDefinitionSearchRequest("credit_100", "积分", "internal_credit", "active", 2, 20);

        // When
        AwardDefinitionSearchQuery query = ASSEMBLER.toSearchQuery(request);

        // Then
        assertAll(
                () -> assertEquals(request.awardCode(), query.awardCode()),
                () -> assertEquals(request.awardName(), query.awardName()),
                () -> assertEquals(request.awardType(), query.awardType()),
                () -> assertEquals(request.state(), query.state()),
                () -> assertEquals(2, query.page().pageNumber()),
                () -> assertEquals(20, query.page().pageSize())
        );
    }

    /**
     * 验证创建请求全部进入应用命令，并由服务端补充操作人。
     */
    @Test
    void shouldMapCreateRequestAndAuthenticatedOperator() {
        // Given
        CreateAwardDefinitionRequest request = new CreateAwardDefinitionRequest(
                "credit_100", "100积分", "internal_credit", "automatic", null,
                "{\"amount\":100}", 1, "中奖后自动发放积分");

        // When
        CreateAwardDefinitionCommand command = ASSEMBLER.toCreateCommand(request, OPERATOR);

        // Then
        assertAll(
                () -> assertEquals(request.awardCode(), command.awardCode()),
                () -> assertEquals(request.awardName(), command.awardName()),
                () -> assertEquals(request.awardType(), command.awardType()),
                () -> assertEquals(request.claimMode(), command.claimMode()),
                () -> assertEquals(request.providerCode(), command.providerCode()),
                () -> assertEquals(request.awardConfig(), command.awardConfig()),
                () -> assertEquals(request.configVersion(), command.configVersion()),
                () -> assertEquals(request.awardDesc(), command.awardDesc()),
                () -> assertEquals(OPERATOR, command.operator())
        );
    }

    /**
     * 验证修改命令中的奖品标识来自路径，操作人来自认证会话。
     */
    @Test
    void shouldMapUpdateRequestWithPathAwardId() {
        // Given
        UpdateAwardDefinitionRequest request = new UpdateAwardDefinitionRequest(
                "credit_200", "200积分", "internal_credit", "automatic", null,
                "{\"amount\":200}", 2, "中奖后自动发放二百积分", 3);

        // When
        UpdateAwardDefinitionCommand command = ASSEMBLER.toUpdateCommand(AWARD_ID, request, OPERATOR);

        // Then
        assertAll(
                () -> assertEquals(AWARD_ID, command.awardId()),
                () -> assertEquals(request.awardCode(), command.awardCode()),
                () -> assertEquals(request.awardConfig(), command.awardConfig()),
                () -> assertEquals(request.configVersion(), command.configVersion()),
                () -> assertEquals(request.expectedVersion(), command.expectedVersion()),
                () -> assertEquals(OPERATOR, command.operator())
        );
    }

    /**
     * 验证状态命令只组合路径标识、目标状态、版本和认证操作人。
     */
    @Test
    void shouldMapStateRequestWithServerControlledFields() {
        // Given
        ChangeAwardDefinitionStateRequest request = new ChangeAwardDefinitionStateRequest("active", 2);

        // When
        ChangeAwardDefinitionStateCommand command = ASSEMBLER.toStateCommand(AWARD_ID, request, OPERATOR);

        // Then
        assertAll(
                () -> assertEquals(AWARD_ID, command.awardId()),
                () -> assertEquals("active", command.targetState()),
                () -> assertEquals(2, command.expectedVersion()),
                () -> assertEquals(OPERATOR, command.operator())
        );
    }

    /**
     * 验证应用层长整型标识会完整转换为字符串，不经过 JavaScript 数值解析。
     */
    @Test
    void shouldMapResultAndPreserveLongAwardId() {
        // Given
        LocalDateTime createTime = LocalDateTime.of(2026, 10, 22, 10, 0);
        LocalDateTime updateTime = LocalDateTime.of(2026, 10, 22, 10, 30);
        AwardDefinitionResult result = new AwardDefinitionResult(
                AWARD_ID, "credit_100", "100积分", "internal_credit", "automatic", null,
                "{\"amount\":100}", 1, "中奖后自动发放积分", "draft", 1,
                "admin", "admin", createTime, updateTime);

        // When
        AwardDefinitionResponse response = ASSEMBLER.toResponse(result);

        // Then
        assertAll(
                () -> assertEquals("9007199254740993", response.awardId()),
                () -> assertEquals(result.awardCode(), response.awardCode()),
                () -> assertEquals(result.awardType(), response.awardType()),
                () -> assertEquals(result.awardConfig(), response.awardConfig()),
                () -> assertEquals(result.state(), response.state()),
                () -> assertEquals(result.version(), response.version()),
                () -> assertEquals(createTime, response.createTime()),
                () -> assertEquals(updateTime, response.updateTime())
        );
    }

    /**
     * 验证分页转换只改变记录类型，并完整保留分页元数据和长整型奖品标识。
     */
    @Test
    void shouldMapPageResultAndPreserveMetadata() {
        // Given
        AwardDefinitionResult result = result();
        PageResult<AwardDefinitionResult> source = new PageResult<>(List.of(result), 21, 2, 20);

        // When
        PageResult<AwardDefinitionResponse> response = ASSEMBLER.toPageResponse(source);

        // Then
        assertAll(
                () -> assertEquals(1, response.records().size()),
                () -> assertEquals(AWARD_ID.toString(), response.records().get(0).awardId()),
                () -> assertEquals(21, response.total()),
                () -> assertEquals(2, response.pageNumber()),
                () -> assertEquals(20, response.pageSize())
        );
    }

    private AwardDefinitionResult result() {
        return new AwardDefinitionResult(AWARD_ID, "credit_100", "100积分", "internal_credit", "automatic", null, "{\"amount\":100}", 1, "中奖后自动发放积分", "active", 2, "admin", "admin", LocalDateTime.of(2026, 10, 22, 10, 0), LocalDateTime.of(2026, 10, 22, 10, 30));
    }
}
