package com.lavyoung.marketforge.infrastructure.persistent.model;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证 MyBatis 可以跨模块读取任务发布抢占参数。
 *
 * <p>该测试覆盖生产环境使用模块路径运行时的反射访问边界，避免
 * {@link TaskPublishingClaim} 所在包未向 MyBatis 开放而触发
 * {@code InaccessibleObjectException}。</p>
 *
 * @author lavyoung
 */
class TaskPublishingClaimModuleAccessTest {

    /**
     * 验证 MyBatis MetaObject 能够读取 record 访问器。
     */
    @Test
    void shouldExposeRecordAccessorsToMyBatis() {
        // Given
        LocalDateTime claimedAt = LocalDateTime.of(2026, 10, 15, 23, 30);
        LocalDateTime publishingExpiredBefore = claimedAt.minusMinutes(5);
        TaskPublishingClaim claim = new TaskPublishingClaim(
                "user-001",
                "event-001",
                "publishing",
                "create",
                "publish_failed",
                "publishing",
                claimedAt,
                publishingExpiredBefore
        );
        MetaObject metaObject = SystemMetaObject.forObject(claim);

        // When
        Object publishingState = metaObject.getValue("publishingState");
        Object expiredBefore = metaObject.getValue("publishingExpiredBefore");

        // Then
        assertAll(
                () -> assertEquals("publishing", publishingState),
                () -> assertEquals(publishingExpiredBefore, expiredBefore)
        );
    }
}
