package com.lavyoung.marketforge.application.activity.query;

import com.lavyoung.marketforge.application.activity.model.ActivitySkuDefinitionResult;
import com.lavyoung.marketforge.application.activity.model.ActivitySkuDefinitionSearchQuery;
import com.lavyoung.marketforge.types.model.PageResult;

/**
 * 活动 SKU 运营读模型端口。
 *
 * <p>该端口服务于后台分页查询，不承担领域行为。基础设施实现可以直接从活动 SKU 表
 * 构建应用查询结果，但不得把 MyBatis 参数或持久化对象泄漏到应用层。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/27
 */
public interface IActivitySkuDefinitionReader {

    /**
     * 分页查询活动 SKU 定义。
     *
     * @param query 查询条件
     * @return 活动 SKU 分页结果
     * @throws NullPointerException 查询条件为空时抛出
     */
    PageResult<ActivitySkuDefinitionResult> search(ActivitySkuDefinitionSearchQuery query);
}
