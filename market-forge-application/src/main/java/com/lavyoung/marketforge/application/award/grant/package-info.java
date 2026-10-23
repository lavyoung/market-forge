/**
 * 奖品履约策略、配置协议与注册能力。
 *
 * <p>本包负责根据奖品类型选择具体履约处理器，并按“奖品类型 + 配置版本”
 * 校验结构化奖品配置。处理器和校验器可以调用领域服务或出站端口，
 * 但不得包含 HTTP、MyBatis、MQ 消息对象等技术模型。</p>
 *
 * <p>具体 JSON 解码、第三方供应商 SDK 和远程调用由 Infrastructure 提供
 * 适配器实现，避免技术组件泄漏到应用编排中。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/23
 */
package com.lavyoung.marketforge.application.award.grant;