package com.lavyoung.marketforge.domain.activity.service.impl;

import com.lavyoung.marketforge.domain.activity.repository.IActivityRepository;
import com.lavyoung.marketforge.domain.activity.service.AbstractRaffleActivity;
import org.springframework.stereotype.Service;

/**
 *
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/20
 */
@Service
public class RaffleActivityServiceImpl extends AbstractRaffleActivity {

    public RaffleActivityServiceImpl(IActivityRepository activityRepository) {
        super(activityRepository);
    }


}
