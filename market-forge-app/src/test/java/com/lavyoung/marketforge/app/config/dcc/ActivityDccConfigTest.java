package com.lavyoung.marketforge.app.config.dcc;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 活动动态配置测试。
 *
 * <p>验证活动配置 Bean 实现 application 层降级端口，
 * 并且使用 {@link DccValue} 声明了正确的 ZooKeeper 配置键和默认值。
 *
 * @author lavyoung
 * @since 2026-10-13
 */
class ActivityDccConfigTest {

    /**
     * 验证活动配置默认处于非降级状态。
     */
    @Test
    void shouldBeNormalByDefault() {
        ActivityDccConfig config = new ActivityDccConfig();

        assertFalse(config.isDegraded());
    }

    /**
     * 验证活动降级字段使用正确的 DCC 配置声明。
     *
     * @throws NoSuchFieldException 配置字段不存在时抛出
     */
    @Test
    void shouldDeclareActivityDegradeSwitch() throws NoSuchFieldException {
        Field field = ActivityDccConfig.class.getDeclaredField("degraded");
        DccValue annotation = field.getAnnotation(DccValue.class);

        assertTrue(field.isAnnotationPresent(DccValue.class));
        org.junit.jupiter.api.Assertions.assertEquals(
                "activity.degrade.switch:false",
                annotation.value()
        );
    }
}
