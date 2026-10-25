/**
 * 策略应用服务。
 *
 * <p>负责策略定义运营、运行时预热、抽奖执行和库存同步等用例编排。
 * 核心抽奖与生命周期规则属于 Domain，HTTP 和认证上下文属于 Trigger，
 * 数据库存取和缓存实现属于 Infrastructure。</p>
 */
package com.lavyoung.marketforge.application.strategy.service;
