package com.lavyoung.marketforge.infrastructure.persistent.po;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 奖品表持久化对象。
 * <p>
 * 继承 {@link BasePO} 统一维护创建时间和更新时间。
 *
 * @author lavyoung
 * @version 1.0.0-SNAPSHOT
 * @email lavyoung1325@outlook.com
 */
@Getter
@Setter
@NoArgsConstructor
public class AwardPO extends BasePO {

    /**
     * 数据库自增主键。
     */
    private Long id;
    /**
     * 奖品业务标识。
     */
    private Long awardId;
    /**
     * 奖品业务键。
     */
    private String awardKey;
    /**
     * 奖品发放配置。
     */
    private String awardConfig;
    /**
     * 奖品描述。
     */
    private String awardDesc;
}
