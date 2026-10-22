package com.lavyoung.marketforge.application.activity.model;

/**
 * 发布活动展台命令。
 *
 * @param stageId         展台业务标识
 * @param expectedVersion 调用方读取到的乐观锁版本
 * @param operator        当前运营人员标识
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
public record PublishActivityStageCommand(Long stageId, int expectedVersion, String operator) {
}
