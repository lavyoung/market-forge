package com.lavyoung.marketforge.application.activity.service.impl;

import com.lavyoung.marketforge.application.activity.model.ActivityCountDefinitionResult;
import com.lavyoung.marketforge.application.activity.model.ActivityCountDefinitionSearchQuery;
import com.lavyoung.marketforge.application.activity.query.IActivityCountDefinitionReader;
import com.lavyoung.marketforge.application.activity.service.IActivityCountDefinitionSearchService;
import com.lavyoung.marketforge.types.model.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * 活动次数配置运营查询应用服务默认实现。
 *
 * <p>负责声明只读事务和守护查询输入，分页 SQL 与持久化对象转换由基础设施读取适配器完成。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/26
 */
@Service
@RequiredArgsConstructor
public class ActivityCountDefinitionSearchServiceImpl implements IActivityCountDefinitionSearchService {

    private final IActivityCountDefinitionReader activityCountDefinitionReader;

    @Override
    @Transactional(readOnly = true)
    public PageResult<ActivityCountDefinitionResult> search(ActivityCountDefinitionSearchQuery query) {
        return activityCountDefinitionReader.search(Objects.requireNonNull(query, "query must not be null"));
    }
}
