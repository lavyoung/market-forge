package com.lavyoung.marketforge.application.award.grant.internalcredit;

import com.lavyoung.marketforge.application.award.grant.AwardConfigDecoder;
import com.lavyoung.marketforge.application.award.grant.AwardConfigValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 平台内部积分奖品版本 1 配置校验器。
 *
 * <p>版本 1 的配置格式为：</p>
 *
 * <pre>{@code
 * {
 *   "amount": 100
 * }
 * }</pre>
 *
 * <p>校验器通过配置解码端口完成 JSON 结构、字段类型、未知字段和积分数量校验。
 * 成功解码为 {@link InternalCreditAwardConfig} 即表示配置合法。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/23
 */
@Component
@RequiredArgsConstructor
public class InternalCreditAwardConfigValidator implements AwardConfigValidator {

    private final AwardConfigDecoder configDecoder;

    @Override
    public String awardType() {
        return InternalCreditAwardConfig.AWARD_TYPE;
    }

    @Override
    public int configVersion() {
        return InternalCreditAwardConfig.CONFIG_VERSION;
    }

    @Override
    public void validate(String configContent) {
        configDecoder.decode(configContent, InternalCreditAwardConfig.class);
    }
}
