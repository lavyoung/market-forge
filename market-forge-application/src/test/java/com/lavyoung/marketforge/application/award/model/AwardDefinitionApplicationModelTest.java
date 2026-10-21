package com.lavyoung.marketforge.application.award.model;

import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 奖品定义应用模型契约测试。
 *
 * <p>验证外部入口能够提交的字段边界，防止后续迭代把服务端生成字段或数据库当前状态
 * 意外暴露到应用命令中，破坏身份审计和状态机约束。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
class AwardDefinitionApplicationModelTest {

    /**
     * Given 创建奖品命令，When 检查记录组件，Then 不应允许调用方指定标识、状态和审计时间。
     */
    @Test
    void shouldKeepSystemControlledFieldsOutOfCreateCommand() {
        // Given & When
        Set<String> componentNames = componentNames(CreateAwardDefinitionCommand.class);

        // Then
        assertAll(
                () -> assertFalse(componentNames.contains("awardId")),
                () -> assertFalse(componentNames.contains("state")),
                () -> assertFalse(componentNames.contains("version")),
                () -> assertFalse(componentNames.contains("createdBy")),
                () -> assertFalse(componentNames.contains("createTime")),
                () -> assertTrue(componentNames.contains("operator"))
        );
    }

    /**
     * Given 修改草稿命令，When 检查记录组件，Then 应携带奖品标识、预期版本和操作人。
     */
    @Test
    void shouldCarryIdentityAndOptimisticLockForDraftUpdate() {
        // Given & When
        Set<String> componentNames = componentNames(UpdateAwardDefinitionCommand.class);

        // Then
        assertAll(
                () -> assertTrue(componentNames.contains("awardId")),
                () -> assertTrue(componentNames.contains("expectedVersion")),
                () -> assertTrue(componentNames.contains("operator")),
                () -> assertFalse(componentNames.contains("state"))
        );
    }

    /**
     * Given 状态变更命令，When 检查记录组件，Then 应由服务端查询当前状态而非信任客户端输入。
     */
    @Test
    void shouldNotAcceptClientReportedCurrentState() {
        // Given & When
        Set<String> componentNames = componentNames(ChangeAwardDefinitionStateCommand.class);

        // Then
        assertAll(
                () -> assertTrue(componentNames.contains("awardId")),
                () -> assertTrue(componentNames.contains("targetState")),
                () -> assertTrue(componentNames.contains("expectedVersion")),
                () -> assertTrue(componentNames.contains("operator")),
                () -> assertFalse(componentNames.contains("currentState"))
        );
    }

    private Set<String> componentNames(Class<? extends Record> recordType) {
        return Arrays.stream(recordType.getRecordComponents())
                .map(RecordComponent::getName)
                .collect(Collectors.toUnmodifiableSet());
    }
}
