package com.lavyoung.marketforge.application.credit.service.impl;

import com.lavyoung.marketforge.application.credit.model.ExchangeCreditForSkuCommand;
import com.lavyoung.marketforge.application.credit.service.ICreditExchangeApplicationService;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivitySkuEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.SkuRechargeEntity;
import com.lavyoung.marketforge.domain.activity.model.vo.OrderTradeTypeVO;
import com.lavyoung.marketforge.domain.activity.repository.IActivityRepository;
import com.lavyoung.marketforge.domain.activity.service.IRaffleActivityAccountQuotaService;
import com.lavyoung.marketforge.domain.credit.model.entity.CreditAdjustEntity;
import com.lavyoung.marketforge.domain.credit.model.vo.CreditAccountBusinessTypeVO;
import com.lavyoung.marketforge.domain.credit.model.vo.CreditAccountTradeTypeVO;
import com.lavyoung.marketforge.domain.credit.service.ICreditAccountService;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * 积分兑换应用服务默认实现。
 *
 * <p>该实现负责完成“待支付活动 SKU 下单 + 积分支付”的跨领域编排。
 * 积分支付成功后，积分账户仓储会写入可靠消息任务；消息消费者收到积分调额成功事件后，
 * 再将待支付活动订单发货入账，给用户增加对应抽奖次数。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreditExchangeApplicationServiceImpl implements ICreditExchangeApplicationService {

    private final ICreditAccountService creditAccountService;
    private final IRaffleActivityAccountQuotaService raffleActivityAccountQuotaService;

    private final IActivityRepository activityRepository;

    /**
     * 使用积分兑换活动 SKU 对应的抽奖次数。
     *
     * <p>处理顺序如下：</p>
     * <ol>
     *     <li>校验调用方提供的用户、SKU 和外部幂等号。</li>
     *     <li>按 SKU 查询服务端积分价格并校验其可用性。</li>
     *     <li>创建积分支付类型的待支付活动 SKU 订单。</li>
     *     <li>依据服务端价格扣减用户积分。</li>
     * </ol>
     *
     * <p>积分扣减完成后，会由可靠消息异步触发活动订单发货与抽奖次数入账。</p>
     *
     * @param command 积分兑换活动 SKU 命令
     * @return 活动 SKU 订单号
     * @throws BusinessException 当参数非法、SKU 不存在、SKU 未配置积分价格、
     *                           积分不足或订单创建失败时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String exchangeCreditForSku(ExchangeCreditForSkuCommand command) {
        ExchangeCreditForSkuCommand validateCommand = validateCommand(command);
        Long creditAmount = resolveCreditAmount(validateCommand.sku());
        // 1. 先创建积分支付类型的活动 SKU 订单。
        // CREDIT_PAY 交易不会立即给用户增加抽奖次数，而是先写入 WAIT_PAY 待支付订单。
        String orderId = raffleActivityAccountQuotaService.createSkuRechargeOrder(new SkuRechargeEntity(
                validateCommand.userId(),
                validateCommand.sku(),
                validateCommand.outBusinessNo(),
                OrderTradeTypeVO.CREDIT_PAY
        ));

        // 2. 再扣减用户积分。
        // 扣减成功后，积分仓储会写入 CreditAdjustSuccessEvent 可靠消息任务，
        // 后续 MQ 消费者根据 userId + outBusinessNo 发货入账。
        creditAccountService.adjustCredit(new CreditAdjustEntity(
                validateCommand.userId(),
                validateCommand.outBusinessNo(),
                creditAmount,
                CreditAccountTradeTypeVO.EXPENSE,
                CreditAccountBusinessTypeVO.EXCHANGE
        ));
        return orderId;
    }

    /**
     * 校验积分兑换命令的调用方输入。
     *
     * <p>积分价格不属于客户端输入，因此不在这里校验；
     * 它会由 {@link #resolveCreditAmount(Long)} 从服务端配置中读取。</p>
     *
     * @param command 待校验的积分兑换命令
     * @return 校验通过的原始命令
     * @throws BusinessException 当用户、SKU 或外部业务幂等号为空时抛出
     */
    private ExchangeCreditForSkuCommand validateCommand(ExchangeCreditForSkuCommand command) {
        ExchangeCreditForSkuCommand validCommand = Objects.requireNonNull(command, "command must not be null");
        if (StringUtils.isAnyBlank(validCommand.userId(), validCommand.outBusinessNo())
                || validCommand.sku() == null || validCommand.sku() <= 0) {
            throw BusinessException.of(
                    BusinessResponseCode.POINTS_TRANSACTION_AMOUNT_INVALID,
                    validCommand.userId(),
                    validCommand.sku(),
                    validCommand.outBusinessNo()
            );
        }
        return validCommand;
    }

    /**
     * 查询并校验活动 SKU 的服务端积分兑换价格。
     *
     * <p>积分价格必须由运营配置决定。SKU 不存在时返回“SKU 不存在”异常；
     * 价格为空、零或负数时说明该 SKU 不支持积分兑换或配置错误，
     * 不能创建待支付订单，更不能继续扣减积分。</p>
     *
     * @param sku 活动商品 SKU
     * @return 兑换该 SKU 需要消耗的正整数积分
     * @throws BusinessException 当 SKU 不存在或积分价格无效时抛出
     */
    private Long resolveCreditAmount(Long sku) {
        ActivitySkuEntity activitySku = activityRepository.queryActivitySku(sku);
        if (activitySku == null) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_SKU_NOT_FOUND, sku);
        }
        if (activitySku.creditAmount() == null || activitySku.creditAmount() <= 0) {
            throw BusinessException.of(
                    BusinessResponseCode.ACTIVITY_SKU_CREDIT_AMOUNT_INVALID,
                    sku,
                    activitySku.creditAmount()
            );
        }
        return activitySku.creditAmount();
    }
}
