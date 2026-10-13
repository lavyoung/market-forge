package com.lavyoung.marketforge.app.config.dcc;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * {@link DccBeanPostProcessor} 单元测试。
 *
 * <p>验证 Spring Bean 初始化成功和动态配置绑定失败两条路径，
 * 同时验证容器销毁时会关闭字段绑定器。
 *
 * @author lavyoung
 * @since 2026-10-13
 */
class DccBeanPostProcessorTest {

    /**
     * 验证 Bean 初始化后会调用字段绑定器，并返回原对象。
     *
     * @throws Exception 测试模拟绑定器抛出受检异常时抛出
     */
    @Test
    void shouldBindBeanAfterInitialization() throws Exception {
        DccFieldBinder binder = mock(DccFieldBinder.class);
        DccBeanPostProcessor processor = new DccBeanPostProcessor(binder);
        Object bean = new Object();

        Object result = processor.postProcessAfterInitialization(
                bean,
                "testBean"
        );

        assertSame(bean, result);
        verify(binder).bind(bean);
    }

    /**
     * 验证绑定失败时转换成 BeanCreationException。
     *
     * @throws Exception 测试模拟绑定器抛出受检异常时抛出
     */
    @Test
    void shouldWrapBindingFailure() throws Exception {
        DccFieldBinder binder = mock(DccFieldBinder.class);
        IllegalArgumentException cause = new IllegalArgumentException(
                "invalid DCC value"
        );
        doThrow(cause).when(binder).bind(any());

        DccBeanPostProcessor processor = new DccBeanPostProcessor(binder);
        Object bean = new Object();

        BeanCreationException exception = assertThrows(
                BeanCreationException.class,
                () -> processor.postProcessAfterInitialization(
                        bean,
                        "testBean"
                )
        );

        assertEquals("testBean", exception.getBeanName());
        assertTrue(exception.getCause() instanceof Exception);
    }

    /**
     * 验证关闭后置处理器时会关闭字段绑定器。
     *
     * @throws Exception 关闭绑定器失败时抛出
     */
    @Test
    void shouldCloseFieldBinder() throws Exception {
        DccFieldBinder binder = mock(DccFieldBinder.class);
        doNothing().when(binder).close();
        DccBeanPostProcessor processor = new DccBeanPostProcessor(binder);

        processor.close();

        verify(binder).close();
    }
}
