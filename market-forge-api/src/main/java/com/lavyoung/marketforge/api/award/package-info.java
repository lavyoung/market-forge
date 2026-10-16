/**
 * 用户奖品 REST API 契约。
 *
 * <p>本包定义用户中奖记录等奖品相关的 HTTP 接口，负责声明资源路径、请求方式、
 * 参数校验入口和统一响应结构。接口只描述对调用方稳定公开的协议，不承担应用流程编排、
 * 领域规则、数据库访问或 Elasticsearch 查询。</p>
 *
 * <p>具体 HTTP 适配由 Trigger 模块中的 Controller 完成，业务用例由 Application 模块提供，
 * 因此 API 模块不得依赖 Controller、Application Service 或基础设施实现。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/16
 */
package com.lavyoung.marketforge.api.award;
