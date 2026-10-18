package com.lavyoung.marketforge.application.activity.service.impl;

import com.lavyoung.marketforge.application.activity.model.ActivityOrderSearchItem;
import com.lavyoung.marketforge.application.activity.model.ActivityOrderSearchQuery;
import com.lavyoung.marketforge.application.activity.query.IActivityOrderReader;
import com.lavyoung.marketforge.application.activity.service.IActivityOrderSearchService;
import com.lavyoung.marketforge.types.model.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 活动订单运营查询应用服务默认实现。
 *
 * <p>负责守护应用服务输入，并将只读查询委派给活动订单读取端口。
 * Elasticsearch 查询构造、索引选择和文档转换均由 Infrastructure 层负责。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/18
 */
@Service
@RequiredArgsConstructor
public class ActivityOrderSearchServiceImpl implements IActivityOrderSearchService {

    private final IActivityOrderReader activityOrderReader;

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResult<ActivityOrderSearchItem> search(ActivityOrderSearchQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        return activityOrderReader.search(query);
    }
}
