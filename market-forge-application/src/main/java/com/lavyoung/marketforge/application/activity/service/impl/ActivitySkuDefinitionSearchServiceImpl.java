package com.lavyoung.marketforge.application.activity.service.impl;

import com.lavyoung.marketforge.application.activity.model.ActivitySkuDefinitionResult;
import com.lavyoung.marketforge.application.activity.model.ActivitySkuDefinitionSearchQuery;
import com.lavyoung.marketforge.application.activity.query.IActivitySkuDefinitionReader;
import com.lavyoung.marketforge.application.activity.service.IActivitySkuDefinitionSearchService;
import com.lavyoung.marketforge.types.model.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * 活动 SKU 运营查询应用服务默认实现。
 *
 * <p>负责声明只读事务并守护查询输入，分页 SQL 和持久化对象转换由基础设施读取适配器完成。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/27
 */
@Service
@RequiredArgsConstructor
public class ActivitySkuDefinitionSearchServiceImpl implements IActivitySkuDefinitionSearchService {

    private final IActivitySkuDefinitionReader activitySkuDefinitionReader;

    @Override
    @Transactional(readOnly = true)
    public PageResult<ActivitySkuDefinitionResult> search(ActivitySkuDefinitionSearchQuery query) {
        return activitySkuDefinitionReader.search(Objects.requireNonNull(query, "query must not be null"));
    }
}
