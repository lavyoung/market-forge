/**
 * 抽奖策略运营配置聚合。
 *
 * <p>本包负责维护策略根配置、策略奖品和策略规则之间的一致性，并承载策略生命周期、
 * 草稿修改及历史策略复制语义。高频抽奖运行时模型仍位于 {@code model.entity}，
 * 避免运营配置职责进入抽奖主链路。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
package com.lavyoung.marketforge.domain.strategy.model.aggregate;