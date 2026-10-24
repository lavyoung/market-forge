package com.lavyoung.marketforge.application.award.service;

import com.lavyoung.marketforge.application.award.model.AwardTypeMetadata;

import java.util.List;

/**
 * 奖品类型运营元数据查询应用服务。
 *
 * <p>向入站适配器提供系统当前真实支持的奖品类型。返回结果已经通过
 * 发奖处理器、领取模式和配置协议的启动期一致性校验。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
public interface IAwardTypeMetadataApplicationService {

    /**
     * 查询系统当前支持的全部奖品类型。
     *
     * <p>结果按照奖品类型编码稳定排序，并且不可修改。</p>
     *
     * @return 可供运营人员创建和编辑奖品定义的类型元数据
     */
    List<AwardTypeMetadata> listSupportedTypes();
}
