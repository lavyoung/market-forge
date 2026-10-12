package com.lavyoung.marketforge.domain.activity.service.quota.impl;

import com.lavyoung.marketforge.domain.activity.model.aggregate.CreateQuotaOrderAggregate;
import com.lavyoung.marketforge.domain.activity.model.entity.*;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivitySkuStockKeyVO;
import com.lavyoung.marketforge.domain.activity.model.vo.OrderStateVO;
import com.lavyoung.marketforge.domain.activity.repository.IActivityRepository;
import com.lavyoung.marketforge.domain.activity.repository.IActivitySkuStockMessageRepository;
import com.lavyoung.marketforge.domain.activity.service.quota.AbstractRaffleRaffleActivityAccountQuotaService;
import com.lavyoung.marketforge.domain.activity.service.quota.policy.ITradePolicy;
import com.lavyoung.marketforge.domain.activity.service.quota.rule.factory.DefaultActivityChainFactory;
import com.lavyoung.marketforge.types.common.Constants;
import com.lavyoung.marketforge.types.utils.DateUtil;
import com.lavyoung.marketforge.types.utils.IdGenerator;
import org.springframework.stereotype.Service;

import java.util.Map;

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

    private final IActivityRepository activityRepository;

    public RaffleRaffleActivityAccountQuotaServiceServiceImpl(IActivityRepository activityRepository, IActivitySkuStockMessageRepository activitySkuStockMessageRepository, DefaultActivityChainFactory activityChainFactory, Map<String, ITradePolicy> tradePolicyGroup) {
        super(activityRepository, activitySkuStockMessageRepository, activityChainFactory, tradePolicyGroup);
        this.activityRepository = activityRepository;
    }

    @Override
    protected void doUpdateOrder(DeliveryOrderEntity deliveryOrderEntity) {
        activityRepository.deliverCreditPayOrder(deliveryOrderEntity);
    }

    /**
     * 构建活动 SKU 充值订单聚合。
     * <p>
     * 充值订单代表用户通过签到、兑换、购买等方式获得抽奖次数的入账结果。订单状态直接置为完成，
     * 并保留外部业务号作为幂等依据，后续由仓储负责写入订单并累计总、日、月账户额度。
     *
     * @param skuRechargeEntity   SKU 充值请求
     * @param activitySkuEntity   活动 SKU 配置
     * @param activityEntity      活动配置
     * @param activityCountEntity 次数配置
     * @return 创建额度订单聚合
     */
    @Override
    protected CreateQuotaOrderAggregate buildOrderAggregate(SkuRechargeEntity skuRechargeEntity, ActivitySkuEntity activitySkuEntity, ActivityEntity activityEntity, ActivityCountEntity activityCountEntity) {
        ActivityOrderEntity activityOrderEntity = ActivityOrderEntity.builder()
                .userId(skuRechargeEntity.userId())
                .activityId(activitySkuEntity.activityId())
                .sku(skuRechargeEntity.sku())
                .activityName(activityEntity.activityName())
                .strategyId(activityEntity.strategyId())
                .endDateTime(activityEntity.endDateTime())
                .orderId(Constants.BusinessNoPrefix.RAFFLE_ORDER_PREFIX + IdGenerator.nextId())
                .orderTime(DateUtil.now())
                .totalCount(activityCountEntity.totalCount())
                .dayCount(activityCountEntity.dayCount())
                .monthCount(activityCountEntity.monthCount())
                .state(resolveOrderState(skuRechargeEntity).getCode())
                .outBusinessNo(skuRechargeEntity.outBusinessNo())
                .build();
        return CreateQuotaOrderAggregate.builder()
                .userId(skuRechargeEntity.userId())
                .activityId(activitySkuEntity.activityId())
                .totalCount(activityCountEntity.totalCount())
                .dayCount(activityCountEntity.dayCount())
                .monthCount(activityCountEntity.monthCount())
                .activityOrder(activityOrderEntity)
                .build();
    }

    /**
     * 根据活动 SKU 订单交易类型解析初始订单状态。
     *
     * <p>无支付交易创建后即可完成；积分支付交易创建后必须等待积分扣减成功，
     * 因此初始状态为待支付。</p>
     *
     * @param skuRechargeEntity 活动 SKU 下单请求
     * @return 初始订单状态
     */
    private OrderStateVO resolveOrderState(SkuRechargeEntity skuRechargeEntity) {
        return switch (skuRechargeEntity.orderTradeType()) {
            case REBATE_NO_PAY -> OrderStateVO.COMPLETE;
            case CREDIT_PAY -> OrderStateVO.WAIT_PAY;
        };
    }

    /**
     * 获取一条待同步的活动 SKU 库存消息。
     *
     * @return 活动 SKU 库存消息；队列为空时返回 {@code null}
     * @throws Exception 当底层队列读取失败时抛出
     */
    @Override
    public ActivitySkuStockKeyVO takeQueueValue() throws Exception {
        return activitySkuStockMessageRepository.poll().orElse(null);
    }

    /**
     * 清空活动 SKU 库存同步队列。
     * <p>
     * 当前版本尚未暴露批量清理能力，调用该方法会直接抛出不支持异常，避免调用方误以为清理成功。
     *
     * @throws UnsupportedOperationException 当前方法暂未支持时抛出
     */
    @Override
    public void clearQueueValue() {
        throw new UnsupportedOperationException("暂不支持");
    }

    /**
     * 根据库存同步消息扣减数据库侧活动 SKU 库存。
     *
     * @param sku 活动 SKU
     */
    @Override
    public void updateActivitySkuStock(Long sku) {
        activityRepository.updateActivitySkuStock(sku);
    }

    /**
     * 清空指定活动 SKU 的数据库库存和缓存库存。
     *
     * @param sku 活动 SKU
     */
    @Override
    public void clearActivitySkuStock(Long sku) {
        activityRepository.clearActivitySkuStock(sku);
    }
}
