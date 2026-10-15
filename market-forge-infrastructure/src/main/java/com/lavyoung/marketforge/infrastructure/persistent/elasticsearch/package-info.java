/**
 * Elasticsearch 持久化与检索适配模块。
 *
 * <p>该模块负责 Elasticsearch 客户端装配、索引文档映射、查询条件构建，
 * 以及领域查询端口的基础设施实现。领域层和应用层不得直接依赖
 * Elasticsearch 官方客户端、索引名称或查询 DSL。</p>
 *
 * <p>包内主要职责划分如下：</p>
 *
 * <ul>
 *     <li>{@code config}：客户端及连接属性配置；</li>
 *     <li>{@code document}：Elasticsearch 索引文档对象；</li>
 *     <li>{@code assembler}：索引文档与领域查询结果之间的转换；</li>
 *     <li>{@code repository}：领域查询端口的 Elasticsearch 实现。</li>
 * </ul>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/15
 */
package com.lavyoung.marketforge.infrastructure.persistent.elasticsearch;