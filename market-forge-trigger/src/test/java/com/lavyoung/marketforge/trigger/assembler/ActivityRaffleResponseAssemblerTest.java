package com.lavyoung.marketforge.trigger.assembler;

import com.lavyoung.marketforge.api.activity.request.ActivityAwardListRequest;
import com.lavyoung.marketforge.api.activity.response.ActivityAccountResponse;
import com.lavyoung.marketforge.application.activity.model.ActivityAccountResult;
import com.lavyoung.marketforge.application.activity.model.ActivityAwardListCommand;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证活动抽奖接口层装配器的边界模型转换。
 */
class ActivityRaffleResponseAssemblerTest {

    private static final ActivityRaffleResponseAssembler ASSEMBLER =
            Mappers.getMapper(ActivityRaffleResponseAssembler.class);

    /**
     * Given 活动页查询请求，When 转换为应用命令，Then 用户和活动标识保持一致。
     */
    @Test
    void shouldConvertAwardListRequestToCommand() {
        // Given
        ActivityAwardListRequest request = new ActivityAwardListRequest("user-001", 100_301L);

        // When
        ActivityAwardListCommand command = ASSEMBLER.toCommand(request);

        // Then
        assertAll(
                () -> assertEquals(request.userId(), command.userId()),
                () -> assertEquals(request.activityId(), command.activityId())
        );
    }

    /**
     * Given 活动账户应用结果，When 转换为 API 响应，Then 总、日、月三组额度字段完整保留。
     */
    @Test
    void shouldConvertActivityAccountResultToResponse() {
        // Given
        ActivityAccountResult result = new ActivityAccountResult(10, 8, 3, 2, 7, 6);

        // When
        ActivityAccountResponse response = ASSEMBLER.toResponse(result);

        // Then
        assertAll(
                () -> assertEquals(result.totalCount(), response.totalCount()),
                () -> assertEquals(result.totalCountSurplus(), response.totalCountSurplus()),
                () -> assertEquals(result.dayCount(), response.dayCount()),
                () -> assertEquals(result.dayCountSurplus(), response.dayCountSurplus()),
                () -> assertEquals(result.monthCount(), response.monthCount()),
                () -> assertEquals(result.monthCountSurplus(), response.monthCountSurplus())
        );
    }
}
