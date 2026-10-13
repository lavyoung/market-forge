package com.lavyoung.marketforge.app.config.dcc;

import com.lavyoung.marketforge.application.activity.service.IActivityDegradeSwitch;
import io.github.lavyoung.dcc.core.DccValue;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

/**
 * 活动动态配置。
 *
 * <p>该类只描述营销业务自己的动态配置，不负责 ZooKeeper
 * 连接、监听和配置转换。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/13
 */
@Setter
@Getter
@Component
public class ActivityDccConfig implements IActivityDegradeSwitch {

    /**
     * 活动降级开关。
     *
     * <p>当值为 {@code true} 时，活动业务应拒绝正常参与请求，
     * 或进入项目定义的降级处理流程。</p>
     */
    @DccValue("activity.degrade.switch:false")
    private boolean activityDegradeSwitch;

    /**
     * 活动白名单用户。
     *
     * <p>多个用户 ID 使用英文逗号分隔，例如：
     * {@code user-001,user-002}。</p>
     *
     * <p>空字符串表示当前没有配置白名单。</p>
     */
    @DccValue("activity.whitelist:")
    private String activityWhitelist;

    /**
     * 活动黑名单用户。
     *
     * <p>多个用户 ID 使用英文逗号分隔，例如：
     * {@code user-003,user-004}。</p>
     *
     * <p>空字符串表示当前没有配置黑名单。</p>
     */
    @DccValue("activity.blacklist:")
    private String activityBlacklist;

    @Override
    public boolean isDegraded() {
        return isActivityDegradeSwitch();
    }
}
