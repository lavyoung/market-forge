package com.lavyoung.marketforge.application.award.service;

import com.lavyoung.marketforge.application.award.model.AwardDefinitionResult;
import com.lavyoung.marketforge.application.award.model.AwardDefinitionSearchQuery;
import com.lavyoung.marketforge.application.award.query.IAwardDefinitionReader;
import com.lavyoung.marketforge.application.award.service.impl.AwardDefinitionSearchServiceImpl;
import com.lavyoung.marketforge.types.model.PageRequest;
import com.lavyoung.marketforge.types.model.PageResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * {@link AwardDefinitionSearchServiceImpl} 单元测试。
 *
 * <p>验证应用服务只守护查询输入并委托读取端口，不在应用层构造 MyBatis 查询条件。</p>
 */
@ExtendWith(MockitoExtension.class)
class AwardDefinitionSearchServiceImplTest {

    @Mock
    private IAwardDefinitionReader awardDefinitionReader;

    @InjectMocks
    private AwardDefinitionSearchServiceImpl service;

    /**
     * 验证合法查询原样委托给读取端口并返回分页结果。
     */
    @Test
    void shouldDelegateSearchToReader() {
        // Given
        AwardDefinitionSearchQuery query = new AwardDefinitionSearchQuery(
                "credit_100", null, "internal_credit", "active", new PageRequest(1, 20));
        PageResult<AwardDefinitionResult> expected = PageResult.empty(1, 20);
        when(awardDefinitionReader.search(query)).thenReturn(expected);

        // When
        PageResult<AwardDefinitionResult> actual = service.search(query);

        // Then
        assertEquals(expected, actual);
        verify(awardDefinitionReader).search(query);
    }

    /**
     * 验证空查询在进入基础设施读取端口前被拒绝。
     */
    @Test
    void shouldRejectNullQueryBeforeCallingReader() {
        // Given / When / Then
        assertThrows(NullPointerException.class, () -> service.search(null));
        verifyNoInteractions(awardDefinitionReader);
    }
}
