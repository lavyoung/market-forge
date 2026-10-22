package com.lavyoung.marketforge.infrastructure.persistent.query;

import com.lavyoung.marketforge.application.activity.model.ActivityDefinitionResult;
import com.lavyoung.marketforge.application.activity.model.ActivityDefinitionSearchQuery;
import com.lavyoung.marketforge.application.activity.query.IActivityDefinitionReader;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.activity.ActivityDefinitionReadModelAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.dao.activity.IActivityDao;
import com.lavyoung.marketforge.infrastructure.persistent.model.ActivityDefinitionSearchParam;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivityPO;
import com.lavyoung.marketforge.types.model.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

/**
 * 基于活动主表的运营查询读取适配器。
 *
 * <p>先统计满足条件的活动总数；没有记录时直接返回空页，避免执行额外分页 SQL。
 * 存在记录时按照更新时间和主键倒序读取，并转换为 Application 读模型。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
@Repository
@RequiredArgsConstructor
public class ActivityDefinitionReader implements IActivityDefinitionReader {

    private final IActivityDao activityDao;
    private final ActivityDefinitionReadModelAssembler readModelAssembler;

    @Override
    public PageResult<ActivityDefinitionResult> search(ActivityDefinitionSearchQuery query) {
        ActivityDefinitionSearchQuery validQuery = Objects.requireNonNull(query, "query must not be null");
        ActivityDefinitionSearchParam searchParam = new ActivityDefinitionSearchParam(validQuery.activityId(), validQuery.activityName(), validQuery.state(), validQuery.page().offset(), validQuery.page().pageSize());
        long total = activityDao.countActivityDefinitions(searchParam);
        if (total == 0) {
            return PageResult.empty(validQuery.page().pageNumber(), validQuery.page().pageSize());
        }

        List<ActivityPO> activities = Objects.requireNonNull(activityDao.queryActivityDefinitionPage(searchParam), "activity definition query result must not be null");
        List<ActivityDefinitionResult> records = readModelAssembler.toResults(activities);
        return new PageResult<>(records, total, validQuery.page().pageNumber(), validQuery.page().pageSize());
    }
}
