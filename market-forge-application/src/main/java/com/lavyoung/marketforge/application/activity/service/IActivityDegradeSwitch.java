package com.lavyoung.marketforge.application.activity.service;

/**
 * 活动降级状态查询端口。
 *
 * <p>application 层只依赖该抽象，不关心降级配置来自 ZooKeeper、
 * Redis、数据库还是本地配置文件。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/13
 */
public interface IActivityDegradeSwitch {

    /**
     * 判断活动抽奖是否处于降级状态。
     *
     * @return {@code true} 表示活动已降级，不允许继续抽奖；
     * {@code false} 表示活动正常运行
     */
    boolean isDegraded();
}
