/**
 * 仓储基础设施配置。
 *
 * <p>负责绑定仓储实现使用的缓存生命周期、持久化策略等技术参数。
 * 该包不得定义抽奖业务规则，也不得向领域层暴露 Redis 或 Spring 配置类型。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/21
 */
package com.lavyoung.marketforge.infrastructure.persistent.repository.config;