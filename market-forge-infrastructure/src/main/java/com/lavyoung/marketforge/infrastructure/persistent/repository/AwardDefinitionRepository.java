package com.lavyoung.marketforge.infrastructure.persistent.repository;

import com.lavyoung.marketforge.domain.award.model.entity.AwardDefinitionEntity;
import com.lavyoung.marketforge.domain.award.model.entity.AwardDefinitionStateChange;
import com.lavyoung.marketforge.domain.award.repository.IAwardDefinitionRepository;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.AwardDefinitionAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.dao.IAwardDao;
import com.lavyoung.marketforge.infrastructure.persistent.model.AwardDefinitionStateUpdateParam;
import com.lavyoung.marketforge.infrastructure.persistent.po.AwardPO;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.Optional;

/**
 * 奖品定义仓储实现。
 *
 * <p>负责在奖品定义领域模型和 MyBatis 持久化模型之间建立边界，
 * 并完成奖品查询、创建、草稿条件更新和生命周期状态条件更新。</p>
 *
 * <p>数据库唯一键异常在该边界转换为稳定的业务异常；
 * 草稿状态或乐观锁版本不匹配时返回 {@code false}，由应用层决定如何响应运营请求。</p>
 *
 * <p>该仓储只管理奖品基础定义，不负责用户中奖记录、发奖任务和第三方履约调用。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/21
 */
@Repository
@RequiredArgsConstructor
public class AwardDefinitionRepository implements IAwardDefinitionRepository {

    private final IAwardDao awardDao;
    private final AwardDefinitionAssembler awardDefinitionAssembler;

    @Override
    public Optional<AwardDefinitionEntity> queryByAwardId(Long awardId) {
        Objects.requireNonNull(awardId, "awardId must not be null");
        return awardDao.queryAwardByAwardId(awardId).map(awardDefinitionAssembler::toEntity);
    }

    @Override
    public Optional<AwardDefinitionEntity> queryByAwardCode(String awardCode) {
        Objects.requireNonNull(awardCode, "awardCode must not be null");
        return awardDao.queryAwardByAwardKey(awardCode).map(awardDefinitionAssembler::toEntity);
    }

    @Override
    public void create(AwardDefinitionEntity definition) {
        Objects.requireNonNull(definition, "definition must not be null");
        AwardPO awardPO = awardDefinitionAssembler.toPO(definition);

        try {
            int affectedRows = awardDao.insert(awardPO);
            if (affectedRows != 1) {
                throw BusinessException.of(BusinessResponseCode.AWARD_DEFINITION_CREATE_FAILED,
                        definition.awardId(), definition.awardCode());
            }
        } catch (DuplicateKeyException exception) {
            throw BusinessException.of(BusinessResponseCode.AWARD_DEFINITION_DUPLICATED,
                    exception, definition.awardId(), definition.awardCode());
        }
    }

    @Override
    public boolean updateDraft(AwardDefinitionEntity definition) {
        Objects.requireNonNull(definition, "definition must not be null");
        AwardPO awardPO = awardDefinitionAssembler.toPO(definition);

        try {
            return awardDao.updateDraftDefinition(awardPO) == 1;
        } catch (DuplicateKeyException exception) {
            throw BusinessException.of(BusinessResponseCode.AWARD_DEFINITION_DUPLICATED,
                    exception, definition.awardId(), definition.awardCode());
        }
    }

    @Override
    public boolean changeState(AwardDefinitionStateChange stateChange) {
        Objects.requireNonNull(stateChange, "stateChange must not be null");
        AwardDefinitionStateUpdateParam stateUpdate = new AwardDefinitionStateUpdateParam(
                stateChange.awardId(),
                stateChange.currentState().getCode(),
                stateChange.targetState().getCode(),
                stateChange.expectedVersion(),
                stateChange.operator()
        );
        return awardDao.changeDefinitionState(stateUpdate) == 1;
    }
}
