package com.lavyoung.marketforge.app.config.dcc;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.util.Objects;

/**
 * DCC 动态配置 Bean 后置处理器。
 *
 * <p>Spring 创建业务 Bean 后，本处理器会自动调用
 * {@link DccFieldBinder#bind(Object)}，扫描 Bean 中的
 * {@link DccValue} 字段并建立 ZooKeeper 配置监听。
 *
 * <p>该类只负责接入 Spring 生命周期，不负责配置解析、
 * 字段类型转换或 ZooKeeper 节点监听。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/13
 */
public final class DccBeanPostProcessor implements BeanPostProcessor, AutoCloseable {

    /**
     * 动态配置字段绑定器。
     */
    private final DccFieldBinder fieldBinder;

    /**
     * 创建 Spring Bean 后置处理器。
     *
     * @param fieldBinder 动态配置字段绑定器
     * @throws NullPointerException 绑定器为空时抛出
     */
    public DccBeanPostProcessor(DccFieldBinder fieldBinder) {
        this.fieldBinder = Objects.requireNonNull(fieldBinder, "fieldBinder must not be null");
    }

    /**
     * 在 Spring Bean 初始化完成后执行动态配置绑定。
     *
     * <p>没有 {@link DccValue} 字段的 Bean 会被快速跳过。
     * 如果发现配置定义错误或监听器启动失败，
     * 会抛出 {@link BeansException} 终止应用启动，
     * 防止应用在动态配置未生效的状态下继续运行。
     *
     * @param bean     已初始化的 Spring Bean
     * @param beanName Spring Bean 名称
     * @return 原始 Bean 实例
     * @throws BeansException 动态配置绑定失败时抛出
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        try {
            fieldBinder.bind(bean);
            return bean;
        } catch (Exception e) {
            throw new BeanCreationException(beanName, "DCC dynamic configuration binding failed", e);
        }
    }

    /**
     * 关闭所有动态配置监听器。
     */
    @Override
    public void close() throws Exception {
        fieldBinder.close();
    }
}
