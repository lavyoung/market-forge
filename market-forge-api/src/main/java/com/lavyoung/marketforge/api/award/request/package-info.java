/**
 * 用户奖品 API 请求模型。
 *
 * <p>本包中的类型负责接收 HTTP 查询参数或请求体，并通过 Jakarta Validation 与 Spring
 * 格式化注解完成协议层校验和类型转换。请求模型属于外部协议，不应直接传入 Domain，
 * 也不得包含 Elasticsearch DSL、数据库字段或基础设施客户端类型。</p>
 *
 * <p>Trigger 层负责将请求模型转换为 Application 命令或查询对象，使外部接口的字段变化
 * 不会直接影响应用用例和领域模型。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/16
 */
package com.lavyoung.marketforge.api.award.request;
