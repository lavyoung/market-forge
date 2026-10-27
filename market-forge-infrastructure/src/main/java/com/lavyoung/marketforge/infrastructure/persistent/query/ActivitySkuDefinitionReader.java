package com.lavyoung.marketforge.infrastructure.persistent.query;

import com.lavyoung.marketforge.application.activity.model.ActivitySkuDefinitionResult;
import com.lavyoung.marketforge.application.activity.model.ActivitySkuDefinitionSearchQuery;
import com.lavyoung.marketforge.application.activity.query.IActivitySkuDefinitionReader;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.activity.ActivitySkuDefinitionReadModelAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.dao.activity.IActivitySkuDao;
import com.lavyoung.marketforge.infrastructure.persistent.model.ActivitySkuDefinitionSearchParam;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivitySkuPO;
import com.lavyoung.marketforge.types.model.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

/**
 * 基于活动 SKU 表的运营查询读取适配器。
 *
 * <p>先统计满足条件的总数，空结果时跳过分页读取；存在数据时按照更新时间和主键倒序
 * 查询，保证运营页面翻页顺序稳定。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/27
 */
@Repository
@RequiredArgsConstructor
public class ActivitySkuDefinitionReader implements IActivitySkuDefinitionReader {

    private final IActivitySkuDao activitySkuDao;
    private final ActivitySkuDefinitionReadModelAssembler readModelAssembler;

    @Override
    public PageResult<ActivitySkuDefinitionResult> search(ActivitySkuDefinitionSearchQuery query) {
        ActivitySkuDefinitionSearchQuery validQuery = Objects.requireNonNull(query, "query must not be null");
        ActivitySkuDefinitionSearchParam searchParam = new ActivitySkuDefinitionSearchParam(validQuery.sku(),
                validQuery.activityId(), validQuery.activityCountId(), validQuery.page().offset(),
                validQuery.page().pageSize());
        long total = activitySkuDao.countActivitySkuDefinitions(searchParam);
        if (total == 0) {
            return PageResult.empty(validQuery.page().pageNumber(), validQuery.page().pageSize());
        }
        List<ActivitySkuPO> definitions = Objects.requireNonNull(
                activitySkuDao.queryActivitySkuDefinitionPage(searchParam),
                "activity SKU definition query result must not be null");
        return new PageResult<>(readModelAssembler.toResults(definitions), total,
                validQuery.page().pageNumber(), validQuery.page().pageSize());
    }
}
