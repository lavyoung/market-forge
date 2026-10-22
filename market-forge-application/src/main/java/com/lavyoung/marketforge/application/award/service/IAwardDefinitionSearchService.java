package com.lavyoung.marketforge.application.award.service;

import com.lavyoung.marketforge.application.award.model.AwardDefinitionResult;
import com.lavyoung.marketforge.application.award.model.AwardDefinitionSearchQuery;
import com.lavyoung.marketforge.types.model.PageResult;

/**
 * 奖品定义运营查询应用服务。
 *
 * <p>为 Trigger 层提供稳定的奖品分页查询用例。HTTP 请求必须先转换为
 * {@link AwardDefinitionSearchQuery}，不得把 API Request 直接传入应用层。</p>
 *
 * <p>该服务只提供读取能力，不参与奖品创建、编辑或生命周期状态变更。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
public interface IAwardDefinitionSearchService {

    /**
     * 分页查询奖品定义。
     *
     * @param query 奖品定义查询条件
     * @return 奖品定义分页结果
     * @throws NullPointerException 查询条件为空时抛出
     * @throws RuntimeException     数据库查询或结果转换失败时抛出
     */
    PageResult<AwardDefinitionResult> search(AwardDefinitionSearchQuery query);
}
