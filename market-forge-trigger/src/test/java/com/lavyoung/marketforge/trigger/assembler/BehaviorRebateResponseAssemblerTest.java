package com.lavyoung.marketforge.trigger.assembler;

import com.lavyoung.marketforge.api.behavior.request.BehaviorRebateRequest;
import com.lavyoung.marketforge.api.behavior.response.BehaviorRebateResponse;
import com.lavyoung.marketforge.application.behavior.model.BehaviorRebateOrderCreateResult;
import com.lavyoung.marketforge.application.behavior.model.CreateBehaviorRebateOrderCommand;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证用户行为返利接口层装配器的协议模型和应用模型转换。
 */
class BehaviorRebateResponseAssemblerTest {

    private static final BehaviorRebateResponseAssembler ASSEMBLER =
            Mappers.getMapper(BehaviorRebateResponseAssembler.class);

    /**
     * Given 合法的 API 请求，When 转换为应用命令，Then 字段应按应用层边界模型原样传递。
     */
    @Test
    void shouldConvertRequestToCommand() {
        // Given
        BehaviorRebateRequest request =
                new BehaviorRebateRequest("user-001", "sign", "sign-20261010-user-001");

        // When
        CreateBehaviorRebateOrderCommand command = ASSEMBLER.toCommand(request);

        // Then
        assertAll(
                () -> assertEquals(request.userId(), command.userId()),
                () -> assertEquals(request.behaviorType(), command.behaviorType()),
                () -> assertEquals(request.outBusinessNo(), command.outBusinessNo())
        );
    }

    /**
     * Given 应用层返回返利订单创建结果，When 转换为 API 响应，Then 订单号和事件号不能丢失。
     */
    @Test
    void shouldConvertResultToResponse() {
        // Given
        BehaviorRebateOrderCreateResult result = new BehaviorRebateOrderCreateResult(
                List.of("RO202610100001"),
                List.of("event-001")
        );

        // When
        BehaviorRebateResponse response = ASSEMBLER.toResponse(result);

        // Then
        assertAll(
                () -> assertEquals(result.orderIds(), response.orderIds()),
                () -> assertEquals(result.eventIds(), response.eventIds())
        );
    }
}
