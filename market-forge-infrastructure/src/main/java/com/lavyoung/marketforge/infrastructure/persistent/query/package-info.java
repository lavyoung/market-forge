/**
 * 运营查询端口的数据库读取适配器。
 *
 * <p>本包负责把 Application 层的查询条件转换为数据库查询参数，并将持久化结果
 * 投影为应用读模型。这里不承载领域规则，也不向 Application 层暴露 MyBatis、
 * 持久化对象或数据库分页实现。</p>
 */
package com.lavyoung.marketforge.infrastructure.persistent.query;
