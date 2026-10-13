package com.lavyoung.marketforge.app.config.dcc;

import java.lang.annotation.*;

/**
 * 标记需要由分布式动态配置中心维护的字段。
 *
 * <p>注解值使用 {@code 配置键:默认值} 格式，例如：
 *
 * <pre>{@code
 * @DccValue("activity.degrade.switch:false")
 * private boolean degradeSwitch;
 * }</pre>
 *
 * <p>系统启动时读取默认值；ZooKeeper 中配置发生变化后，
 * DCC 监听器会根据配置键找到对应字段，并更新字段内容。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/12
 */
@Documented
@Retention(RetentionPolicy.RUNTIME) // 表示程序运行时仍然可以读取注解
@Target(ElementType.FIELD)
public @interface DccValue {

    /**
     * 获取动态配置项定义。
     *
     * <p>格式为 {@code 配置键:默认值}。配置键用于定位 ZooKeeper
     * 中的具体配置，默认值用于 ZooKeeper 暂时没有该配置时的初始化。
     *
     * @return 动态配置键及默认值
     */
    String value();
}
