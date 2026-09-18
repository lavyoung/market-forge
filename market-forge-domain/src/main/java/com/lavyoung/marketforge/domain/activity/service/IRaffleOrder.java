package com.lavyoung.marketforge.domain.activity.service;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivityOrderEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityShopCartEntity;

/**
 *
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/18
 */
public interface IRaffleOrder {

    ActivityOrderEntity createRaffleActivityOrder(ActivityShopCartEntity activityShopCart);
}
