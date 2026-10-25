package com.lavyoung.marketforge.infrastructure.persistent.assembler;

import com.lavyoung.marketforge.domain.strategy.model.aggregate.StrategyDefinitionAggregate;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyAwardEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyRuleEntity;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyDefinitionStateVO;
import com.lavyoung.marketforge.infrastructure.persistent.po.StrategyPO;
import org.mapstruct.*;

import java.util.List;

/**
 * 策略定义聚合与策略根持久化对象转换器。
 *
 * <p>策略根字段由 MapStruct 负责映射。策略奖品和策略规则需要分别批量查询，
 * 因此由 {@link #toAggregate(StrategyPO, List, List)} 将三类查询结果组装为完整聚合。</p>
 *
 * <p>数据库状态编码在此处转换为领域枚举。若数据库出现未知状态，
 * 转换立即失败，禁止损坏的配置继续进入发布流程。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface StrategyDefinitionAssembler {

    /**
     * 将策略定义聚合转换为策略根持久化对象。
     *
     * <p>数据库自增主键由数据库生成，因此忽略 {@code id}。聚合中的奖品和规则
     * 由各自的 Assembler 转换，不进入策略根表。</p>
     *
     * @param source 策略定义聚合
     * @return 策略根持久化对象
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "state", source = "state", qualifiedByName = "fromDefinitionState")
    StrategyPO toPO(StrategyDefinitionAggregate source);

    /**
     * 将三张表的读取结果组装为完整策略定义聚合。
     *
     * @param strategy 策略根持久化对象
     * @param awards   当前策略的全部奖品
     * @param rules    当前策略的全部规则
     * @return 完整策略定义聚合；根记录为空时返回 {@code null}
     * @throws IllegalArgumentException 数据库状态未知或子配置不属于当前策略时抛出
     */
    default StrategyDefinitionAggregate toAggregate(StrategyPO strategy, List<StrategyAwardEntity> awards,
                                                    List<StrategyRuleEntity> rules) {
        if (strategy == null) {
            return null;
        }

        return new StrategyDefinitionAggregate(
                strategy.getStrategyId(),
                strategy.getStrategyDesc(),
                strategy.getRuleModels(),
                toDefinitionState(strategy.getState()),
                strategy.getVersion(),
                strategy.getCreatedBy(),
                strategy.getUpdatedBy(),
                strategy.getCreateTime(),
                strategy.getUpdateTime(),
                awards,
                rules
        );
    }

    /**
     * 将数据库生命周期编码转换为领域状态。
     *
     * @param code 数据库状态编码
     * @return 策略定义生命周期状态
     * @throws IllegalArgumentException 状态编码无法识别时抛出
     */
    @Named("toDefinitionState")
    default StrategyDefinitionStateVO toDefinitionState(String code) {
        return StrategyDefinitionStateVO.of(code)
                .orElseThrow(() -> new IllegalArgumentException("unknown strategy definition state: " + code));
    }

    /**
     * 提取策略生命周期状态的持久化编码。
     *
     * @param state 策略定义生命周期状态
     * @return 数据库存储编码
     */
    @Named("fromDefinitionState")
    default String fromDefinitionState(StrategyDefinitionStateVO state) {
        return state.getCode();
    }
}
