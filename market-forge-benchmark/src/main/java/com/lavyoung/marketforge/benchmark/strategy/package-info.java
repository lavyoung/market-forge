/**
 * 抽奖概率算法性能基准。
 *
 * <p>该包使用 JMH 隔离测量概率算法的装配时间、单次派发延迟和内存分配，
 * 不启动 Spring 容器，也不访问 Redis、MySQL 等外部基础设施。</p>
 */
package com.lavyoung.marketforge.benchmark.strategy;
