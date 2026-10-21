package com.lavyoung.marketforge.infrastructure.persistent.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 奖品定义持久化对象。
 *
 * <p>对应 {@code award} 表，保存运营后台维护的奖品基础定义、发奖类型、
 * 领取方式、履约配置、生命周期状态和乐观锁版本。</p>
 *
 * <p>该持久化对象同时兼容抽奖策略现有的奖品查询投影。
 * 策略域只使用奖品标识、业务键、发奖配置和描述；运营链路使用全部字段。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/21
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("award")
public class AwardPO extends BasePO {

    /**
     * 数据库自增主键。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 奖品业务标识。
     *
     * <p>由业务侧生成，抽奖策略通过该字段引用具体奖品。</p>
     */
    private Long awardId;

    /**
     * 奖品业务编码。
     *
     * <p>数据库沿用原有 {@code award_key} 字段，领域模型中对应 {@code awardCode}。</p>
     */
    private String awardKey;

    /**
     * 奖品展示名称。
     */
    private String awardName;

    /**
     * 奖品发放类型编码。
     *
     * <p>例如 {@code internal_credit}、{@code third_party_coupon}。</p>
     */
    private String awardType;

    /**
     * 奖品领取模式编码。
     *
     * <p>{@code automatic} 表示中奖后自动发放；
     * {@code manual} 表示等待用户补充领取信息。</p>
     */
    private String claimMode;

    /**
     * 外部履约供应商编码。
     *
     * <p>内部积分等平台自营奖品可以为空。</p>
     */
    private String providerCode;

    /**
     * 奖品发放配置内容。
     *
     * <p>使用 JSON 字符串保存，具体结构由对应的发奖处理器解释。</p>
     */
    private String awardConfig;

    /**
     * 奖品发放配置结构版本。
     */
    private Integer configVersion;

    /**
     * 奖品说明。
     */
    private String awardDesc;

    /**
     * 奖品定义生命周期状态。
     *
     * <p>可选值包括 {@code draft}、{@code active} 和 {@code disabled}。</p>
     */
    private String state;

    /**
     * 数据乐观锁版本。
     *
     * <p>草稿修改和状态变更时必须将该字段作为更新条件。</p>
     */
    private Integer version;

    /**
     * 创建人标识。
     */
    private String createdBy;

    /**
     * 最后修改人标识。
     */
    private String updatedBy;
}