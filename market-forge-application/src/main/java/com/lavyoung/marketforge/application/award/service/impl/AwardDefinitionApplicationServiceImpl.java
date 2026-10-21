package com.lavyoung.marketforge.application.award.service.impl;

import com.lavyoung.marketforge.application.award.model.AwardDefinitionResult;
import com.lavyoung.marketforge.application.award.model.ChangeAwardDefinitionStateCommand;
import com.lavyoung.marketforge.application.award.model.CreateAwardDefinitionCommand;
import com.lavyoung.marketforge.application.award.model.UpdateAwardDefinitionCommand;
import com.lavyoung.marketforge.application.award.service.IAwardDefinitionApplicationService;
import com.lavyoung.marketforge.domain.award.model.entity.AwardDefinitionEntity;
import com.lavyoung.marketforge.domain.award.model.entity.AwardDefinitionStateChange;
import com.lavyoung.marketforge.domain.award.model.vo.AwardClaimModeVO;
import com.lavyoung.marketforge.domain.award.model.vo.AwardConfigSnapshot;
import com.lavyoung.marketforge.domain.award.model.vo.AwardDefinitionStateVO;
import com.lavyoung.marketforge.domain.award.model.vo.AwardTypeVO;
import com.lavyoung.marketforge.domain.award.repository.IAwardDefinitionRepository;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import com.lavyoung.marketforge.types.utils.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * 奖品定义应用服务默认实现。
 *
 * <p>本类负责编排奖品定义的运营用例，不处理 HTTP 参数、MyBatis 持久化对象
 * 或具体发奖渠道。创建时由服务端生成奖品标识；修改和状态变更同时校验调用方版本，
 * 最终仍由数据库条件更新防止并发覆盖。</p>
 *
 * <p>命令到领域实体的转换只在当前服务中使用，因此保留为有业务含义的私有方法，
 * 暂不增加独立转换器。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/21
 */
@Service
@RequiredArgsConstructor
public class AwardDefinitionApplicationServiceImpl implements IAwardDefinitionApplicationService {

    private static final int AWARD_ID_LENGTH = 18;

    private final IAwardDefinitionRepository awardDefinitionRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AwardDefinitionResult create(CreateAwardDefinitionCommand command) {
        CreateAwardDefinitionCommand validCommand =
                Objects.requireNonNull(command, "command must not be null");

        Long awardId = Long.valueOf(IdGenerator.nextNumericId(AWARD_ID_LENGTH));
        AwardDefinitionEntity definition = createNewDefinition(awardId, validCommand);
        awardDefinitionRepository.create(definition);

        return toResult(queryRequired(awardId));
    }

