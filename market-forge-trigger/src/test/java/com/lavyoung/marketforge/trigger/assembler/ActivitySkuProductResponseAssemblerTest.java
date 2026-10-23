package com.lavyoung.marketforge.trigger.assembler;

import com.lavyoung.marketforge.api.activity.response.ActivitySkuProductResponse;
import com.lavyoung.marketforge.application.activity.model.ActivitySkuProductResult;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证活动 SKU 商品应用结果到 HTTP 响应的协议边界转换。
 */
class ActivitySkuProductResponseAssemblerTest {

    private static final ActivitySkuProductResponseAssembler ASSEMBLER =
            Mappers.getMapper(ActivitySkuProductResponseAssembler.class);

    /**
     * Given 超出 JavaScript 安全整数范围的 SKU 和活动标识，
     * When 转换为 API 响应，Then 标识无损转为字符串且可计算数量保持数值类型。
     */
    @Test
    void shouldConvertLongIdentifiersToStringsAndPreserveNumericAmounts() {
        // Given
        ActivitySkuProductResult result = new ActivitySkuProductResult(
                9_007_199_254_740_993L,
                9_007_199_254_740_994L,
                100,
                80,
                10,
                2,
                6,
                100L
        );

        // When
        ActivitySkuProductResponse response = ASSEMBLER.toResponse(result);

        // Then
        assertAll(
                () -> assertEquals("9007199254740993", response.sku()),
                () -> assertEquals("9007199254740994", response.activityId()),
                () -> assertEquals(result.stockCount(), response.stockCount()),
                () -> assertEquals(result.stockCountSurplus(), response.stockCountSurplus()),
                () -> assertEquals(result.totalCount(), response.totalCount()),
                () -> assertEquals(result.dayCount(), response.dayCount()),
                () -> assertEquals(result.monthCount(), response.monthCount()),
                () -> assertEquals(result.creditAmount(), response.creditAmount())
        );
    }
}
