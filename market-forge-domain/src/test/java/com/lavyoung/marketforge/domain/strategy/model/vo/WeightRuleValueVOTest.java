package com.lavyoung.marketforge.domain.strategy.model.vo;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 权重规则配置值对象测试。
 *
 * <p>验证多档位排序、最高可用门槛匹配、标准化序列化以及重复门槛等异常边界。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
class WeightRuleValueVOTest {

    /**
     * Given 未按门槛排序的多个档位，When 解析和匹配，Then 应排序并选择不高于完成次数的最高档位。
     */
    @Test
    void shouldSortNormalizeAndMatchHighestWeightTier() {
        // Given & When
        WeightRuleValueVO ruleValue = WeightRuleValueVO.parse("20:200003;5:200001/200002");

        // Then
        assertAll(
                () -> assertEquals(List.of(5L, 20L), ruleValue.tiers().stream().map(WeightTierVO::threshold).toList()),
                () -> assertEquals(5L, ruleValue.match(19).orElseThrow().threshold()),
                () -> assertEquals(20L, ruleValue.match(20).orElseThrow().threshold()),
                () -> assertEquals(Map.of(5L, List.of(200001L, 200002L), 20L, List.of(200003L)), ruleValue.toTierMap()),
                () -> assertEquals("5:200001/200002;20:200003", ruleValue.toConfigValue()),
                () -> assertThrows(UnsupportedOperationException.class,
                        () -> ruleValue.tiers().add(new WeightTierVO(30, List.of(200004L))))
        );
    }

    /**
     * Given 兼容数据带有一个末尾分号，When 解析，Then 应接受并输出不带末尾分号的标准配置。
     */
    @Test
    void shouldNormalizeSingleTrailingSemicolon() {
        WeightRuleValueVO ruleValue = WeightRuleValueVO.parse("5:200001;");

        assertEquals("5:200001", ruleValue.toConfigValue());
    }

    /**
     * Given 用户次数低于第一个档位，When 匹配，Then 应返回空结果并交给默认奖池继续处理。
     */
    @Test
    void shouldReturnEmptyBeforeFirstThreshold() {
        WeightRuleValueVO ruleValue = WeightRuleValueVO.parse("5:200001");

        assertFalse(ruleValue.match(4).isPresent());
    }

    /**
     * Given 权重规则包含重复门槛或空档位，When 解析，Then 应拒绝存在覆盖歧义的配置。
     */
    @Test
    void shouldRejectInvalidWeightRuleValue() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> WeightRuleValueVO.parse(null)),
                () -> assertThrows(IllegalArgumentException.class, () -> WeightRuleValueVO.parse("")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> WeightRuleValueVO.parse("5:200001;5:200002")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> WeightRuleValueVO.parse("5:200001;;20:200002")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> WeightRuleValueVO.parse("5:200001;;")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> WeightRuleValueVO.parse("5:200001").match(-1))
        );
    }
}
