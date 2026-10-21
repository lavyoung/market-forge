package com.lavyoung.marketforge.infrastructure.persistent.assembler;

import com.lavyoung.marketforge.domain.award.model.entity.AwardDefinitionEntity;
import com.lavyoung.marketforge.domain.award.model.vo.AwardClaimModeVO;
import com.lavyoung.marketforge.domain.award.model.vo.AwardConfigSnapshot;
import com.lavyoung.marketforge.domain.award.model.vo.AwardDefinitionStateVO;
import com.lavyoung.marketforge.domain.award.model.vo.AwardTypeVO;
import com.lavyoung.marketforge.infrastructure.persistent.po.AwardPO;
import org.mapstruct.*;

/**
 * 奖品定义领域实体与持久化对象转换器。
 *
 * <p>负责隔离领域模型和 MyBatis 持久化模型，并完成数据库字符串编码与
 * 奖品类型、领取模式、生命周期状态和配置快照之间的转换。</p>
 *
 * <p>数据库出现未知领取模式或未知状态时，转换过程立即失败，
 * 防止损坏的配置继续进入活动发布或奖品发放流程。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/21
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AwardDefinitionAssembler {

    /**
     * 将奖品持久化对象转换为完整奖品定义。
     *
     * @param source 数据库奖品记录
     * @return 完整奖品定义；传入 {@code null} 时返回 {@code null}
     * @throws IllegalArgumentException 数据库中的类型、领取模式或状态编码不合法时抛出
     */
    @Mapping(target = "awardCode", source = "awardKey")
    @Mapping(target = "awardType", source = "awardType", qualifiedByName = "toAwardType")
    @Mapping(target = "claimMode", source = "claimMode", qualifiedByName = "toClaimMode")
    @Mapping(target = "awardConfig", expression = "java(toAwardConfig(source))")
    @Mapping(target = "state", source = "state", qualifiedByName = "toDefinitionState")
    AwardDefinitionEntity toEntity(AwardPO source);

    /**
     * 将奖品定义转换为数据库持久化对象。
     *
     * <p>数据库主键由数据库生成，因此转换时忽略 {@code id}。
     * 奖品配置内容和配置版本分别写入两个字段。</p>
     *
     * @param source 完整奖品定义
     * @return 可供 MyBatis 新增或更新的持久化对象
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "awardKey", source = "awardCode")
    @Mapping(target = "awardType", source = "awardType", qualifiedByName = "fromAwardType")
    @Mapping(target = "claimMode", source = "claimMode", qualifiedByName = "fromClaimMode")
    @Mapping(target = "awardConfig", source = "awardConfig.content")
    @Mapping(target = "configVersion", source = "awardConfig.version")
    @Mapping(target = "state", source = "state", qualifiedByName = "fromDefinitionState")
    AwardPO toPO(AwardDefinitionEntity source);

    /**
     * 将数据库奖品类型编码转换为开放式奖品类型值对象。
     *
     * @param code 数据库奖品类型编码
     * @return 奖品类型值对象
     * @throws NullPointerException     编码为空时抛出
     * @throws IllegalArgumentException 编码格式不合法时抛出
     */
    @Named("toAwardType")
    default AwardTypeVO toAwardType(String code) {
        return new AwardTypeVO(code);
    }

    /**
     * 提取奖品类型持久化编码。
     *
     * @param awardType 奖品类型值对象
     * @return 数据库存储编码
     */
    @Named("fromAwardType")
    default String fromAwardType(AwardTypeVO awardType) {
        return awardType.code();
    }

    /**
     * 将数据库领取模式编码转换为领域枚举。
     *
     * @param code 数据库领取模式编码
     * @return 领取模式
     * @throws IllegalArgumentException 编码无法识别时抛出
     */
    @Named("toClaimMode")
    default AwardClaimModeVO toClaimMode(String code) {
        return AwardClaimModeVO.of(code)
                .orElseThrow(() -> new IllegalArgumentException("unknown award claim mode: " + code));
    }

    /**
     * 提取领取模式持久化编码。
     *
     * @param claimMode 领取模式
     * @return 数据库存储编码
     */
    @Named("fromClaimMode")
    default String fromClaimMode(AwardClaimModeVO claimMode) {
        return claimMode.getCode();
    }

    /**
     * 将数据库生命周期状态转换为领域枚举。
     *
     * @param code 数据库生命周期状态编码
     * @return 奖品定义状态
     * @throws IllegalArgumentException 编码无法识别时抛出
     */
    @Named("toDefinitionState")
    default AwardDefinitionStateVO toDefinitionState(String code) {
        return AwardDefinitionStateVO.of(code)
                .orElseThrow(() -> new IllegalArgumentException("unknown award definition state: " + code));
    }

    /**
     * 提取奖品定义状态持久化编码。
     *
     * @param state 奖品定义状态
     * @return 数据库存储编码
     */
    @Named("fromDefinitionState")
    default String fromDefinitionState(AwardDefinitionStateVO state) {
        return state.getCode();
    }

    /**
     * 将分散存储的配置内容和版本组装成不可变快照。
     *
     * @param source 数据库奖品记录
     * @return 奖品发放配置快照
     * @throws NullPointerException     配置内容或版本为空时抛出
     * @throws IllegalArgumentException 配置内容为空白或版本非法时抛出
     */
    default AwardConfigSnapshot toAwardConfig(AwardPO source) {
        if (source == null) {
            return null;
        }
        return new AwardConfigSnapshot(source.getAwardConfig(), source.getConfigVersion());
    }
}