package com.lavyoung.marketforge.infrastructure.persistent.po;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 抽奖策略奖品关联表持久化对象。
 * <p>
 * 记录策略内奖品的库存、概率、规则及展示顺序，并继承 {@link BasePO} 的审计字段。
 *
 * @author lavyoung
 * @version 1.0.0-SNAPSHOT
 * @email lavyoung1325@outlook.com
 */
@Getter
@Setter
@NoArgsConstructor
public class StrategyAwardPO extends BasePO {
    /**
     * 数据库自增主键。
     */
    private Long id;
    /**
     * 抽奖策略业务标识。
     */
    private Long strategyId;
    /**
     * 奖品业务标识。
     */
    private Long awardId;
    /**
     * 奖品标题。
     */
    private String awardTitle;
    /**
     * 奖品副标题。
     */
    private String awardSubtitle;
    /**
     * 奖品库存总量。
     */
    private Integer awardCount;
    /**
     * 奖品库存剩余量。
     */
    private Integer awardCountSurplus;
    /**
     * 奖品中奖概率。
     */
    private BigDecimal awardRate;
    /**
     * 以分隔符连接的规则模型编码。
     */
    private String ruleModels;
    /**
     * 奖品展示顺序。
     */
    private Integer sort;

}
