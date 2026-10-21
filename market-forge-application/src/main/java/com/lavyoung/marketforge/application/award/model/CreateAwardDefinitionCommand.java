package com.lavyoung.marketforge.application.award.model;

/**
 * 创建奖品定义命令。
 *
 * <p>奖品标识、初始状态、乐观锁版本和审计时间由服务端生成，
 * 外部调用方不能指定这些系统控制字段。</p>
 *
 * @param awardCode     奖品业务编码
 * @param awardName     奖品名称
 * @param awardType     奖品发放类型编码
 * @param claimMode     领取模式编码
 * @param providerCode  履约供应商编码；内部奖品可以为空
 * @param awardConfig   奖品发放配置快照内容
 * @param configVersion 配置结构版本
 * @param awardDesc     奖品说明；可以为空
 * @param operator      创建奖品的运营人员标识
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/21
 */
public record CreateAwardDefinitionCommand(
        String awardCode,
        String awardName,
        String awardType,
        String claimMode,
        String providerCode,
        String awardConfig,
        int configVersion,
        String awardDesc,
        String operator
) {
}