package com.lavyoung.marketforge.infrastructure.persistent.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lavyoung.marketforge.infrastructure.persistent.po.StrategyRulePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 抽奖策略规则数据访问接口。
 * <p>
 * 在 MyBatis-Plus 通用 CRUD 能力之外，提供策略规则列表及复合条件查询方法。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0-SNAPSHOT
 */
@Mapper
public interface IStrategyRuleDao extends BaseMapper<StrategyRulePO> {

    /**
     * 查询指定策略配置的全部规则。
     *
     * @param strategyId 策略标识
     * @return 策略规则列表；不存在时返回空列表
     */
    List<StrategyRulePO> queryStrategyRuleList(@Param("strategyId") Long strategyId);

    /**
     * 根据策略、奖品和规则模型查询规则。
     *
     * @param strategyId 策略标识
     * @param awardId    奖品标识；查询策略级规则时为 {@code null}
     * @param ruleModel  规则模型标识
     * @return 策略规则持久化对象；不存在时返回 {@link Optional#empty()}
     */
    Optional<StrategyRulePO> queryStrategyRule(
            @Param("strategyId") Long strategyId,
            @Param("awardId") Long awardId,
            @Param("ruleModel") String ruleModel);

}
