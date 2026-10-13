package com.lavyoung.marketforge.app.config.dcc;

import com.lavyoung.marketforge.application.activity.service.IActivityDegradeSwitch;
import org.springframework.stereotype.Component;

/**
 * 活动动态配置。
 *
 * <p>本类属于 app 装配层，负责把 ZooKeeper 动态配置转换为
 * application 层可以使用的活动降级状态。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/13
 */
@Component
public class ActivityDccConfig implements IActivityDegradeSwitch {

    /**
     * 活动抽奖降级开关。
     *
     * <p>true 表示活动进入降级状态； ZooKeeper 回调线程会修改字段 volatile 保证配置更新后，其他线程可以及时读取到最新值
     * false 表示活动正常运行。
     */
    @DccValue("activity.degrade.switch:false")
    private volatile boolean degraded;

    @Override
    public boolean isDegraded() {
        return degraded;
    }
}
