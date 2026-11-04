/**
 * 提供跨模块模型构造期的基础参数约束。
 *
 * <p>本包只包含与具体业务无关的 Java 对象前置条件，例如非空白文本和文本长度。
 * 活动状态、库存关系、策略概率等业务规则仍由对应领域模型负责。</p>
 *
 * <p>校验失败只抛出 Java 标准异常，不依赖 Spring、Bean Validation 或业务错误码，
 * 保证 Domain 和 Application 都可以安全使用。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/04
 */
package com.lavyoung.marketforge.types.validation;