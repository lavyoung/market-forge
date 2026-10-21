package com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 验证 {@link ProbabilitySnapshotKey} 对概率空间和快照版本的约束。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
class ProbabilitySnapshotKeyTest {

    /**
     * Given 合法装配键和版本，When 创建快照键，Then 完整保留不可变快照身份。
     */
    @Test
    void shouldCreateValidSnapshotKey() {
        // Given & When
        ProbabilitySnapshotKey snapshotKey = new ProbabilitySnapshotKey("100001_rule_weight_10", 7L);

        // Then
        assertEquals("100001_rule_weight_10", snapshotKey.assembleKey());
        assertEquals(7L, snapshotKey.version());
    }

    /**
     * Given 空或空白装配键，When 创建快照键，Then 拒绝无法定位概率空间的快照。
     */
    @Test
    void shouldRejectBlankAssembleKey() {
        assertThrows(IllegalArgumentException.class, () -> new ProbabilitySnapshotKey(" ", 1L));
        assertThrows(IllegalArgumentException.class, () -> new ProbabilitySnapshotKey(null, 1L));
    }

    /**
     * Given 非正版本，When 创建快照键，Then 拒绝不具备单调版本语义的快照。
     */
    @Test
    void shouldRejectNonPositiveVersion() {
        assertThrows(IllegalArgumentException.class, () -> new ProbabilitySnapshotKey("100001", 0L));
        assertThrows(IllegalArgumentException.class, () -> new ProbabilitySnapshotKey("100001", -1L));
    }
}
