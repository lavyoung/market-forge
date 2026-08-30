package com.lavyoung.marketforge.infrastructure.persistent.po;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 奖品持久化对象
 *
 * @author lavyoung
 * @version 1.0.0-SNAPSHOT
 * @email lavyoung1325@outlook.com
 */
@Getter
@Setter
@NoArgsConstructor
public class AwardPO {

    /**
     * 自增id
     *
     */
    private Long id;
    /**
     * 奖品id
     *
     */
    private Long awardId;
    /**
     * 奖品key
     *
     */
    private String awardKey;
    /**
     * 奖品配置
     *
     */
    private String awardConfig;
    /**
     * 奖品描述
     *
     */
    private String awardDesc;
    /**
     * 创建时间
     *
     */
    private LocalDateTime createTime;
    /**
     * 更新时间
     *
     */
    private LocalDateTime updateTime;
}
