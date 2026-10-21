package com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model;

import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.ProbabilityAlgorithmType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 {@link ProbabilitySnapshotPointer} 的标识、算法类型和版本不变量。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
class ProbabilitySnapshotPointerTest {

    /**
     * Given 合法装配键、算法类型和版本，When 创建指针，Then 完整保留快照身份。
     */
    @Test
    void shouldCreateValidSnapshotPointer() {
        // Given & When
        ProbabilitySnapshotPointer pointer = new ProbabilitySnapshotPointer(
                "100001_rule_weight_10", ProbabilityAlgorithmType.ALIAS_METHOD, 7L);

        // Then
        assertEquals("100001_rule_weight_10", pointer.assembleKey());
        assertSame(ProbabilityAlgorithmType.ALIAS_METHOD, pointer.algorithmType());
        assertEquals(7L, pointer.version());
    }

    /**
     * Given 当前生效指针，When 转换为数据查询键，Then 仅保留同一装配键和版本。
     */
    @Test
    void shouldCreateSnapshotKeyFromPointer() {
        // Given
        ProbabilitySnapshotPointer pointer = new ProbabilitySnapshotPointer(
                "100001_rule_weight_10", ProbabilityAlgorithmType.BINARY_SEARCH, 8L);

        // When
        ProbabilitySnapshotKey snapshotKey = pointer.snapshotKey();

        // Then
        assertEquals("100001_rule_weight_10", snapshotKey.assembleKey());
        assertEquals(8L, snapshotKey.version());
    }

    /**
     * Given 空或空白装配键，When 创建指针，Then 拒绝无法定位的概率快照。
     */
    @Test
    void shouldRejectBlankAssembleKey() {
        assertThrows(IllegalArgumentException.class,
                () -> new ProbabilitySnapshotPointer(" ", ProbabilityAlgorithmType.ALIAS_METHOD, 1L));
        assertThrows(IllegalArgumentException.class,
                () -> new ProbabilitySnapshotPointer(null, ProbabilityAlgorithmType.ALIAS_METHOD, 1L));
    }

    /**
     * Given 空算法类型，When 创建指针，Then 拒绝无法选择派发算法的快照。
     */
    @Test
    void shouldRejectNullAlgorithmType() {
        assertThrows(NullPointerException.class, () -> new ProbabilitySnapshotPointer("100001", null, 1L));
    }

    /**
     * Given 非正版本，When 创建指针，Then 拒绝不具备单调版本语义的快照。
     */
    @Test
    void shouldRejectNonPositiveVersion() {
        assertThrows(IllegalArgumentException.class,
                () -> new ProbabilitySnapshotPointer("100001", ProbabilityAlgorithmType.ALIAS_METHOD, 0L));
        assertThrows(IllegalArgumentException.class,
                () -> new ProbabilitySnapshotPointer("100001", ProbabilityAlgorithmType.ALIAS_METHOD, -1L));
    }
}
