package com.lavyoung.marketforge.infrastructure.persistent.repository;

import com.lavyoung.marketforge.domain.strategy.model.aggregate.StrategyDefinitionAggregate;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyAwardEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyDefinitionStateChange;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyRuleEntity;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyDefinitionRepository;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.StrategyAwardAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.StrategyDefinitionAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.StrategyRuleAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.dao.IStrategyAwardDao;
import com.lavyoung.marketforge.infrastructure.persistent.dao.IStrategyDao;
import com.lavyoung.marketforge.infrastructure.persistent.dao.IStrategyRuleDao;
import com.lavyoung.marketforge.infrastructure.persistent.model.StrategyDefinitionStateUpdateParam;
import com.lavyoung.marketforge.infrastructure.persistent.po.StrategyAwardPO;
import com.lavyoung.marketforge.infrastructure.persistent.po.StrategyPO;
import com.lavyoung.marketforge.infrastructure.persistent.po.StrategyRulePO;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 策略定义运营仓储实现。
 *
 * <p>本仓储负责在策略定义聚合与三张 MyBatis 持久化表之间建立防腐边界。
 * 现有 {@link StrategyRepository} 继续服务抽奖运行时、概率快照和库存访问，
 * 两者不得互相替代。</p>
 *
 * <p>完整聚合读取使用只读事务，保证根配置、奖品和规则来自同一个数据库快照。
 * 创建和草稿更新使用写事务，任何子配置写入失败时都回滚策略根记录。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
@Repository
@RequiredArgsConstructor
public class StrategyDefinitionRepository implements IStrategyDefinitionRepository {

    private final IStrategyDao strategyDao;
    private final IStrategyAwardDao strategyAwardDao;
    private final IStrategyRuleDao strategyRuleDao;

    private final StrategyDefinitionAssembler strategyDefinitionAssembler;
    private final StrategyAwardAssembler strategyAwardAssembler;
    private final StrategyRuleAssembler strategyRuleAssembler;

    @Override
    @Transactional(readOnly = true)
    public Optional<StrategyDefinitionAggregate> queryByStrategyId(Long strategyId) {
        Objects.requireNonNull(strategyId, "strategyId must not be null");
        Optional<StrategyPO> strategyPO = strategyDao.queryStrategyByStrategyId(strategyId);
        if (strategyPO.isEmpty()) {
            return Optional.empty();
        }
        List<StrategyAwardEntity> awards = strategyAwardAssembler.toEntities(
                strategyAwardDao.queryStrategyAwardList(strategyId));
        List<StrategyRuleEntity> rules = strategyRuleAssembler.toEntities(
                strategyRuleDao.queryStrategyRuleList(strategyId));
        return Optional.of(strategyDefinitionAssembler.toAggregate(strategyPO.get(), awards, rules));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(StrategyDefinitionAggregate aggregate) {
        Objects.requireNonNull(aggregate, "aggregate must not be null");
        try {
            StrategyPO strategy = strategyDefinitionAssembler.toPO(aggregate);
            if (strategyDao.insert(strategy) != 1) {
                throw BusinessException.of(BusinessResponseCode.STRATEGY_DEFINITION_SAVE_FAILED, aggregate.strategyId());
            }
            saveChildren(aggregate);
        } catch (DuplicateKeyException exception) {
            throw BusinessException.of(BusinessResponseCode.STRATEGY_DEFINITION_DUPLICATED,
                    exception, aggregate.strategyId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDraft(StrategyDefinitionAggregate aggregate) {
        Objects.requireNonNull(aggregate, "aggregate must not be null");
        try {
            StrategyPO strategy = strategyDefinitionAssembler.toPO(aggregate);
            if (strategyDao.updateDraft(strategy) != 1) {
                return false;
            }
            strategyAwardDao.deleteByStrategyId(aggregate.strategyId());
            strategyRuleDao.deleteByStrategyId(aggregate.strategyId());
            saveChildren(aggregate);
            return true;
        } catch (DuplicateKeyException exception) {
            throw BusinessException.of(BusinessResponseCode.STRATEGY_DEFINITION_DUPLICATED,
                    exception, aggregate.strategyId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean changeState(StrategyDefinitionStateChange stateChange) {
        Objects.requireNonNull(stateChange, "stateChange must not be null");
        StrategyDefinitionStateUpdateParam stateUpdateParam = new StrategyDefinitionStateUpdateParam(
                stateChange.strategyId(),
                stateChange.currentState().getCode(),
                stateChange.targetState().getCode(),
                stateChange.expectedVersion(),
                stateChange.operator()
        );
        return strategyDao.changeState(stateUpdateParam) == 1;
    }

    /**
     * 批量写入策略聚合的奖品和规则。
     *
     * <p>空集合不调用批量 Mapper，避免生成缺少 {@code VALUES} 内容的非法 SQL。
     * DAO 返回的影响行数必须与集合大小一致，否则抛出业务异常并回滚整个事务。</p>
     *
     * @param aggregate 待保存的策略定义聚合
     * @throws BusinessException 任意批量写入记录数不完整时抛出
     */
    private void saveChildren(StrategyDefinitionAggregate aggregate) {
        List<StrategyAwardPO> awardPOS = strategyAwardAssembler.toPOs(aggregate.awards());
        if (!awardPOS.isEmpty() && strategyAwardDao.insertBatch(awardPOS) != awardPOS.size()) {
            throw BusinessException.of(BusinessResponseCode.STRATEGY_DEFINITION_SAVE_FAILED,
                    aggregate.strategyId(), "strategy_award");
        }

        List<StrategyRulePO> rules = strategyRuleAssembler.toPOs(aggregate.rules());
        if (!rules.isEmpty() && strategyRuleDao.insertBatch(rules) != rules.size()) {
            throw BusinessException.of(BusinessResponseCode.STRATEGY_DEFINITION_SAVE_FAILED,
                    aggregate.strategyId(), "strategy_rule");
        }

    }
}
