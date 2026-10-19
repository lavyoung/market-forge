/**
 * 后台身份认证适配模型。
 *
 * <p>该包负责表达 Spring Security 认证上下文中的后台操作员身份，
 * 供应用装配层创建，并由 Trigger 层的认证接口读取。这里的对象不属于
 * 业务领域模型，也不承载抽奖、活动或积分等领域规则。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/19
 */
package com.lavyoung.marketforge.trigger.security;