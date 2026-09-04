package com.lavyoung.marketforge.domain.strategy.model.entity;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 奖品
 *
 * @author lavyoung
 * @version 1.0.0
 * @email lavyoung1325@outlook.com
 * @date 2026/08/31
 */
@Data
@Builder
public class AwardEntity {

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
