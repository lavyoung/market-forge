package com.lavyoung.marketforge.domain.activity.service.impl;

import com.lavyoung.marketforge.domain.activity.model.aggregate.CreateQuotaOrderAggregate;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityCountEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivitySkuEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.SkuRechargeEntity;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivitySkuStockKeyVO;
import com.lavyoung.marketforge.domain.activity.repository.IActivityRepository;
import com.lavyoung.marketforge.domain.activity.service.quota.AbstractRaffleRaffleActivityAccountQuotaService;
import com.lavyoung.marketforge.domain.activity.service.quota.rule.factory.DefaultActivityChainFactory;
import org.springframework.stereotype.Service;

/**
 * 默认活动账户额度服务实现。
 * <p>
 * 承接 SKU 充值订单创建流程，并作为活动 SKU 库存异步同步用例的领域服务入口。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/20
 */
@Service
public class RaffleRaffleActivityAccountQuotaServiceServiceImpl extends AbstractRaffleRaffleActivityAccountQuotaService {

    public RaffleRaffleActivityAccountQuotaServiceServiceImpl(IActivityRepository activityRepository, DefaultActivityChainFactory activityChainFactory) {
        super(activityRepository, activityChainFactory);
    }

    @Override
    protected void doSaveOrder(CreateQuotaOrderAggregate createQuotaOrderAggregate) {
        activityRepository.saveOrderAggregate(createQuotaOrderAggregate);
    }

    @Override
    protected CreateQuotaOrderAggregate buildOrderAggregate(SkuRechargeEntity skuRechargeEntity, ActivitySkuEntity activitySkuEntity, ActivityEntity activityEntity, ActivityCountEntity activityCountEntity) {
        // todo
        return null;
    }

    @Override
    public ActivitySkuStockKeyVO takeQueueValue() throws Exception {
        return null;
    }

    @Override
    public void clearQueueValue() {

    }

    @Override
    public void updateActivitySkuStock(Long sku) {
        // todo 更新库存
    }

    @Override
    public void clearActivitySkuStock(Long sku) {
        // todo
    }
}
