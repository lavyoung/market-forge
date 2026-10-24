package com.lavyoung.marketforge.application.award.service.impl;

import com.lavyoung.marketforge.application.award.metadata.AwardTypeMetadataRegistry;
import com.lavyoung.marketforge.application.award.model.AwardTypeMetadata;
import com.lavyoung.marketforge.application.award.service.IAwardTypeMetadataApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 奖品类型运营元数据查询应用服务实现。
 *
 * <p>本实现只负责暴露已经完成启动期校验的奖品能力，不重新扫描 Spring Bean，
 * 也不访问数据库或重新执行配置校验。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
@Service
@RequiredArgsConstructor
public class AwardTypeMetadataApplicationServiceImpl implements IAwardTypeMetadataApplicationService {

    private final AwardTypeMetadataRegistry metadataRegistry;

    @Override
    public List<AwardTypeMetadata> listSupportedTypes() {
        return metadataRegistry.listAll();
    }
}
