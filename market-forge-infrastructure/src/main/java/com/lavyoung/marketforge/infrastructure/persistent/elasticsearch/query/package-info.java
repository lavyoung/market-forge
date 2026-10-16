/**
 * Elasticsearch 查询实现。
 *
 * <p>负责实现 Application 层声明的只读接口，将查询条件转换为 Elasticsearch DSL、执行查询并返回应用读模型。
 * 该包不承载领域规则，也不向上层暴露搜索引擎技术对象。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/16
 */
package com.lavyoung.marketforge.infrastructure.persistent.elasticsearch.query;
