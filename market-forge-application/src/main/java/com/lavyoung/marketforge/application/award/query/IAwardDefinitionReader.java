package com.lavyoung.marketforge.application.award.query;

import com.lavyoung.marketforge.application.award.model.AwardDefinitionResult;
import com.lavyoung.marketforge.application.award.model.AwardDefinitionSearchQuery;
import com.lavyoung.marketforge.types.model.PageResult;

/**
 * 奖品定义运营查询读取端口。
 *
 * <p>该端口只表达运营页面需要的分页读取能力。Infrastructure 层负责使用
 * MySQL 实现查询，但不得向应用层暴露 MyBatis、PO 或分页插件类型。</p>
 *
 * <p>它不同于领域 Repository：Repository 负责加载和保存领域实体，
 * Reader 负责生成不参与业务决策的页面读模型。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
public interface IAwardDefinitionReader {

    /**
     * 分页查询奖品定义。
     *
     * <p>结果必须按照更新时间倒序、数据库主键倒序稳定排列。</p>
     *
     * @param query 奖品定义查询条件
     * @return 奖品定义分页结果
     * @throws NullPointerException 查询条件为空时抛出
     * @throws RuntimeException     数据库查询或结果转换失败时抛出
     */
    PageResult<AwardDefinitionResult> search(AwardDefinitionSearchQuery query);
}