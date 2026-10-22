package com.lavyoung.marketforge.infrastructure.persistent.query;

import com.lavyoung.marketforge.application.activity.model.ActivityStageResult;
import com.lavyoung.marketforge.application.activity.model.ActivityStageSearchQuery;
import com.lavyoung.marketforge.application.activity.query.IActivityStageReader;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.activity.ActivityStageReadModelAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.dao.activity.IActivityStageDao;
import com.lavyoung.marketforge.infrastructure.persistent.model.ActivityStageSearchParam;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivityStagePO;
import com.lavyoung.marketforge.types.model.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

/**
 * 基于活动展台主表的运营查询读取适配器。
 *
 * <p>先统计满足条件的总数；总数为零时直接返回空页，避免额外分页 SQL。存在记录时
 * 按更新时间和主键倒序读取，保证翻页顺序稳定。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
@Repository
@RequiredArgsConstructor
public class ActivityStageReader implements IActivityStageReader {

    private final IActivityStageDao activityStageDao;
    private final ActivityStageReadModelAssembler readModelAssembler;

    @Override
    public PageResult<ActivityStageResult> search(ActivityStageSearchQuery query) {
        ActivityStageSearchQuery validQuery = Objects.requireNonNull(query, "query must not be null");
        ActivityStageSearchParam searchParam = new ActivityStageSearchParam(validQuery.channel(), validQuery.source(), validQuery.state(), validQuery.page().offset(), validQuery.page().pageSize());
        long total = activityStageDao.countActivityStages(searchParam);
        if (total == 0) {
            return PageResult.empty(validQuery.page().pageNumber(), validQuery.page().pageSize());
        }

        List<ActivityStagePO> stages = Objects.requireNonNull(activityStageDao.queryActivityStagePage(searchParam), "activity stage query result must not be null");
        return new PageResult<>(readModelAssembler.toResults(stages), total, validQuery.page().pageNumber(), validQuery.page().pageSize());
    }
}
