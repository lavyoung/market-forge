package com.lavyoung.marketforge.domain.strategy.repository;

import com.lavyoung.marketforge.domain.strategy.model.vo.RuleTreeVO;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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
     * @return 已组装的规则树视图；没有匹配配置时返回空
     */
    Optional<RuleTreeVO> queryRuleTreeVOByTreeId(List<RuleModel> ruleModels);

    /**
     * 批量查询奖品次数锁规则配置。
     * <p>
     * 入参是奖品配置中的规则树标识，例如 {@code tree_lock_1}、{@code tree_lock_2}。
     * 方法会查询每棵规则树中 {@code rule_lock} 节点的配置值，
     * 并转换为“规则树标识 -> 解锁所需抽奖次数”的映射。
     * <p>
     * 返回示例：
     * <pre>
     * tree_lock_1 -> 1
     * tree_lock_2 -> 2
     * </pre>
     *
     * @param treeIds 规则树标识列表
     * @return 规则树标识到解锁次数的映射；没有配置次数锁时返回空 Map
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 规则值不是非负整数时抛出
     */
    Map<String, Integer> queryAwardRuleLockCount(List<String> treeIds);
}
