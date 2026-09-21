package com.lavyoung.marketforge.domain.activity.service.armory;

import java.time.LocalDateTime;

/**
 *
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/21
 */
public interface IActivityDispatch {

    boolean subtractionActivitySkuStock(Long sku, LocalDateTime endDateTime);
}
