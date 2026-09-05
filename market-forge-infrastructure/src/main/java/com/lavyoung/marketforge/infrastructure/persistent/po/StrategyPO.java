package com.lavyoung.marketforge.infrastructure.persistent.po;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 抽奖策略表持久化对象。
 * <p>
 * 记录策略基础配置，并继承 {@link BasePO} 的审计字段。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0-SNAPSHOT
 */
@Getter
@Setter
@NoArgsConstructor
public class StrategyPO extends BasePO {
    /**
     * 数据库自增主键。
     */
    private Long id;
    /**
     * 策略业务标识。
     */
    private Long strategyId;
    /**
     * 策略描述。
     */
    private String strategyDesc;
    /**
     * 以分隔符连接的策略级规则模型编码。
     */
    private String ruleModels;

}
