package com.lavyoung.marketforge.domain.activity.service.quota;

import com.lavyoung.marketforge.domain.activity.repository.IActivityRepository;
import com.lavyoung.marketforge.domain.activity.service.quota.rule.factory.DefaultActivityChainFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 活动域服务支持基类。
 * <p>
 * 统一持有活动仓储端口和活动规则责任链工厂，减少具体领域服务的重复依赖声明。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/20
 */
@Component
@RequiredArgsConstructor
public class RaffleActivitySupport {

    protected final IActivityRepository activityRepository;

    protected final DefaultActivityChainFactory activityChainFactory;

}
