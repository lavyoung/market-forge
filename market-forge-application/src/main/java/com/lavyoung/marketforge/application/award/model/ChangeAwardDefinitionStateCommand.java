package com.lavyoung.marketforge.application.award.model;

/**
 * 变更奖品定义生命周期状态命令。
 *
 * <p>命令不接收调用方声明的当前状态。应用服务会重新查询数据库当前状态，
 * 再由领域状态对象判断是否允许流转，避免客户端伪造当前状态。</p>
 *
 * @param awardId         奖品业务标识
 * @param targetState     目标状态编码
 * @param expectedVersion 调用方读取到的乐观锁版本
 * @param operator        执行状态变更的运营人员标识
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/21
 */
public record ChangeAwardDefinitionStateCommand(
        Long awardId,
        String targetState,
        int expectedVersion,
        String operator
) {
}
