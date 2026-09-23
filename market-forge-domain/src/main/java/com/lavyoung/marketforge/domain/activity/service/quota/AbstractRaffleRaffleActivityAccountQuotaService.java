package com.lavyoung.marketforge.domain.activity.service.quota;

import com.lavyoung.marketforge.domain.activity.model.aggregate.CreateQuotaOrderAggregate;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityCountEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivitySkuEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.SkuRechargeEntity;
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
     * 创建活动 SKU 充值订单。
     * <p>
     * 该方法是“抽奖次数入账”的模板流程：校验请求参数、查询 SKU/活动/次数配置、执行活动责任链
     * 校验并预扣库存，随后构建额度订单聚合并交给子类保存。返回的订单号可用于调用方追踪本次
     * 充值入账结果。
     *
     * @param skuRechargeEntity 活动 SKU 充值请求，包含用户、SKU 和外部业务幂等号
     * @return 活动额度充值订单号
     * @throws BusinessException 当参数非法、配置缺失、活动不可用或库存扣减失败时抛出
     */
    @Override
    public String createSkuRechargeOrder(SkuRechargeEntity skuRechargeEntity) {
        // 参数校验
        String userId = skuRechargeEntity.userId();
        Long sku = skuRechargeEntity.sku();
        String outBusinessNo = skuRechargeEntity.outBusinessNo();
        if (sku == null || StringUtils.isAnyBlank(userId, outBusinessNo)) {
            throw BusinessException.of(CommonResponseCode.PARAM_INVALID, skuRechargeEntity);
        }
        // 查询基本信息
        // 通过sku查询活动信息
        ActivitySkuEntity activitySkuEntity = activityRepository.queryActivitySku(sku);
        if (activitySkuEntity == null) {
            log.warn("创建SKU库存充值订单 SKU商品不存在，sku={} outBusinessNo={}, userId={}", sku, outBusinessNo, userId);
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_SKU_NOT_FOUND, userId, sku, outBusinessNo);
        }
        // 查询活动信息
        ActivityEntity activityEntity = activityRepository.getActivityEntityByIdActivityId(activitySkuEntity.activityId());
        if (activityEntity == null) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_NOT_FOUND, userId, sku, activitySkuEntity.activityId(), outBusinessNo);
        }
        // 查询次数信息（用户在活动上可参与的次数）
        ActivityCountEntity activityCountEntity = activityRepository.queryRaffleActivityCountByActivityCountId(activitySkuEntity.activityCountId());
        if (activityCountEntity == null) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_COUNT_NOT_FOUND,
                    userId, sku, activitySkuEntity.activityId(), activitySkuEntity.activityCountId(), outBusinessNo);
        }
        // 活动规则校验
        IActionChain chain = activityChainFactory.openActionChain();
        boolean success = chain.action(activitySkuEntity, activityEntity, activityCountEntity);
        if (!success) {
            log.warn("创建SKU库存充值订单，SKU库存不足，库存扣减失败 userId={}, sku={}", userId, sku);
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_SKU_STOCK_DEDUCT_FAILED, userId, sku);
        }
        // 构建订单对象
        CreateQuotaOrderAggregate createQuotaOrderAggregate = buildOrderAggregate(skuRechargeEntity, activitySkuEntity, activityEntity, activityCountEntity);
        // 保存订单
        doSaveOrder(createQuotaOrderAggregate);
        // 返回单号
        return createQuotaOrderAggregate.activityOrder().orderId();
    }

    /**
     * 保存活动 SKU 充值订单聚合。
     * <p>
     * 该扩展点负责完成“抽奖次数入账”侧的持久化：创建活动额度订单、更新或创建用户活动账户，
     * 并记录外部业务单号的幂等结果。实现方应保证订单与账户变更在同一事务内完成，避免出现
     * 订单创建成功但账户次数未增加，或账户次数增加但订单缺失的中间状态。
     *
     * @param createQuotaOrderAggregate 创建额度订单聚合，包含充值订单、用户、活动 SKU、次数配置和幂等号
     * @throws BusinessException 当订单重复、账户入账失败或持久化状态不一致时抛出
     */
    protected abstract void doSaveOrder(CreateQuotaOrderAggregate createQuotaOrderAggregate);

    /**
     * 构建活动 SKU 充值订单聚合。
     * <p>
     * 该扩展点只负责把已校验通过的充值请求转换为领域聚合，不应再次扣减库存或访问外部资源。
     * 聚合中的活动订单代表一次“给用户增加抽奖次数”的入账单，通常应携带用户标识、活动标识、
     * SKU、抽奖策略、总/月/日次数、外部业务幂等号和订单完成状态。
     *
     * @param skuRechargeEntity   活动 SKU 充值实体
     * @param activitySkuEntity   活动 SKU 实体
     * @param activityEntity      活动实体
     * @param activityCountEntity 活动次数配置实体
     * @return 创建额度订单聚合，用于后续一次性保存订单和账户额度
     * @throws BusinessException 当必要配置缺失、次数配置非法或订单聚合无法构建时抛出
     */
    protected abstract CreateQuotaOrderAggregate buildOrderAggregate(SkuRechargeEntity skuRechargeEntity, ActivitySkuEntity activitySkuEntity, ActivityEntity activityEntity, ActivityCountEntity activityCountEntity);
}
