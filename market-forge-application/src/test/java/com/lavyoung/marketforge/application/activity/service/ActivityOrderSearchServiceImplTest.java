package com.lavyoung.marketforge.application.activity.service;

import com.lavyoung.marketforge.application.activity.model.ActivityOrderSearchItem;
import com.lavyoung.marketforge.application.activity.model.ActivityOrderSearchQuery;
import com.lavyoung.marketforge.application.activity.query.IActivityOrderReader;
import com.lavyoung.marketforge.application.activity.service.impl.ActivityOrderSearchServiceImpl;
import com.lavyoung.marketforge.types.model.PageResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * 活动订单运营查询应用服务测试。
 *
 * <p>验证应用服务只承担输入守护和查询用例编排，并将实际订单读取委派给
 * {@link IActivityOrderReader}。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/18
 */
@ExtendWith(MockitoExtension.class)
class ActivityOrderSearchServiceImplTest {

    @Mock
    private IActivityOrderReader activityOrderReader;

    @InjectMocks
    private ActivityOrderSearchServiceImpl service;

    /**
     * 验证合法查询会原样委派给活动订单读取器并返回其分页结果。
     */
    @Test
    void shouldDelegateSearchToActivityOrderReader() {
        // Given
        ActivityOrderSearchQuery query = new ActivityOrderSearchQuery(
                "user-001", 100301L, null, "used", null, null, 1, 20
        );
        PageResult<ActivityOrderSearchItem> expected = new PageResult<>(List.of(), 0, 1, 20);
        when(activityOrderReader.search(query)).thenReturn(expected);

        // When
        PageResult<ActivityOrderSearchItem> actual = service.search(query);

        // Then
        assertEquals(expected, actual);
        verify(activityOrderReader).search(query);
    }

    /**
     * 验证空查询会在应用服务边界被拒绝，并且不会调用外部读取器。
     */
    @Test
    void shouldRejectNullQueryBeforeCallingActivityOrderReader() {
        // Given / When / Then
        assertThrows(NullPointerException.class, () -> service.search(null));
        verifyNoInteractions(activityOrderReader);
    }
}
