package com.lavyoung.marketforge.infrastructure.persistent.query;

import com.lavyoung.marketforge.application.award.model.AwardDefinitionResult;
import com.lavyoung.marketforge.application.award.model.AwardDefinitionSearchQuery;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.AwardDefinitionReadModelAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.dao.IAwardDao;
import com.lavyoung.marketforge.infrastructure.persistent.model.AwardDefinitionSearchParam;
import com.lavyoung.marketforge.infrastructure.persistent.po.AwardPO;
import com.lavyoung.marketforge.types.model.PageRequest;
import com.lavyoung.marketforge.types.model.PageResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 奖品定义运营查询读取适配器测试。
 *
 * <p>覆盖分页参数下沉、正常读模型返回、零记录短路、超出末页以及非法空参数，
 * 防止运营查询出现额外 SQL、分页元数据丢失或基础设施类型泄漏。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class AwardDefinitionReaderTest {

    @Mock
    private IAwardDao awardDao;

    @Mock
    private AwardDefinitionReadModelAssembler readModelAssembler;

    @InjectMocks
    private AwardDefinitionReader reader;

    /**
     * Given 有匹配数据的第二页查询，When 执行搜索，Then 应下沉正确偏移量并返回完整分页结果。
     */
    @Test
    void shouldSearchAwardDefinitionsWithStablePageMetadata() {
        // Given
        AwardDefinitionSearchQuery query = query(new PageRequest(2, 20));
        AwardPO po = new AwardPO();
        AwardDefinitionResult expected = result(1001L, "credit_100");
        when(awardDao.countAwardDefinitions(org.mockito.ArgumentMatchers.any())).thenReturn(21L);
        when(awardDao.queryAwardDefinitionPage(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(po));
        when(readModelAssembler.toResults(List.of(po))).thenReturn(List.of(expected));

        // When
        PageResult<AwardDefinitionResult> actual = reader.search(query);

        // Then
        ArgumentCaptor<AwardDefinitionSearchParam> captor = ArgumentCaptor.forClass(AwardDefinitionSearchParam.class);
        verify(awardDao).countAwardDefinitions(captor.capture());
        verify(awardDao).queryAwardDefinitionPage(captor.capture());
        assertAll(
                () -> assertEquals("credit", captor.getAllValues().get(0).awardCode()),
                () -> assertEquals("积分", captor.getAllValues().get(0).awardName()),
                () -> assertEquals("internal_credit", captor.getAllValues().get(0).awardType()),
                () -> assertEquals("active", captor.getAllValues().get(0).state()),
                () -> assertEquals(20L, captor.getAllValues().get(0).offset()),
                () -> assertEquals(20, captor.getAllValues().get(0).pageSize()),
                () -> assertEquals(captor.getAllValues().get(0), captor.getAllValues().get(1)),
                () -> assertEquals(List.of(expected), actual.records()),
                () -> assertEquals(21L, actual.total()),
                () -> assertEquals(2, actual.pageNumber()),
                () -> assertEquals(20, actual.pageSize())
        );
    }

    /**
     * Given 统计结果为零，When 执行搜索，Then 应返回空页且不执行无意义的分页 SQL 和装配。
     */
    @Test
    void shouldShortCircuitPageQueryWhenNoAwardDefinitionMatches() {
        // Given
        AwardDefinitionSearchQuery query = query(new PageRequest(1, 20));
        when(awardDao.countAwardDefinitions(org.mockito.ArgumentMatchers.any())).thenReturn(0L);

        // When
        PageResult<AwardDefinitionResult> actual = reader.search(query);

        // Then
        assertAll(
                () -> assertEquals(List.of(), actual.records()),
                () -> assertEquals(0L, actual.total()),
                () -> assertEquals(1, actual.pageNumber()),
                () -> assertEquals(20, actual.pageSize())
        );
        verify(awardDao, never()).queryAwardDefinitionPage(org.mockito.ArgumentMatchers.any());
        verifyNoInteractions(readModelAssembler);
    }

    /**
     * Given 总数存在但请求页超过末页，When 数据库返回空列表，Then 应保留总数并返回空记录。
     */
    @Test
    void shouldKeepTotalWhenRequestedPageIsOutOfRange() {
        // Given
        AwardDefinitionSearchQuery query = query(new PageRequest(3, 20));
        when(awardDao.countAwardDefinitions(org.mockito.ArgumentMatchers.any())).thenReturn(21L);
        when(awardDao.queryAwardDefinitionPage(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        when(readModelAssembler.toResults(List.of())).thenReturn(List.of());

        // When
        PageResult<AwardDefinitionResult> actual = reader.search(query);

        // Then
        assertAll(
                () -> assertEquals(List.of(), actual.records()),
                () -> assertEquals(21L, actual.total()),
                () -> assertEquals(3, actual.pageNumber()),
                () -> assertEquals(20, actual.pageSize())
        );
    }

    /**
     * Given 查询条件为空，When 执行搜索，Then 应在访问数据库之前立即拒绝请求。
     */
    @Test
    void shouldRejectNullQueryBeforeDatabaseAccess() {
        // When & Then
        assertThrows(NullPointerException.class, () -> reader.search(null));
        verifyNoInteractions(awardDao, readModelAssembler);
    }

    private AwardDefinitionSearchQuery query(PageRequest page) {
        return new AwardDefinitionSearchQuery(" credit ", " 积分 ", " internal_credit ", " active ", page);
    }

    private AwardDefinitionResult result(Long awardId, String awardCode) {
        return new AwardDefinitionResult(awardId, awardCode, "100积分", "internal_credit", "automatic", null, "{\"amount\":100}", 1, "抽奖后自动发放100积分", "active", 2, "operator-001", "operator-002", LocalDateTime.of(2026, 10, 22, 10, 0), LocalDateTime.of(2026, 10, 22, 11, 0));
    }
}
