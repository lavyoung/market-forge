package com.lavyoung.marketforge.types.concurrent;

/**
 * 项目内统一线程池名称。
 * <p>
 * 应用层通过这些常量从 {@link NamedExecutorProvider} 获取线程池，避免散落硬编码名称。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/29
 */
public final class ThreadPoolNames {
    /**
     * 消息投递线程池
     */
    public static final String MESSAGE_TASK = "message-task";

    /**
     * 奖励发放线程池
     */
    public static final String AWARD_GRANT = "award-grant";

    private ThreadPoolNames() {
    }
}
