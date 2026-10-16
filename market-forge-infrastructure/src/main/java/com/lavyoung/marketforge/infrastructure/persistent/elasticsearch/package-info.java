/**
 * Elasticsearch 持久化与检索实现。
 *
 * <p>本包负责 Elasticsearch 客户端装配、索引文档映射以及 Application 查询接口实现。
 * Application 层只依赖只读查询接口和应用读模型，不感知索引名称、查询 DSL 与客户端类型。</p>
 *
 * <p>子包职责：</p>
 * <ul>
 *     <li>{@code config}：连接属性与客户端生命周期；</li>
 *     <li>{@code document}：索引 {@code _source} 文档；</li>
 *     <li>{@code assembler}：索引文档与应用读模型转换；</li>
 *     <li>{@code query}：应用只读查询接口的 Elasticsearch 实现。</li>
 * </ul>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/16
 */
package com.lavyoung.marketforge.infrastructure.persistent.elasticsearch;
