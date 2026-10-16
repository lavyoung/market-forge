/**
 * 用户奖品 API 响应模型。
 *
 * <p>本包定义对调用方稳定公开的奖品查询结果。响应模型只表达接口需要展示的业务数据，
 * 不暴露领域实体、数据库 PO、分库来源、Elasticsearch 文档或索引元数据。</p>
 *
 * <p>Trigger 层负责将 Application 查询结果转换为本包模型，保证内部模型调整时
 * REST API 契约仍可独立演进。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/16
 */
package com.lavyoung.marketforge.api.award.response;
