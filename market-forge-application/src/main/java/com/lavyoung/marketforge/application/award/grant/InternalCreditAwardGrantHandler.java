package com.lavyoung.marketforge.application.award.grant;

import com.lavyoung.marketforge.application.award.model.GrantAwardCommand;
import com.lavyoung.marketforge.domain.award.event.AwardFulfillmentSnapshot;
import com.lavyoung.marketforge.domain.award.model.vo.AwardClaimModeVO;
import com.lavyoung.marketforge.domain.credit.model.vo.CreditAccountBusinessTypeVO;
import com.lavyoung.marketforge.domain.credit.service.ICreditAccountService;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 平台内部积分奖品发放处理器。
 *
 * <p>本处理器负责 {@code internal_credit} 类型奖品的真实履约，完整流程如下：</p>
 *
 * <ol>
 *     <li>校验发奖命令和履约快照中的奖品类型。</li>
 *     <li>校验配置版本，当前仅支持版本 1。</li>
 *     <li>通过配置解码端口将 JSON 转换为 {@link InternalCreditAwardConfig}。</li>
 *     <li>使用抽奖订单号作为积分流水的业务幂等号。</li>
 *     <li>调用积分领域服务完成积分入账。</li>
 *     <li>重复积分流水视为幂等成功，其他异常继续向上抛出。</li>
 * </ol>
 *
 * <p>本处理器不修改中奖记录状态。只有处理器成功返回后，上层发奖应用服务
 * 才能将中奖记录推进到已完成状态。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/23
 */
@Component
@RequiredArgsConstructor
public class InternalCreditAwardGrantHandler implements AwardGrantHandler {

    private final AwardConfigDecoder configDecoder;
    private final ICreditAccountService creditAccountService;

    @Override
    public String awardType() {
        return InternalCreditAwardConfig.AWARD_TYPE;
    }

    @Override
    public boolean supportsClaimMode(String claimMode) {
        return AwardClaimModeVO.AUTOMATIC.getCode().equals(claimMode);
    }

    @Override
    public void grant(GrantAwardCommand command) {
        GrantAwardCommand validCommand = Objects.requireNonNull(command, "command must not be null");
        AwardFulfillmentSnapshot snapshot = validCommand.fulfillmentSnapshot();

        if (!InternalCreditAwardConfig.AWARD_TYPE.equals(snapshot.awardType())) {
            throw BusinessException.of(
                    BusinessResponseCode.AWARD_GRANT_NOT_SUPPORTED,
                    snapshot.awardType(),
                    validCommand.awardId()
            );
        }
        if (snapshot.configVersion() != InternalCreditAwardConfig.CONFIG_VERSION) {
            throw BusinessException.of(
                    BusinessResponseCode.AWARD_CONFIG_INVALID,
                    snapshot.awardType(),
                    snapshot.configVersion()
            );
        }

        InternalCreditAwardConfig config = configDecoder.decode(snapshot.configContent(), InternalCreditAwardConfig.class);
        try {
            creditAccountService.grantCredit(
                    validCommand.userId(),
                    validCommand.orderId(),
                    config.amount(),
                    CreditAccountBusinessTypeVO.AWARD_GRANT
            );
        } catch (BusinessException exception) {
            if (exception.getResponseCode() != BusinessResponseCode.POINTS_TRANSACTION_DUPLICATED) {
                throw exception;
            }
        }
    }
}
