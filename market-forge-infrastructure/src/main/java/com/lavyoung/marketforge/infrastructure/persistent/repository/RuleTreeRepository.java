package com.lavyoung.marketforge.infrastructure.persistent.repository;

import com.lavyoung.marketforge.domain.strategy.model.vo.RuleTreeNodeLineVo;
import com.lavyoung.marketforge.domain.strategy.model.vo.RuleTreeNodeVO;
import com.lavyoung.marketforge.domain.strategy.model.vo.RuleTreeVO;
import com.lavyoung.marketforge.domain.strategy.repository.IRuleTreeRepository;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.RuleTreeAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.RuleTreeNodeAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.RuleTreeNodeLineAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.dao.IRuleTreeDao;
import com.lavyoung.marketforge.infrastructure.persistent.dao.IRuleTreeNodeDao;
import com.lavyoung.marketforge.infrastructure.persistent.dao.IRuleTreeNodeLineDao;
import com.lavyoung.marketforge.infrastructure.persistent.po.RuleTreeNodeLinePO;
import com.lavyoung.marketforge.infrastructure.persistent.po.RuleTreeNodePO;
import com.lavyoung.marketforge.infrastructure.persistent.po.RuleTreePO;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 规则树仓储实现。
 * <p>
 * 负责从持久化层加载规则树、节点与连线配置，并组装为领域层规则树视图。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuleTreeRepository implements IRuleTreeRepository {

    /**
     * 规则树主表数据访问对象。
     */
    private final IRuleTreeDao treeDao;

    /**
     * 规则树节点数据访问对象。
     */
    private final IRuleTreeNodeDao ruleTreeNodeDao;

    /**
     * 规则树节点连线数据访问对象。
     */
    private final IRuleTreeNodeLineDao ruleTreeNodeLineDao;

    /**
     * 规则树主表转换器。
     */
    private final RuleTreeAssembler ruleTreeAssembler;

    /**
     * 规则树节点转换器。
     */
    private final RuleTreeNodeAssembler ruleTreeNodeAssembler;

    /**
     * 规则树节点连线转换器。
     */
    private final RuleTreeNodeLineAssembler ruleTreeNodeLineAssembler;

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<RuleTreeVO> queryRuleTreeVOByTreeId(List<RuleModel> ruleModels) {
        if (ruleModels == null || ruleModels.isEmpty()) {
            return Optional.empty();
        }
        Optional<RuleModel> rootRule = ruleModels.stream()
                .filter(Objects::nonNull)
                .findFirst();
        return rootRule.flatMap(ruleModel -> treeDao.queryByRootRuleKey(ruleModel.getCode()))
                .map(this::assembleRuleTree);
    }

    /**
     * 装配规则树主配置、节点及连线。
     *
     * @param treePO 规则树主配置
     * @return 可供领域引擎执行的完整规则树
     * @throws IllegalStateException 节点标识重复时抛出
     */
    private RuleTreeVO assembleRuleTree(RuleTreePO treePO) {
        RuleTreeVO tree = ruleTreeAssembler.toVO(treePO);
        List<RuleTreeNodePO> nodePOList = ruleTreeNodeDao.queryByTreeId(treePO.getTreeId());
        List<RuleTreeNodeLinePO> linePOList = ruleTreeNodeLineDao.queryByTreeId(treePO.getTreeId());

        Map<String, List<RuleTreeNodeLineVo>> linesBySource = linePOList.stream()
                .map(ruleTreeNodeLineAssembler::toVO)
                .collect(Collectors.groupingBy(RuleTreeNodeLineVo::ruleNodeFrom));
        Map<String, RuleTreeNodeVO> nodesByRuleKey = nodePOList.stream()
                .map(node -> assembleNode(node, linesBySource))
                .collect(Collectors.toUnmodifiableMap(RuleTreeNodeVO::ruleKey, node -> node));

        return new RuleTreeVO(
                tree.treeId(),
                tree.treeName(),
                tree.treeDesc(),
                tree.treeRootRule(),
                nodesByRuleKey
        );
    }

    /**
     * 为单个规则节点附加由该节点发出的连线。
     *
     * @param nodePO        节点持久化对象
     * @param linesBySource 按来源节点分组的连线
     * @return 已装配连线的规则节点
     */
    private RuleTreeNodeVO assembleNode(
            RuleTreeNodePO nodePO,
            Map<String, List<RuleTreeNodeLineVo>> linesBySource) {
        RuleTreeNodeVO node = ruleTreeNodeAssembler.toVO(nodePO);
        return new RuleTreeNodeVO(
                node.treeId(),
                node.ruleKey(),
                node.ruleDesc(),
                node.ruleValue(),
                List.copyOf(linesBySource.getOrDefault(node.ruleKey(), List.of()))
        );
    }
}
