package com.lavyoung.marketforge.application.activity.service.impl;

import com.lavyoung.marketforge.application.tx.ITransactionExecutor;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityStageEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityStageStateChange;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivityStageStateVO;
import com.lavyoung.marketforge.domain.activity.repository.IActivityStageRepository;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 活动展台发布事务。
 *
 * <p>在同一本地事务内锁定指定渠道和来源的全部展台记录，先失效原生效配置，再激活目标草稿。
 * 任何条件更新失败都会抛出异常并整体回滚，避免同一个 SC 暴露多个活动或没有生效活动。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
@Component
@RequiredArgsConstructor
public class ActivityStagePublicationTransaction {

    private final IActivityStageRepository activityStageRepository;
    private final ITransactionExecutor transactionExecutor;

    /**
     * 原子切换指定渠道和来源的生效活动。
     *
     * @param preparedStage   已经完成运行时预热的目标展台快照
     * @param expectedVersion 调用方持有的目标展台版本
     * @param operator        当前运营人员标识
     * @throws BusinessException 目标配置已变化、状态不匹配或条件更新失败时抛出
     */
    public void publish(ActivityStageEntity preparedStage, int expectedVersion, String operator) {
        transactionExecutor.execute(() -> switchActiveStage(preparedStage, expectedVersion, operator));
    }

    private void switchActiveStage(ActivityStageEntity preparedStage, int expectedVersion, String operator) {
        List<ActivityStageEntity> lockedStages = activityStageRepository.lockByChannelAndSource(
                preparedStage.channel(), preparedStage.source());
        ActivityStageEntity targetStage = lockedStages.stream()
                .filter(stage -> stage.stageId().equals(preparedStage.stageId()))
                .findFirst()
                .orElseThrow(() -> BusinessException.of(BusinessResponseCode.ACTIVITY_STAGE_CONFLICT,
                        preparedStage.stageId(), expectedVersion));

        ensurePreparedSnapshotIsCurrent(preparedStage, targetStage, expectedVersion);
        expireCurrentActiveStages(lockedStages, targetStage.stageId(), operator);
        activateTargetStage(targetStage, operator);
    }

    private void ensurePreparedSnapshotIsCurrent(ActivityStageEntity preparedStage, ActivityStageEntity targetStage,
                                                 int expectedVersion) {
        if (targetStage.version() != expectedVersion
                || !targetStage.activityId().equals(preparedStage.activityId())
                || targetStage.state() != ActivityStageStateVO.CREATE) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_STAGE_CONFLICT,
                    targetStage.stageId(), expectedVersion, targetStage.version());
        }
    }

    private void expireCurrentActiveStages(List<ActivityStageEntity> lockedStages, Long targetStageId, String operator) {
        lockedStages.stream()
                .filter(ActivityStageEntity::isActive)
                .filter(stage -> !stage.stageId().equals(targetStageId))
                .forEach(stage -> expireStage(stage, operator));
    }

    private void expireStage(ActivityStageEntity stage, String operator) {
        ActivityStageStateChange change = new ActivityStageStateChange(
                stage.stageId(), ActivityStageStateVO.ACTIVE, ActivityStageStateVO.EXPIRED, stage.version(), operator);
        if (!activityStageRepository.changeState(change)) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_STAGE_CONFLICT, stage.stageId(), stage.version());
        }
    }

    private void activateTargetStage(ActivityStageEntity stage, String operator) {
        ActivityStageStateChange change = new ActivityStageStateChange(
                stage.stageId(), ActivityStageStateVO.CREATE, ActivityStageStateVO.ACTIVE, stage.version(), operator);
        if (!activityStageRepository.changeState(change)) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_STAGE_CONFLICT, stage.stageId(), stage.version());
        }
    }
}
