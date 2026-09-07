package com.lavyoung.marketforge.infrastructure.persistent.repository;

import com.lavyoung.marketforge.domain.strategy.model.vo.RuleTreeVO;
import com.lavyoung.marketforge.domain.strategy.repository.IRuleTreeRepository;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

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
     * {@inheritDoc}
     */
    @Override
    public RuleTreeVO queryRuleTreeVOByTreeId(List<RuleModel> ruleModels) {
        return null;
    }
}
