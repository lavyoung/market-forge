package com.lavyoung.marketforge.domain.strategy.model.vo;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 权重奖池单档位值对象测试。
 *
 * <p>验证次数门槛和候选奖品集合能够稳定解析，同时拒绝空奖池、重复奖品和非法标识。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
class WeightTierVOTest {

    /**
     * Given 合法单档位配置，When 解析并序列化，Then 应保留门槛及奖品顺序。
     */
    @Test
    void shouldParseAndSerializeWeightTier() {
        WeightTierVO tier = WeightTierVO.parse(" 10 : 200001 / 200002 ");

        assertAll(
                () -> assertEquals(10L, tier.threshold()),
                () -> assertEquals(List.of(200001L, 200002L), tier.awardIds()),
                () -> assertEquals("10:200001/200002", tier.toConfigValue()),
                () -> assertThrows(UnsupportedOperationException.class, () -> tier.awardIds().add(200003L))
        );
    }

    /**
     * Given 档位包含空值、重复值或非正数，When 创建或解析，Then 应拒绝非法子奖池。
     */
    @Test
    void shouldRejectInvalidWeightTier() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> WeightTierVO.parse(null)),
                () -> assertThrows(IllegalArgumentException.class, () -> WeightTierVO.parse("10")),
                () -> assertThrows(IllegalArgumentException.class, () -> WeightTierVO.parse("-1:200001")),
                () -> assertThrows(IllegalArgumentException.class, () -> WeightTierVO.parse("10:award")),
                () -> assertThrows(IllegalArgumentException.class, () -> WeightTierVO.parse("10:200001/200001")),
                () -> assertThrows(IllegalArgumentException.class, () -> WeightTierVO.parse("10:0")),
                () -> assertThrows(IllegalArgumentException.class, () -> new WeightTierVO(10, List.of()))
        );
    }
}
