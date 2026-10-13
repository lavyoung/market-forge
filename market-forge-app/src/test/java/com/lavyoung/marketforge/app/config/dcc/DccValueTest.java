package com.lavyoung.marketforge.app.config.dcc;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link DccValue} 注解元数据测试。
 *
 * <p>验证注解可以在运行时被反射读取，并且只能用于字段，
 * 这是字段绑定器工作的前提。
 *
 * @author lavyoung
 * @since 2026-10-13
 */
class DccValueTest {

    /**
     * 验证注解值可以在运行时读取。
     *
     * <p>Given 一个被 {@link DccValue} 标记的字段，When 读取注解，
     * Then 得到配置键和默认值定义。
     *
     * @throws NoSuchFieldException 测试字段不存在时抛出
     */
    @Test
    void shouldReadDccDefinitionAtRuntime() throws NoSuchFieldException {
        Field field = Target.class.getDeclaredField("degradeSwitch");

        DccValue annotation = field.getAnnotation(DccValue.class);

        assertEquals(
                "activity.degrade.switch:false",
                annotation.value()
        );
        assertTrue(field.isAnnotationPresent(DccValue.class));
    }

    /**
     * 测试动态配置字段。
     */
    private static final class Target {

        /**
         * 活动降级开关。
         */
        @DccValue("activity.degrade.switch:false")
        private boolean degradeSwitch;
    }
}
