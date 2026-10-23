package com.lavyoung.marketforge.application.award.grant;

import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 奖品配置校验器注册表。
 *
 * <p>注册表在 Spring 容器启动时按照“奖品类型 + 配置版本”建立不可变索引。
 * 同一协议存在多个校验器时立即阻止应用启动，避免运行时随机选择实现。</p>
 *
 * <p>运营奖品启用流程只需要向注册表提交类型、版本和配置内容，
 * 不需要依赖任何具体奖品校验器。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/23
 */
@Component
public class AwardConfigValidatorRegistry {

    private final Map<ValidatorKey, AwardConfigValidator> validators;

    /**
     * 收集并校验 Spring 容器中的配置校验器。
     *
     * @param validatorList Spring 容器发现的全部配置校验器
     * @throws NullPointerException  校验器集合或其中元素为空时抛出
     * @throws IllegalStateException 类型为空白、版本非法或协议重复注册时抛出
     */
    public AwardConfigValidatorRegistry(List<AwardConfigValidator> validatorList) {
        Objects.requireNonNull(validatorList, "validatorList must not be null");
        Map<ValidatorKey, AwardConfigValidator> registeredValidators = new HashMap<>();

        for (AwardConfigValidator validator : validatorList) {
            Objects.requireNonNull(validator, "award config validator must not be null");
            ValidatorKey key = createKey(validator.awardType(), validator.configVersion());

            AwardConfigValidator previous = registeredValidators.putIfAbsent(key, validator);
            if (previous != null) {
                throw new IllegalStateException(
                        "duplicate award config validator: " + key.awardType() + ":" + key.configVersion());
            }
        }

        this.validators = Map.copyOf(registeredValidators);
    }

    /**
     * 按奖品类型和配置版本执行配置校验。
     *
     * @param awardType     奖品类型编码
     * @param configVersion 配置结构版本
     * @param configContent 配置内容
     * @throws BusinessException 未注册对应协议或配置内容非法时抛出
     */
    public void validate(String awardType, int configVersion, String configContent) {
        ValidatorKey key = toBusinessKey(awardType, configVersion);
        AwardConfigValidator validator = validators.get(key);

        if (validator == null) {
            throw BusinessException.of(
                    BusinessResponseCode.AWARD_CONFIG_INVALID,
                    awardType,
                    configVersion
            );
        }

        validator.validate(configContent);
    }

    /**
     * 判断系统是否支持指定奖品配置协议。
     *
     * @param awardType     奖品类型编码
     * @param configVersion 配置结构版本
     * @return 已注册对应校验器时返回 {@code true}
     */
    public boolean supports(String awardType, int configVersion) {
        if (awardType == null || awardType.isBlank() || configVersion <= 0) {
            return false;
        }
        return validators.containsKey(new ValidatorKey(awardType, configVersion));
    }

    /**
     * 创建启动阶段使用的注册键。
     *
     * @param awardType     校验器声明的奖品类型
     * @param configVersion 校验器声明的配置版本
     * @return 通过启动期约束检查的注册键
     * @throws IllegalStateException 类型为空白或版本非法时抛出
     */
    private ValidatorKey createKey(String awardType, int configVersion) {
        if (awardType == null || awardType.isBlank()) {
            throw new IllegalStateException("award config validator type must not be blank");
        }
        if (configVersion <= 0) {
            throw new IllegalStateException("award config validator version must be positive");
        }
        return new ValidatorKey(awardType, configVersion);
    }

    /**
     * 创建业务调用使用的注册键。
     *
     * @param awardType     奖品类型
     * @param configVersion 配置版本
     * @return 用于查询校验器的注册键
     * @throws BusinessException 类型为空或版本非法时抛出
     */
    private ValidatorKey toBusinessKey(String awardType, int configVersion) {
        if (awardType == null || awardType.isBlank() || configVersion <= 0) {
            throw BusinessException.of(
                    BusinessResponseCode.AWARD_CONFIG_INVALID,
                    awardType,
                    configVersion
            );
        }
        return new ValidatorKey(awardType, configVersion);
    }

    /**
     * 奖品配置协议注册键。
     *
     * @param awardType     奖品类型
     * @param configVersion 配置版本
     */
    private record ValidatorKey(String awardType, int configVersion) {
    }
}
