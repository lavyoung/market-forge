package com.lavyoung.marketforge.infrastructure.persistent.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 抽奖策略根配置持久化对象。
 *
 * <p>映射 {@code strategy} 表，保存策略基础配置、生命周期、乐观锁和审计操作人。
 * 策略奖品和策略规则分别由 {@link StrategyAwardPO} 与 {@link StrategyRulePO} 映射。</p>
 *
 * <p>本类是 MyBatis 持久化模型，需要保持可变性，因此不使用 {@code record}。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("strategy")
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

    /**
     * 策略生命周期状态：draft、active 或 disabled。
     */
    private String state;

    /**
     * 乐观锁版本，从 1 开始。
     */
    private Integer version;

    /**
     * 策略创建人标识。
     */
    private String createdBy;

    /**
     * 最后修改、发布或停用策略的操作人标识。
     */
    private String updatedBy;
}
