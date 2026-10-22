package com.lavyoung.marketforge.application.activity.service.impl;

import com.lavyoung.marketforge.application.activity.model.ActivityDefinitionResult;
import com.lavyoung.marketforge.application.activity.model.ActivityDefinitionSearchQuery;
import com.lavyoung.marketforge.application.activity.query.IActivityDefinitionReader;
import com.lavyoung.marketforge.application.activity.service.IActivityDefinitionSearchService;
import com.lavyoung.marketforge.types.model.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * 活动定义运营查询应用服务默认实现。
 *
 * <p>负责校验应用查询输入、声明只读事务，并将实际读取委托给
 * {@link IActivityDefinitionReader}。分页 SQL、PO 转换和数据库访问全部由
 * Infrastructure 层负责。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
@Service
@RequiredArgsConstructor
public class ActivityDefinitionSearchServiceImpl implements IActivityDefinitionSearchService {

    private final IActivityDefinitionReader activityDefinitionReader;

    @Override
    @Transactional(readOnly = true)
    public PageResult<ActivityDefinitionResult> search(ActivityDefinitionSearchQuery query) {
        ActivityDefinitionSearchQuery validQuery = Objects.requireNonNull(query, "query must not be null");
        return activityDefinitionReader.search(validQuery);
    }
}
