package com.lavyoung.marketforge.application.award.service.impl;

import com.lavyoung.marketforge.application.award.model.UserAwardRecordSearchItem;
import com.lavyoung.marketforge.application.award.model.UserAwardRecordSearchQuery;
import com.lavyoung.marketforge.application.award.query.IUserAwardRecordReader;
import com.lavyoung.marketforge.application.award.service.IUserAwardRecordSearchService;
import com.lavyoung.marketforge.types.model.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 用户中奖记录搜索应用服务默认实现。
 *
 * <p>负责校验应用层查询输入并将查询委派给中奖记录读取器。
 * Elasticsearch 查询构造、索引选择和文档映射由基础设施实现负责。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/15
 */
@Service
@RequiredArgsConstructor
public class UserAwardRecordSearchServiceImpl implements IUserAwardRecordSearchService {

    private final IUserAwardRecordReader recordReader;

    @Override
    public PageResult<UserAwardRecordSearchItem> search(UserAwardRecordSearchQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        return recordReader.search(query);
    }
}
