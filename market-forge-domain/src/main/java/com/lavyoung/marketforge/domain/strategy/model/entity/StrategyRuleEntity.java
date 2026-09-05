package com.lavyoung.marketforge.domain.strategy.model.entity;

import com.lavyoung.marketforge.types.common.Constants;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import lombok.Builder;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 抽奖策略规则实体。
 * <p>
 * 描述策略级或奖品级规则，并提供对权重规则配置值的结构化解析能力。
 *
 * @param strategyId 策略标识
 * @param awardId    奖品标识；策略级规则可为空
 * @param ruleType   规则类型：1 为策略规则，2 为奖品规则
 * @param ruleModel  规则模型编码
 * @param ruleValue  规则配置值
 * @param ruleDesc   规则描述
 * @param createTime 创建时间
 * @param updateTime 更新时间
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/08/31
 */
@Builder
public record StrategyRuleEntity(
        Long strategyId,
        Long awardId,
        Integer ruleType,
        String ruleModel,
        String ruleValue,
        String ruleDesc,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {

    /**
     * 将规则值解析为“权重门槛—可选奖品标识列表”的映射。
     * <p>
     * 规则值格式为 {@code 权重值:奖品ID/奖品ID;权重值:奖品ID}；
     * 当前规则无需解析或规则值为空时返回空映射。
     *
     * @return 权重门槛到奖品标识列表的映射，不返回 {@code null}
     * @throws NumberFormatException     规则值中的奖品标识不是有效数字时抛出
     * @throws IndexOutOfBoundsException 规则值缺少权重门槛或奖品列表时抛出
     */
    public Map<String, List<Long>> ruleWeightValues() {
        if (!RuleModel.WEIGHT.getCode().equals(ruleModel)) {
            return Map.of();
        }
        if (StringUtils.isBlank(ruleValue)) {
            return Map.of();
        }
        return Arrays.stream(ruleValue.split(Constants.SEMICOLON)).map(e -> Arrays.stream(e.split(Constants.COLON)).toList())
                .collect(Collectors.toUnmodifiableMap(
                        parts -> parts.get(0),
                        parts -> Arrays.stream(parts.get(1).split(Constants.SPLIT)).map(Long::valueOf).toList()
                ));
    }
}
