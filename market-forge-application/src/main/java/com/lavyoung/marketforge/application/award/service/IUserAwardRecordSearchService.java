package com.lavyoung.marketforge.application.award.service;

import com.lavyoung.marketforge.application.award.model.UserAwardRecordSearchItem;
import com.lavyoung.marketforge.application.award.model.UserAwardRecordSearchQuery;
import com.lavyoung.marketforge.types.model.PageResult;

/**
 * 用户中奖记录运营查询服务。
 *
 * <p>该服务为 Trigger 层提供稳定的中奖记录查询用例。HTTP 请求模型应先在
 * Controller 中转换为 {@link UserAwardRecordSearchQuery}，不得将 Web 协议对象
 * 直接传入应用层。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/15
 */
public interface IUserAwardRecordSearchService {

    /**
     * 分页检索用户中奖记录。
     *
     * @param query 用户中奖记录搜索条件
     * @return 满足条件的中奖记录分页结果
     * @throws IllegalArgumentException 查询条件为空或参数非法时抛出
     * @throws RuntimeException         搜索服务不可用或查询执行失败时抛出
     */
    PageResult<UserAwardRecordSearchItem> search(UserAwardRecordSearchQuery query);
}
