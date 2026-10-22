package com.lavyoung.marketforge.application.activity.service.impl;

import com.lavyoung.marketforge.application.activity.model.ActivityStageResult;
import com.lavyoung.marketforge.application.activity.model.ActivityStageSearchQuery;
import com.lavyoung.marketforge.application.activity.query.IActivityStageReader;
import com.lavyoung.marketforge.application.activity.service.IActivityStageSearchService;
import com.lavyoung.marketforge.types.model.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * 活动展台运营查询应用服务默认实现。
 *
 * <p>该服务负责声明只读事务和守护查询输入，具体 SQL、分页偏移量以及 PO 转换均由
 * Infrastructure 读取适配器完成。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
@Service
@RequiredArgsConstructor
public class ActivityStageSearchServiceImpl implements IActivityStageSearchService {

    private final IActivityStageReader activityStageReader;

    @Override
    @Transactional(readOnly = true)
    public PageResult<ActivityStageResult> search(ActivityStageSearchQuery query) {
        return activityStageReader.search(Objects.requireNonNull(query, "query must not be null"));
    }
}
