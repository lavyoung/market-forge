package com.lavyoung.marketforge.infrastructure.persistent.query;

import com.lavyoung.marketforge.application.activity.model.ActivityCountDefinitionResult;
import com.lavyoung.marketforge.application.activity.model.ActivityCountDefinitionSearchQuery;
import com.lavyoung.marketforge.application.activity.query.IActivityCountDefinitionReader;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.activity.ActivityCountDefinitionReadModelAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.dao.activity.IActivityCountDao;
import com.lavyoung.marketforge.infrastructure.persistent.model.ActivityCountDefinitionSearchParam;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivityCountPO;
import com.lavyoung.marketforge.types.model.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

/**
 * 基于活动次数配置表的运营查询读取适配器。
 *
 * <p>先统计总数，空结果时跳过分页 SQL；存在数据时使用稳定排序读取当前页。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/26
 */
@Repository
@RequiredArgsConstructor
public class ActivityCountDefinitionReader implements IActivityCountDefinitionReader {

    private final IActivityCountDao activityCountDao;
    private final ActivityCountDefinitionReadModelAssembler readModelAssembler;

    @Override
    public PageResult<ActivityCountDefinitionResult> search(ActivityCountDefinitionSearchQuery query) {
        ActivityCountDefinitionSearchQuery validQuery = Objects.requireNonNull(query, "query must not be null");
        ActivityCountDefinitionSearchParam searchParam = new ActivityCountDefinitionSearchParam(
                validQuery.activityCountId(), validQuery.activityCountName(), validQuery.page().offset(),
                validQuery.page().pageSize());
        long total = activityCountDao.countActivityCountDefinitions(searchParam);
        if (total == 0) {
            return PageResult.empty(validQuery.page().pageNumber(), validQuery.page().pageSize());
        }
        List<ActivityCountPO> definitions = Objects.requireNonNull(
                activityCountDao.queryActivityCountDefinitionPage(searchParam),
                "activity count definition query result must not be null");
        return new PageResult<>(readModelAssembler.toResults(definitions), total,
                validQuery.page().pageNumber(), validQuery.page().pageSize());
    }
}
