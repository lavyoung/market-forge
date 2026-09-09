/**
 * Market Forge 对其他服务及客户端公开的远程 API 契约。
 * <p>
 * 该模块采用契约优先方式声明 REST 路由、校验规则及协议 DTO，不包含持久化或领域实现细节。
 * 消费方可以依赖该模块创建 HTTP 客户端代理，提供方则在 Trigger 模块实现这些接口。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/09
 */
package com.lavyoung.marketforge.api;
