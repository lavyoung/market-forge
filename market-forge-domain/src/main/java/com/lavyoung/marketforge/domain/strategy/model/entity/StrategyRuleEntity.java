package com.lavyoung.marketforge.domain.strategy.model.entity;

import com.lavyoung.marketforge.types.common.Constants;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import lombok.Builder;
import lombok.Data;
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
 * @author lavyoung
 * @version 1.0.0
 * @email lavyoung1325@outlook.com
 * @date 2026/08/31
 */
@Data
@Builder
public class StrategyRuleEntity {

    /**
     * 策略id
     */
    private Long strategyId;
    /**
     * 奖品id
     */
    private Long awardId;
    /**
     * 规则类型：【1-策略规则、2-奖品规则】
     */
    private Integer ruleType;
    /**
     * 抽奖规则类型:【rule_lock】
     * com.lavyoung.marketforge.types.domain.strategy.RuleModel
     */
    private String ruleModel;
    /**
     * 抽奖规则比值
     * <p>
     * rule_lock: 配置抽奖次数解锁某个奖品
     * rule_weight: 达到或者消耗积分到达多少 可选抽奖范围 格式： 积分值1:奖品编号1/奖品编号2/奖品编号3;积分值2:奖品编号2/奖品编号3
     * rule_random: 随机奖
     * rule_luck_award: 兜底的幸运星
     *
     */
    private String ruleValue;
    /**
     * 抽奖规则描述
     */
    private String ruleDesc;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;


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
    public Map<String, List<Long>> getRuleWeightValues() {
        if (RuleModel.WEIGHT.getCode().equals(ruleModel)) {
            return Map.of();
        }
        if (StringUtils.isBlank(ruleValue)) {
            return Map.of();
        }
        return Arrays.stream(ruleValue.split(Constants.SEMICOLON)).map(e -> Arrays.stream(e.split(Constants.COLON)).toList())
                .collect(Collectors.toMap(x -> x.get(0), v -> Arrays.stream(v.get(1).split(Constants.SPLIT)).map(Long::valueOf).toList()));
    }
}
