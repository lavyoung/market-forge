package com.lavyoung.marketforge.application.activity.service;

/**
 * 活动用户访问策略端口。
 *
 * <p>该接口由 application 层定义，用于表达活动业务需要的用户访问判断能力。
 * application 层只关心用户是否允许参与活动，不关心名单配置来自 ZooKeeper、
 * Redis、数据库还是其他配置中心。</p>
 *
 * <p>具体实现负责解释活动白名单、黑名单以及相关业务规则。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/13
 */
public interface IActivityAccessPolicy {

    /**
     * 判断用户是否允许参与活动。
     *
     * <p>推荐的默认规则如下：</p>
     *
     * <ul>
     *     <li>用户命中黑名单时，直接拒绝访问；</li>
     *     <li>黑名单为空且白名单为空时，允许访问；</li>
     *     <li>白名单不为空时，仅允许白名单用户访问；</li>
     *     <li>黑名单优先级高于白名单。</li>
     * </ul>
     *
     * @param userId 待判断的用户 ID，不能为空
     * @return {@code true} 表示允许参与活动；
     * {@code false} 表示拒绝参与活动
     * @throws IllegalArgumentException 当 {@code userId} 为空或只包含空白字符时抛出
     */
    boolean isAllowed(String userId);
}
