package com.lavyoung.marketforge.application.award.metadata;

import com.lavyoung.marketforge.application.award.grant.AwardConfigValidatorRegistry;
import com.lavyoung.marketforge.application.award.grant.AwardGrantHandler;
import com.lavyoung.marketforge.application.award.grant.AwardGrantHandlerRegistry;
import com.lavyoung.marketforge.application.award.model.AwardClaimModeMetadata;
import com.lavyoung.marketforge.application.award.model.AwardConfigFieldMetadata;
import com.lavyoung.marketforge.application.award.model.AwardTypeMetadata;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 奖品类型运营元数据注册表。
 *
 * <p>注册表在 Spring 容器启动时收集全部
 * {@link AwardTypeMetadataContributor}，并按照奖品类型建立不可变索引。</p>
 *
 * <p>每项元数据都会与真实发奖处理器和配置校验器进行交叉验证。
 * 任意声明不一致都会阻止应用启动，避免运营后台展示无法真实履约的奖品类型。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/24
 */
@Component
public class AwardTypeMetadataRegistry {

    private final Map<String, AwardTypeMetadata> metadataByType;
    private final List<AwardTypeMetadata> orderedMetadata;

    /**
     * 收集、验证并冻结所有奖品类型元数据。
     *
     * @param contributors            Spring 容器发现的全部元数据贡献者
     * @param grantHandlerRegistry    发奖处理器注册表
     * @param configValidatorRegistry 配置校验器注册表
     * @throws NullPointerException  依赖、贡献者或贡献结果为空时抛出
     * @throws IllegalStateException 没有可用类型、类型重复、字段重复或履约能力不匹配时抛出
     */
    public AwardTypeMetadataRegistry(List<AwardTypeMetadataContributor> contributors,
                                     AwardGrantHandlerRegistry grantHandlerRegistry,
                                     AwardConfigValidatorRegistry configValidatorRegistry) {
        Objects.requireNonNull(contributors, "contributors must not be null");
        Objects.requireNonNull(grantHandlerRegistry, "grantHandlerRegistry must not be null");
        Objects.requireNonNull(configValidatorRegistry, "configValidatorRegistry must not be null");

        if (contributors.isEmpty()) {
            throw new IllegalStateException("at least one award type metadata contributor is required");
        }
        Map<String, AwardTypeMetadata> registeredMetadata = new HashMap<>();
        for (AwardTypeMetadataContributor contributor : contributors) {
            Objects.requireNonNull(contributor, "award type metadata contributor must not be null");
            AwardTypeMetadata metadata = Objects.requireNonNull(contributor.metadata(), "award type metadata must not be null");
            validateGrantCapability(metadata, grantHandlerRegistry);
            validateConfigProtocol(metadata, configValidatorRegistry);
            validateConfigFields(metadata);

            AwardTypeMetadata previous = registeredMetadata.putIfAbsent(metadata.awardType(), metadata);
            if (previous != null) {
                throw new IllegalStateException("duplicate award type metadata: " + metadata.awardType());
            }

        }
        this.metadataByType = Map.copyOf(registeredMetadata);
        List<AwardTypeMetadata> sortedMetadata = new ArrayList<>(registeredMetadata.values());
        sortedMetadata.sort(Comparator.comparing(AwardTypeMetadata::awardType));
        this.orderedMetadata = List.copyOf(sortedMetadata);
    }

    /**
     * 返回系统当前真实支持的全部奖品类型。
     *
     * <p>结果按照 {@code awardType} 稳定排序，便于 API、前端和测试获得
     * 确定性结果。返回集合不可修改。</p>
     *
     * @return 按奖品类型编码排序的不可变元数据列表
     */
    public List<AwardTypeMetadata> listAll() {
        return orderedMetadata;
    }

    /**
     * 按奖品类型查询运营元数据。
     *
     * @param awardType 奖品类型编码
     * @return 找到时返回对应元数据；参数为空或类型不存在时返回空 Optional
     */
    public Optional<AwardTypeMetadata> findByAwardType(String awardType) {
        if (awardType == null || awardType.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(metadataByType.get(awardType));
    }

    /**
     * 验证元数据声明的奖品类型和领取模式具备真实发奖能力。
     *
     * @param metadata             待验证的奖品类型元数据
     * @param grantHandlerRegistry 发奖处理器注册表
     * @throws IllegalStateException 处理器不存在或领取模式不受支持时抛出
     */
    private void validateGrantCapability(AwardTypeMetadata metadata, AwardGrantHandlerRegistry grantHandlerRegistry) {
        if (!grantHandlerRegistry.supports(metadata.awardType())) {
            throw new IllegalStateException("award grant handler is missing for type: " + metadata.awardType());
        }
        AwardGrantHandler handler = grantHandlerRegistry.getRequired(metadata.awardType());
        for (AwardClaimModeMetadata claimMode : metadata.supportedClaimModes()) {
            if (!handler.supportsClaimMode(claimMode.code())) {
                throw new IllegalStateException("unsupported claim mode for award type: "
                        + metadata.awardType() + ":" + claimMode.code());
            }
        }
    }

    /**
     * 验证元数据声明的配置协议存在对应校验器。
     *
     * @param metadata                待验证的奖品类型元数据
     * @param configValidatorRegistry 配置校验器注册表
     * @throws IllegalStateException 配置协议没有对应校验器时抛出
     */
    private void validateConfigProtocol(AwardTypeMetadata metadata, AwardConfigValidatorRegistry configValidatorRegistry) {
        if (!configValidatorRegistry.supports(metadata.awardType(), metadata.configVersion())) {
            throw new IllegalStateException("award config validator is missing for protocol: "
                    + metadata.awardType() + ":" + metadata.configVersion());
        }
    }

    /**
     * 验证同一种奖品配置不存在重复 JSON 字段名称。
     *
     * @param metadata 待验证的奖品类型元数据
     * @throws IllegalStateException 配置字段名称重复时抛出
     */
    private void validateConfigFields(AwardTypeMetadata metadata) {
        Set<String> fieldNames = new HashSet<>();
        for (AwardConfigFieldMetadata field : metadata.configFields()) {
            if (!fieldNames.add(field.name())) {
                throw new IllegalStateException("duplicate config field for award type: "
                        + metadata.awardType() + ":" + field.name());
            }
        }
    }
}
