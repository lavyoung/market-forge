/**
 * Market Forge 基础设施层。
 *
 * <p>该层负责实现 Application Query 接口或 Domain Repository，并封装数据库、缓存、搜索引擎、
 * 消息中间件和外部服务等技术细节。{@code persistent} 统一组织数据库、缓存和搜索存储实现。</p>
 *
 * <p>基础设施可以依赖 Application 与 Domain 的稳定抽象，Application 和 Domain 不得反向依赖
 * 本层的客户端、PO、索引文档或配置类型。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/16
 */
package com.lavyoung.marketforge.infrastructure;
