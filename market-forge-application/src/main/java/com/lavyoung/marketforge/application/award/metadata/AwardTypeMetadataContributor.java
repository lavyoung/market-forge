package com.lavyoung.marketforge.application.award.metadata;

import com.lavyoung.marketforge.application.award.model.AwardTypeMetadata;

/**
 * 奖品类型元数据贡献者。
 *
 * <p>每种可运营配置的奖品类型提供一个实现。新增券码、实物或第三方奖品时，
 * 通过增加贡献者扩展元数据目录，不需要修改公共查询服务。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/24
 */
public interface AwardTypeMetadataContributor {

    /**
     * 返回当前奖品类型的完整配置元数据。
     *
     * @return 非空且已经过自身完整性校验的奖品类型元数据
     */
    AwardTypeMetadata metadata();
}
