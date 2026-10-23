/**
 * 奖品履约配置的基础设施适配实现。
 *
 * <p>本包负责实现 Application 层定义的奖品配置解码端口，将 JSON 等外部表示转换为
 * 类型安全的应用配置对象。这里可以依赖 Jackson 等技术组件，但不得决定具体奖品的
 * 发放条件、幂等策略或状态流转。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/23
 */
package com.lavyoung.marketforge.infrastructure.award.grant;
