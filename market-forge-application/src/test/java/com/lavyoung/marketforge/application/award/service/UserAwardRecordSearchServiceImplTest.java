package com.lavyoung.marketforge.application.award.service;

import com.lavyoung.marketforge.application.award.model.UserAwardRecordSearchItem;
import com.lavyoung.marketforge.application.award.model.UserAwardRecordSearchQuery;
import com.lavyoung.marketforge.application.award.query.IUserAwardRecordReader;
import com.lavyoung.marketforge.application.award.service.impl.UserAwardRecordSearchServiceImpl;
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
 * 用户中奖记录搜索应用服务测试。
 *
 * <p>验证应用服务只承担查询输入校验和用例编排，并将实际数据读取委派给
 * {@link IUserAwardRecordReader}。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/16
 */
@ExtendWith(MockitoExtension.class)
class UserAwardRecordSearchServiceImplTest {

    @Mock
    private IUserAwardRecordReader recordReader;

    @InjectMocks
    private UserAwardRecordSearchServiceImpl service;

    /**
     * 验证合法查询会原样委派给中奖记录读取器并返回分页结果。
     */
    @Test
    void shouldDelegateSearchToRecordReader() {
        // Given
        UserAwardRecordSearchQuery query = new UserAwardRecordSearchQuery(
                "user-001", null, null, null, null, 1, 20
        );
        PageResult<UserAwardRecordSearchItem> expected = new PageResult<>(List.of(), 0, 1, 20);
        when(recordReader.search(query)).thenReturn(expected);

        // When
        PageResult<UserAwardRecordSearchItem> actual = service.search(query);

        // Then
        assertEquals(expected, actual);
        verify(recordReader).search(query);
    }

    /**
     * 验证空查询不会进入外部中奖记录读取器。
     */
    @Test
    void shouldRejectNullQueryBeforeCallingRecordReader() {
        // Given / When / Then
        assertThrows(NullPointerException.class, () -> service.search(null));
        verifyNoInteractions(recordReader);
    }
}
