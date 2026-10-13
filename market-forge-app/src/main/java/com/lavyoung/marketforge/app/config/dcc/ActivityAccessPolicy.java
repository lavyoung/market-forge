package com.lavyoung.marketforge.app.config.dcc;

import com.lavyoung.marketforge.application.activity.service.IActivityAccessPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 活动用户黑白名单策略实现。
 *
 * <p>该类负责将 {@link ActivityDccConfig} 中的动态字符串配置解析为用户集合，
 * 并根据黑名单、白名单规则判断用户是否允许参与活动。</p>
 *
 * <p>本实现属于 app 层的配置适配器：</p>
 *
 * <ul>
 *     <li>application 层只依赖 {@link IActivityAccessPolicy}；</li>
 *     <li>app 层读取 {@link ActivityDccConfig}；</li>
 *     <li>DCC 组件负责动态刷新配置字段；</li>
 *     <li>黑名单优先级高于白名单。</li>
 * </ul>
 *
 * <p>名单格式为英文逗号分隔的用户 ID，例如：
 * {@code user-001,user-002,user-003}。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/13
 */
@Component
@RequiredArgsConstructor
public class ActivityAccessPolicy implements IActivityAccessPolicy {

    private final ActivityDccConfig activityDccConfig;

    /**
     * 判断用户是否允许参与活动。
     *
     * <p>判断顺序如下：</p>
     *
     * <ol>
     *     <li>用户 ID 为空时，直接抛出参数异常；</li>
     *     <li>用户命中黑名单时，直接拒绝；</li>
     *     <li>白名单为空时，允许所有未命中黑名单的用户；</li>
     *     <li>白名单不为空时，仅允许命中白名单的用户。</li>
     * </ol>
     *
     * @param userId 待判断的用户 ID
     * @return {@code true} 表示允许参与活动；
     * {@code false} 表示拒绝参与活动
     * @throws IllegalArgumentException 当用户 ID 为空或只包含空白字符时抛出
     */
    @Override
    public boolean isAllowed(String userId) {
        validateUserId(userId);
        Set<String> blacklist = parseUserIds(activityDccConfig.getActivityBlacklist());
        if (blacklist.contains(userId)) {
            return false;
        }
        Set<String> whitelist = parseUserIds(activityDccConfig.getActivityWhitelist());
        return whitelist.isEmpty() || whitelist.contains(userId);
    }

    /**
     * 校验用户 ID。
     *
     * @param userId 待校验的用户 ID
     * @throws IllegalArgumentException 当用户 ID 为空或只包含空白字符时抛出
     */
    private void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
    }

    /**
     * 解析逗号分隔的用户 ID 字符串。
     *
     * <p>解析时会自动去除每个用户 ID 两侧的空白字符，
     * 并忽略连续逗号产生的空元素。</p>
     *
     * @param rawUserIds DCC 下发的原始用户名单
     * @return 不可变用户 ID 集合；原始值为空时返回空集合
     */
    private Set<String> parseUserIds(String rawUserIds) {
        if (rawUserIds == null || rawUserIds.isBlank()) {
            return Set.of();
        }

        return Arrays.stream(rawUserIds.split(","))
                .map(String::trim)
                .filter(userId -> !userId.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }
}
