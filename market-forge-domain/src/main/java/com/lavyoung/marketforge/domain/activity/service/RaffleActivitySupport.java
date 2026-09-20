package com.lavyoung.marketforge.domain.activity.service;

import com.lavyoung.marketforge.domain.activity.repository.IActivityRepository;
import com.lavyoung.marketforge.domain.activity.service.rule.factory.DefaultActivityChainFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 支持类
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
