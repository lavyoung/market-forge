package com.lavyoung.marketforge.application.award.model;

/**
 * 修改草稿奖品定义命令。
 *
 * <p>该命令只能修改仍处于草稿状态的奖品。{@code expectedVersion}
 * 来自调用方最近一次查询结果，用于阻止并发运营修改互相覆盖。</p>
 *
 * @param awardId         奖品业务标识
 * @param awardCode       奖品业务编码
 * @param awardName       奖品名称
 * @param awardType       奖品发放类型编码
 * @param claimMode       领取模式编码
 * @param providerCode    履约供应商编码；内部奖品可以为空
 * @param awardConfig     奖品发放配置快照内容
 * @param configVersion   配置结构版本
 * @param awardDesc       奖品说明；可以为空
 * @param expectedVersion 调用方读取到的乐观锁版本
 * @param operator        执行修改的运营人员标识
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/21
 */
public record UpdateAwardDefinitionCommand(
        Long awardId,
        String awardCode,
        String awardName,
        String awardType,
        String claimMode,
        String providerCode,
        String awardConfig,
        int configVersion,
        String awardDesc,
        int expectedVersion,
        String operator
) {
}
