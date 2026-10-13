package com.lavyoung.marketforge.app.config;

import com.lavyoung.marketforge.app.config.dcc.DccBeanPostProcessor;
import com.lavyoung.marketforge.app.config.dcc.DccFieldBinder;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ZooKeeper 客户端 Spring 配置。
 *
 * <p>本类负责根据应用配置创建 Curator 客户端，并交给 Spring 管理。
 * 动态配置监听、节点注册和业务降级判断不应写在这里。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/12
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ZooKeeperDccProperties.class)
public class ZooKeeperClientConfig {

    /**
     * 创建由 Spring 管理生命周期的 Curator 客户端。
     *
     * <p>客户端启动时连接远程 ZooKeeper；应用关闭时由 Spring 调用
     * {@code close} 释放网络连接和线程资源。</p>
     *
     * @param properties ZooKeeper 连接配置
     * @return Curator ZooKeeper 客户端
     * @throws IllegalArgumentException 当连接地址或重试参数非法时抛出
     */
    @Bean(initMethod = "start", destroyMethod = "close")
    public CuratorFramework zookeeperClient(ZooKeeperDccProperties properties) {
        ExponentialBackoffRetry backoffRetry = new ExponentialBackoffRetry(
                properties.baseSleepTimeMs(),
                properties.maxRetries()
        );

        return CuratorFrameworkFactory.builder()
                .connectString(properties.connectString())
                .retryPolicy(backoffRetry)
                .sessionTimeoutMs(properties.sessionTimeoutMs())
                .connectionTimeoutMs(properties.connectionTimeoutMs())
                .build();
    }

    /**
     * 创建动态配置字段绑定器。
     *
     * @param properties    ZooKeeper 动态配置属性
     * @param curatorClient Curator ZooKeeper 客户端
     * @return 动态配置字段绑定器
     * @throws IllegalArgumentException 根路径非法时抛出
     */
    @Bean(destroyMethod = "close")
    public DccFieldBinder dccFieldBinder(ZooKeeperDccProperties properties, CuratorFramework curatorClient) {
        return new DccFieldBinder(properties.rootPath(), curatorClient);
    }

    /**
     * 创建 Spring 动态配置 Bean 后置处理器。
     *
     * @param fieldBinder 动态配置字段绑定器
     * @return Spring Bean 后置处理器
     */
    @Bean(destroyMethod = "close")
    public DccBeanPostProcessor dccBeanPostProcessor(DccFieldBinder fieldBinder) {
        return new DccBeanPostProcessor(fieldBinder);
    }
}
