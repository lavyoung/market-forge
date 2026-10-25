/**
 * 抽奖策略领域值对象。
 *
 * <p>该包保存没有独立身份、由自身属性共同决定业务含义的不可变对象，
 * 包括策略生命周期、规则模型、规则树结构、库存键以及规则参数。</p>
 *
 * <p>规则参数值对象同时承担持久化字符串与领域语义之间的转换职责，
 * 确保运营发布校验、概率装配和抽奖运行时使用完全一致的解析规则。
 * 值对象不得访问数据库、Redis、HTTP 或其他基础设施。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
package com.lavyoung.marketforge.domain.strategy.model.vo;