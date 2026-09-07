package com.lavyoung.marketforge.domain.strategy.repository;

import com.lavyoung.marketforge.domain.strategy.model.vo.RuleTreeVO;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;

import java.util.List;

/**
 * 规则树仓储端口。
 * <p>
 * 根据策略关联的规则模型加载并组装领域层可执行的规则树视图。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/07
 */
public interface IRuleTreeRepository {

    /**
     * 查询与给定规则模型匹配的规则树。
     *
     * @param ruleModels 策略在抽奖执行阶段配置的规则模型列表
     * @return 已组装的规则树视图；没有匹配配置时返回 {@code null}
     */
    RuleTreeVO queryRuleTreeVOByTreeId(List<RuleModel> ruleModels);
}