    @Override
    @Transactional(readOnly = true)
    public AwardDefinitionResult queryByAwardId(Long awardId) {
        return toResult(queryRequired(awardId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AwardDefinitionResult updateDraft(UpdateAwardDefinitionCommand command) {
        UpdateAwardDefinitionCommand validCommand =
                Objects.requireNonNull(command, "command must not be null");

        AwardDefinitionEntity current = queryRequired(validCommand.awardId());
        if (!current.canEdit()) {
            throw BusinessException.of(BusinessResponseCode.AWARD_STATE_ERROR, current.awardId(), current.state().getCode());
        }

        ensureExpectedVersion(current, validCommand.expectedVersion());
        AwardDefinitionEntity updated = createUpdatedDefinition(current, validCommand);

        if (!awardDefinitionRepository.updateDraft(updated)) {
            throw BusinessException.of(BusinessResponseCode.AWARD_DEFINITION_CONFLICT,
                    current.awardId(),
                    validCommand.expectedVersion()
            );
        }

        return toResult(queryRequired(current.awardId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AwardDefinitionResult changeState(ChangeAwardDefinitionStateCommand command) {
        ChangeAwardDefinitionStateCommand validCommand =
                Objects.requireNonNull(command, "command must not be null");

        AwardDefinitionEntity current = queryRequired(validCommand.awardId());
        ensureExpectedVersion(current, validCommand.expectedVersion());

        AwardDefinitionStateVO targetState = parseDefinitionState(validCommand.targetState());
        if (!current.state().canTransitionTo(targetState)) {
            throw BusinessException.of(BusinessResponseCode.AWARD_STATE_ERROR,
                    current.awardId(),
                    current.state().getCode(),
                    targetState.getCode()
            );
        }

        AwardDefinitionStateChange stateChange = new AwardDefinitionStateChange(
                current.awardId(),
                current.state(),
                targetState,
                validCommand.expectedVersion(),
                validCommand.operator()
        );

        if (!awardDefinitionRepository.changeState(stateChange)) {
            throw BusinessException.of(BusinessResponseCode.AWARD_DEFINITION_CONFLICT,
                    current.awardId(), validCommand.expectedVersion());
        }

        return toResult(queryRequired(current.awardId()));
    }

    /**
     * 创建初始草稿奖品定义。
     *
     * @param awardId 服务端生成的奖品标识
     * @param command 创建命令
     * @return 初始版本为 1 的草稿奖品定义
     * @throws BusinessException 配置或公共字段不合法时抛出
     */
    private AwardDefinitionEntity createNewDefinition(
            Long awardId,
            CreateAwardDefinitionCommand command
    ) {
        try {
            return new AwardDefinitionEntity(
                    awardId,
                    command.awardCode(),
                    command.awardName(),
                    parseAwardType(command.awardType()),
                    parseClaimMode(command.claimMode()),
                    command.providerCode(),
                    createConfigSnapshot(command.awardConfig(), command.configVersion()),
                    command.awardDesc(),
                    AwardDefinitionStateVO.DRAFT,
                    1,
                    command.operator(),
                    command.operator(),
                    null,
                    null
            );
        } catch (NullPointerException | IllegalArgumentException exception) {
            throw BusinessException.of(BusinessResponseCode.AWARD_CONFIG_INVALID, exception, command.awardCode());
        }
    }

    /**
     * 使用修改命令创建新的不可变草稿实体。
     *
     * @param current 当前数据库奖品定义
     * @param command 修改命令
     * @return 携带调用方预期版本的最新草稿定义
     * @throws BusinessException 配置或公共字段不合法时抛出
     */
    private AwardDefinitionEntity createUpdatedDefinition(AwardDefinitionEntity current,
                                                          UpdateAwardDefinitionCommand command) {
        try {
            return new AwardDefinitionEntity(
                    current.awardId(),
                    command.awardCode(),
                    command.awardName(),
                    parseAwardType(command.awardType()),
                    parseClaimMode(command.claimMode()),
                    command.providerCode(),
                    createConfigSnapshot(command.awardConfig(), command.configVersion()),
                    command.awardDesc(),
                    current.state(),
                    command.expectedVersion(),
                    current.createdBy(),
                    command.operator(),
                    current.createTime(),
                    current.updateTime()
            );
        } catch (NullPointerException | IllegalArgumentException exception) {
            throw BusinessException.of(BusinessResponseCode.AWARD_CONFIG_INVALID, exception, current.awardId());
        }
    }

    /**
     * 查询必须存在的奖品定义。
     *
     * @param awardId 奖品业务标识
     * @return 对应奖品定义
     * @throws NullPointerException 奖品标识为空时抛出
     * @throws BusinessException    奖品不存在时抛出
     */
    private AwardDefinitionEntity queryRequired(Long awardId) {
        Long validAwardId = Objects.requireNonNull(awardId, "awardId must not be null");
        return awardDefinitionRepository.queryByAwardId(validAwardId)
                .orElseThrow(() -> BusinessException.of(BusinessResponseCode.AWARD_NOT_FOUND, validAwardId));
    }

    /**
     * 校验调用方持有的乐观锁版本。
     *
     * <p>应用层检查用于尽早返回明确错误，数据库条件更新仍是最终并发防线。</p>
     *
     * @param current         当前数据库奖品定义
     * @param expectedVersion 调用方持有的版本
     * @throws BusinessException 版本不一致时抛出
     */
    private void ensureExpectedVersion(
            AwardDefinitionEntity current,
            int expectedVersion
    ) {
        if (current.version() != expectedVersion) {
            throw BusinessException.of(BusinessResponseCode.AWARD_DEFINITION_CONFLICT, current.awardId(), expectedVersion, current.version());
        }
    }

    /**
     * 解析开放式奖品类型。
     *
     * @param code 奖品类型编码
     * @return 奖品类型值对象
     * @throws BusinessException 编码为空或格式非法时抛出
     */
    private AwardTypeVO parseAwardType(String code) {
        try {
            return new AwardTypeVO(code);
        } catch (NullPointerException | IllegalArgumentException exception) {
            throw BusinessException.of(BusinessResponseCode.AWARD_TYPE_NOT_SUPPORTED, exception, code);
        }
    }

    /**
     * 解析领取模式。
     *
     * @param code 领取模式编码
     * @return 领取模式
     * @throws BusinessException 编码无法识别时抛出
     */
    private AwardClaimModeVO parseClaimMode(String code) {
        return AwardClaimModeVO.of(code)
                .orElseThrow(() -> BusinessException.of(BusinessResponseCode.AWARD_CONFIG_INVALID, code));
    }

    /**
     * 解析目标生命周期状态。
     *
     * @param code 目标状态编码
     * @return 生命周期状态
     * @throws BusinessException 编码无法识别时抛出
     */
    private AwardDefinitionStateVO parseDefinitionState(String code) {
        return AwardDefinitionStateVO.of(code)
                .orElseThrow(() -> BusinessException.of(BusinessResponseCode.AWARD_STATE_ERROR, code));
    }

    /**
     * 创建发奖配置快照。
     *
     * @param content 配置内容
     * @param version 配置结构版本
     * @return 发奖配置快照
     * @throws BusinessException 配置为空或版本非法时抛出
     */
    private AwardConfigSnapshot createConfigSnapshot(String content, int version) {
        try {
            return new AwardConfigSnapshot(content, version);
        } catch (NullPointerException | IllegalArgumentException exception) {
            throw BusinessException.of(BusinessResponseCode.AWARD_CONFIG_INVALID, exception, version);
        }
    }

    /**
     * 将领域实体转换为应用结果。
     *
     * @param entity 奖品定义领域实体
     * @return 不暴露领域值对象的应用结果
     */
    private AwardDefinitionResult toResult(AwardDefinitionEntity entity) {
        return new AwardDefinitionResult(
                entity.awardId(),
                entity.awardCode(),
                entity.awardName(),
                entity.awardType().code(),
                entity.claimMode().getCode(),
                entity.providerCode(),
                entity.awardConfig().content(),
                entity.awardConfig().version(),
                entity.awardDesc(),
                entity.state().getCode(),
                entity.version(),
                entity.createdBy(),
                entity.updatedBy(),
                entity.createTime(),
                entity.updateTime()
        );
    }
}
