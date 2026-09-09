package com.lavyoung.marketforge.infrastructure.persistent.repository;

import com.lavyoung.marketforge.domain.strategy.model.vo.*;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 验证 {@link RuleTreeRepository} 对规则树主配置、节点和连线的查询组装行为。
 */
@ExtendWith(MockitoExtension.class)
class RuleTreeRepositoryTest {

    private static final String TREE_ID = "900904001";

    @Mock
    private IRuleTreeDao treeDao;
    @Mock
    private IRuleTreeNodeDao nodeDao;
    @Mock
    private IRuleTreeNodeLineDao lineDao;
    @Mock
    private RuleTreeAssembler treeAssembler;
    @Mock
    private RuleTreeNodeAssembler nodeAssembler;
    @Mock
    private RuleTreeNodeLineAssembler lineAssembler;

    private RuleTreeRepository repository;

    /**
     * Given 规则树依赖，When 初始化仓储，Then 使用隔离依赖测试组装逻辑。
     */
    @BeforeEach
    void setUp() {
        repository = new RuleTreeRepository(
                treeDao, nodeDao, lineDao, treeAssembler, nodeAssembler, lineAssembler);
    }

    /**
     * Given 没有执行阶段规则模型，When 查询规则树，Then 返回空且不访问数据库。
     */
    @Test
    void shouldReturnEmptyWhenRuleModelsAreEmpty() {
        // When
        Optional<RuleTreeVO> result = repository.queryRuleTreeVOByTreeId(List.of());

        // Then
        assertTrue(result.isEmpty());
        verifyNoInteractions(treeDao, nodeDao, lineDao, treeAssembler, nodeAssembler, lineAssembler);
    }

    /**
     * Given 主配置包含两个节点及一条连线，When 查询规则树，Then 连线只挂载到来源节点。
     */
    @Test
    void shouldAssembleNodesAndGroupLinesBySource() {
        // Given
        RuleTreePO treePO = treePO();
        RuleTreeNodePO lockPO = nodePO(RuleModel.LOCK.getCode());
        RuleTreeNodePO stockPO = nodePO(RuleModel.RULE_STOCK.getCode());
        RuleTreeNodeLinePO linePO = new RuleTreeNodeLinePO();
        RuleTreeNodeLineVo lineVO = new RuleTreeNodeLineVo(
                Integer.valueOf(TREE_ID), RuleModel.LOCK.getCode(), RuleModel.RULE_STOCK.getCode(),
                RuleLimitTypeVO.EQ, RuleLogicCheckTypeVO.ALLOW);
        when(treeDao.queryByRootRuleKey(RuleModel.LOCK.getCode())).thenReturn(Optional.of(treePO));
        when(nodeDao.queryByTreeId(TREE_ID)).thenReturn(List.of(lockPO, stockPO));
        when(lineDao.queryByTreeId(TREE_ID)).thenReturn(List.of(linePO));
        when(treeAssembler.toVO(treePO)).thenReturn(new RuleTreeVO(
                Integer.valueOf(TREE_ID), "测试规则树", "测试规则树", RuleModel.LOCK.getCode(), Map.of()));
        when(nodeAssembler.toVO(lockPO)).thenReturn(nodeVO(RuleModel.LOCK.getCode()));
        when(nodeAssembler.toVO(stockPO)).thenReturn(nodeVO(RuleModel.RULE_STOCK.getCode()));
        when(lineAssembler.toVO(linePO)).thenReturn(lineVO);

        // When
        RuleTreeVO result = repository.queryRuleTreeVOByTreeId(List.of(RuleModel.LOCK)).orElseThrow();

        // Then
        assertAll(
                () -> assertEquals(RuleModel.LOCK.getCode(), result.treeRootRule()),
                () -> assertEquals(List.of(lineVO), result.treeNodeVOMap().get(RuleModel.LOCK.getCode())
                        .ruleTreeNodeLineVoList()),
                () -> assertTrue(result.treeNodeVOMap().get(RuleModel.RULE_STOCK.getCode())
                        .ruleTreeNodeLineVoList().isEmpty())
        );
    }

    /**
     * 创建规则树主表记录。
     *
     * @return 固定规则树记录
     */
    private RuleTreePO treePO() {
        RuleTreePO treePO = new RuleTreePO();
        treePO.setTreeId(TREE_ID);
        treePO.setTreeNodeRuleKey(RuleModel.LOCK.getCode());
        return treePO;
    }

    /**
     * 创建指定规则标识的节点记录。
     *
     * @param ruleKey 规则标识
     * @return 节点记录
     */
    private RuleTreeNodePO nodePO(String ruleKey) {
        RuleTreeNodePO nodePO = new RuleTreeNodePO();
        nodePO.setTreeId(TREE_ID);
        nodePO.setRuleKey(ruleKey);
        return nodePO;
    }

    /**
     * 创建尚未挂载连线的节点值对象。
     *
     * @param ruleKey 规则标识
     * @return 节点值对象
     */
    private RuleTreeNodeVO nodeVO(String ruleKey) {
        return new RuleTreeNodeVO(Integer.valueOf(TREE_ID), ruleKey, ruleKey, "", List.of());
    }
}
