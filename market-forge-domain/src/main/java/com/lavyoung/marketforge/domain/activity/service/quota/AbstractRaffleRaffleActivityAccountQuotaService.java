package com.lavyoung.marketforge.domain.activity.service.quota;

import com.lavyoung.marketforge.domain.activity.model.aggregate.CreateQuotaOrderAggregate;
import com.lavyoung.marketforge.domain.activity.model.entity.*;
import com.lavyoung.marketforge.domain.activity.repository.IActivityRepository;
import com.lavyoung.marketforge.domain.activity.service.IRaffleActivityAccountQuotaService;
import com.lavyoung.marketforge.domain.activity.service.IRaffleActivitySkuStockService;
import com.lavyoung.marketforge.domain.activity.service.quota.rule.IActionChain;
import com.lavyoung.marketforge.domain.activity.service.quota.rule.factory.DefaultActivityChainFactory;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import com.lavyoung.marketforge.types.model.CommonResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * 活动账户额度创建流程模板。
 * <p>
 * 统一处理 SKU、活动、次数配置查询和活动规则校验，子类负责构建聚合并完成持久化。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/18
 */
@Slf4j
public abstract class AbstractRaffleRaffleActivityAccountQuotaService extends RaffleActivitySupport implements IRaffleActivityAccountQuotaService, IRaffleActivitySkuStockService {

    public AbstractRaffleRaffleActivityAccountQuotaService(IActivityRepository activityRepository, DefaultActivityChainFactory activityChainFactory) {
        super(activityRepository, activityChainFactory);
    }

    /**
     * 根据活动商品购物车创建活动订单。
     *
     * @param activityShopCart 活动商品购物车实体
     * @return 活动订单实体
     */
    @Override
    @Deprecated
    public ActivityOrderEntity createRaffleActivityOrder(ActivityShopCartEntity activityShopCart) {

        // 通过sku查询活动信息
        ActivitySkuEntity activitySkuEntity = activityRepository.queryActivitySku(activityShopCart.sku());
        // 查询活动信息
        ActivityEntity activityEntity = activityRepository.getActivityEntityByIdActivityId(activitySkuEntity.activityId());
        // 查询次数信息（用户在活动上可参与的次数）
        ActivityCountEntity activityCountEntity = activityRepository.queryRaffleActivityCountByActivityCountId(activitySkuEntity.activityCountId());
        log.info("");

        return ActivityOrderEntity.builder().build();
    }

    @Override
    public String createSkuRechargeOrder(SkuRechargeEntity skuRechargeEntity) {
        // 参数校验
        String userId = skuRechargeEntity.userId();
        Long sku = skuRechargeEntity.sku();
        String outBusinessNo = skuRechargeEntity.outBusinessNo();
        if (sku == null || StringUtils.isAnyBlank(userId, outBusinessNo)) {
            throw new BusinessException(CommonResponseCode.PARAM_INVALID);
        }
        // 查询基本信息
        // 通过sku查询活动信息
        ActivitySkuEntity activitySkuEntity = activityRepository.queryActivitySku(sku);
        // 查询活动信息
        ActivityEntity activityEntity = activityRepository.getActivityEntityByIdActivityId(activitySkuEntity.activityId());
        // 查询次数信息（用户在活动上可参与的次数）
        ActivityCountEntity activityCountEntity = activityRepository.queryRaffleActivityCountByActivityCountId(activitySkuEntity.activityCountId());
        // 活动规则校验
        IActionChain chain = activityChainFactory.openActionChain();
        boolean success = chain.action(activitySkuEntity, activityEntity, activityCountEntity);
        if (!success) {
            // todo
            throw new BusinessException(BusinessResponseCode.POINTS_TRANSACTION_DUPLICATED);
        }
        // 构建订单对象
        CreateQuotaOrderAggregate createQuotaOrderAggregate = buildOrderAggregate(skuRechargeEntity, activitySkuEntity, activityEntity, activityCountEntity);
        // 保存订单
        doSaveOrder(createQuotaOrderAggregate);
        // 返回单号
        return createQuotaOrderAggregate.activityOrder().orderId();
    }

    /**
     * 保存创建额度订单聚合。
     *
     * @param createQuotaOrderAggregate 创建额度订单聚合
     */
    protected abstract void doSaveOrder(CreateQuotaOrderAggregate createQuotaOrderAggregate);

    /**
     * 根据充值请求、活动 SKU、活动详情和次数配置构建额度订单聚合。
     *
     * @param skuRechargeEntity   活动 SKU 充值实体
     * @param activitySkuEntity   活动 SKU 实体
     * @param activityEntity      活动实体
     * @param activityCountEntity 活动次数配置实体
     * @return 创建额度订单聚合
     */
    protected abstract CreateQuotaOrderAggregate buildOrderAggregate(SkuRechargeEntity skuRechargeEntity, ActivitySkuEntity activitySkuEntity, ActivityEntity activityEntity, ActivityCountEntity activityCountEntity);
}
