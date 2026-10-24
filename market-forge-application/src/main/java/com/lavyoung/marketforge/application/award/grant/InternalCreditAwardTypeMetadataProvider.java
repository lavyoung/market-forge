package com.lavyoung.marketforge.application.award.grant;

import com.lavyoung.marketforge.application.award.model.AwardConfigFieldMetadata;
import com.lavyoung.marketforge.application.award.model.AwardConfigFieldType;
import com.lavyoung.marketforge.application.award.model.AwardTypeMetadata;
import com.lavyoung.marketforge.domain.award.model.vo.AwardClaimModeVO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;


/**
 * 平台积分奖品类型的运营配置元数据提供者。
 *
 * <p>积分奖品当前只支持自动发放，版本 1 的配置只包含正整数
 * {@code amount}。这里描述配置结构，不执行 JSON 解析或积分入账。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/24
 */
@Component
public class InternalCreditAwardTypeMetadataProvider implements AwardTypeMetadataProvider {

    private static final AwardTypeMetadata METADATA = new AwardTypeMetadata(
            InternalCreditAwardConfig.AWARD_TYPE,
            "平台积分",
            List.of(AwardClaimModeVO.AUTOMATIC.getCode()),
            InternalCreditAwardConfig.CONFIG_VERSION,
            List.of(new AwardConfigFieldMetadata("amount", "积分数量", AwardConfigFieldType.INTEGER, true,
                    "中奖后自动发放到用户积分账户的积分数量", Map.of("minimum", "1", "step", "1"))),
            "中奖后由平台积分账户服务自动完成幂等入账"
    );

    @Override
    public AwardTypeMetadata metadata() {
        return METADATA;
    }
}
