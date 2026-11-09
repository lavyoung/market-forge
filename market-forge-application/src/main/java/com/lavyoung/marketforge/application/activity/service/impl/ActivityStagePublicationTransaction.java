package com.lavyoung.marketforge.application.activity.service.impl;

import com.lavyoung.marketforge.application.tx.ITransactionExecutor;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityStageEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityStagePublicationEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityStageStateChange;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivityStageStateVO;
import com.lavyoung.marketforge.domain.activity.repository.IActivityStagePublicationRepository;
import com.lavyoung.marketforge.domain.activity.repository.IActivityStageRepository;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * 活动展台发布事务。
 *
 * <p>在同一本地事务中完成旧展台失效、目标展台激活和发布任务成功状态写入。
 * 任一条件更新失败都会抛出异常并回滚整个事务，防止运行中的展台状态与运营侧
 * 发布记录产生不一致。</p>
 *
 * <p>配置检查和运行数据预热不属于数据库事务。它们必须由外层发布应用服务提前
 * 完成，避免执行 Redis 写入和概率表装配时长期占用数据库连接与行锁。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/09
 */
@Component
@RequiredArgsConstructor
public class ActivityStagePublicationTransaction {

    private final IActivityStageRepository activityStageRepository;
    private final IActivityStagePublicationRepository activityStagePublicationRepository;
    private final ITransactionExecutor transactionExecutor;

    /**
     * 原子切换生效展台并完成发布任务。
     *
     * <p>调用方必须先将发布任务推进到 {@code switching} 阶段，再通过领域实体的
     * {@link ActivityStagePublicationEntity#succeed(String, java.time.LocalDateTime)}
     * 方法生成成功终态。</p>
     *
     * @param preparedStage        已经完成配置检查和运行数据预热的目标展台
     * @param currentPublication  处于切换阶段的当前发布任务
     * @param succeededPublication 由当前发布任务生成的成功终态
     * @throws NullPointerException     任一参数为空时抛出
     * @throws IllegalArgumentException 展台快照与发布快照不一致，或成功终态不是由当前任务合法生成时抛出
     * @throws BusinessException        展台已变化、条件更新失败或发布任务发生并发冲突时抛出
     */
    public void publish(ActivityStageEntity preparedStage, ActivityStagePublicationEntity currentPublication,
                        ActivityStagePublicationEntity succeededPublication) {
        validatePublicationCommit(preparedStage, currentPublication, succeededPublication);
        transactionExecutor.execute(() -> {
            switchActiveStage(preparedStage, currentPublication.stageVersion(), succeededPublication.operator());
            completePublication(currentPublication, succeededPublication);
        });
    }

    /**
     * 使用旧发布协议切换生效展台。
     *
     * <p>该入口只在迁移期间兼容原有 {@code ActivityStageApplicationServiceImpl}。
     * 它不会记录发布任务，新发布应用服务不得调用该方法。</p>
     *
     * @param preparedStage   已经完成运行数据预热的目标展台
     * @param expectedVersion 调用方持有的展台版本
     * @param operator        当前运营人员
     * @throws BusinessException 展台已变化或条件更新失败时抛出
     * @deprecated 使用包含当前发布任务和成功终态的新发布方法
     */
    @Deprecated(since = "1.0.0", forRemoval = false)
    public void publish(ActivityStageEntity preparedStage, int expectedVersion, String operator) {
        transactionExecutor.execute(() -> switchActiveStage(preparedStage, expectedVersion, operator));
    }

    /**
     * 校验准备发布的展台和两个发布任务快照属于同一个发布过程。
     *
     * <p>除了核对展台业务快照，还会重新调用领域实体的成功行为生成预期结果。
     * 传入的成功终态必须与领域行为生成的对象完全相等，避免调用方手工伪造状态。</p>
     *
     * @param preparedStage        已预热的目标展台
     * @param currentPublication   当前发布任务
     * @param succeededPublication 目标成功终态
     * @throws NullPointerException     任一参数为空时抛出
     * @throws IllegalArgumentException 快照不一致或成功迁移不合法时抛出
     */
    private void validatePublicationCommit(ActivityStageEntity preparedStage,
                                           ActivityStagePublicationEntity currentPublication,
                                           ActivityStagePublicationEntity succeededPublication) {
        Objects.requireNonNull(preparedStage, "preparedStage must not be null");
        Objects.requireNonNull(currentPublication, "currentPublication must not be null");
        Objects.requireNonNull(succeededPublication, "succeededPublication must not be null");

        boolean sameStageSnapshot = Objects.equals(preparedStage.stageId(), currentPublication.stageId())
                && preparedStage.version() == currentPublication.stageVersion()
                && Objects.equals(preparedStage.activityId(), currentPublication.activityId())
                && Objects.equals(preparedStage.channel(), currentPublication.channel())
                && Objects.equals(preparedStage.source(), currentPublication.source());

        if (!sameStageSnapshot) {
            throw new IllegalArgumentException("prepared stage must match publication snapshot");
        }

        ActivityStagePublicationEntity expectedSucceeded = currentPublication.succeed(
                succeededPublication.operator(), succeededPublication.publishedAt());
        if (!expectedSucceeded.equals(succeededPublication)) {
            throw new IllegalArgumentException("succeeded publication must be derived from current publication");
        }
    }

    /**
     * 在事务中锁定渠道展台并切换生效配置。
     *
     * @param preparedStage   已预热的目标展台
     * @param expectedVersion 预期展台版本
     * @param operator        当前运营人员
     * @throws BusinessException 目标不存在、快照过期或条件更新失败时抛出
     */
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

    /**
     * 核对预热前读取的展台和事务内锁定的最新展台。
     *
     * @param preparedStage   预热前读取的展台
     * @param targetStage     事务内锁定的最新展台
     * @param expectedVersion 预期展台版本
     * @throws BusinessException 展台已经被其他请求修改时抛出
     */
    private void ensurePreparedSnapshotIsCurrent(ActivityStageEntity preparedStage, ActivityStageEntity targetStage,
                                                 int expectedVersion) {
        if (targetStage.version() != expectedVersion
                || !targetStage.activityId().equals(preparedStage.activityId())
                || targetStage.state() != ActivityStageStateVO.CREATE) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_STAGE_CONFLICT,
                    targetStage.stageId(), expectedVersion, targetStage.version());
        }
    }

    /**
     * 失效同一渠道来源下除目标展台外的生效配置。
     *
     * @param lockedStages  已锁定的展台集合
     * @param targetStageId 目标展台标识
     * @param operator      当前运营人员
     * @throws BusinessException 任一条件更新失败时抛出
     */
    private void expireCurrentActiveStages(List<ActivityStageEntity> lockedStages, Long targetStageId,
                                           String operator) {
        lockedStages.stream()
                .filter(ActivityStageEntity::isActive)
                .filter(stage -> !stage.stageId().equals(targetStageId))
                .forEach(stage -> expireStage(stage, operator));
    }

    /**
     * 将一个旧生效展台变更为已失效。
     *
     * @param stage    旧生效展台
     * @param operator 当前运营人员
     * @throws BusinessException 条件更新失败时抛出
     */
    private void expireStage(ActivityStageEntity stage, String operator) {
        ActivityStageStateChange change = new ActivityStageStateChange(
                stage.stageId(), ActivityStageStateVO.ACTIVE, ActivityStageStateVO.EXPIRED,
                stage.version(), operator);
        if (!activityStageRepository.changeState(change)) {
            throw BusinessException.of(
                    BusinessResponseCode.ACTIVITY_STAGE_CONFLICT, stage.stageId(), stage.version());
        }
    }

    /**
     * 将目标草稿展台变更为生效状态。
     *
     * @param stage    目标草稿展台
     * @param operator 当前运营人员
     * @throws BusinessException 条件更新失败时抛出
     */
    private void activateTargetStage(ActivityStageEntity stage, String operator) {
        ActivityStageStateChange change = new ActivityStageStateChange(
                stage.stageId(), ActivityStageStateVO.CREATE, ActivityStageStateVO.ACTIVE,
                stage.version(), operator);
        if (!activityStageRepository.changeState(change)) {
            throw BusinessException.of(
                    BusinessResponseCode.ACTIVITY_STAGE_CONFLICT, stage.stageId(), stage.version());
        }
    }

    /**
     * 将发布任务从切换阶段推进为成功终态。
     *
     * <p>条件更新影响零行说明其他请求已经推进发布任务。此时必须抛出异常，
     * 使事务执行器同步回滚旧展台失效和目标展台激活。</p>
     *
     * @param currentPublication   当前切换阶段发布任务
     * @param succeededPublication 成功终态
     * @throws BusinessException 发布任务快照已经过期时抛出
     */
    private void completePublication(ActivityStagePublicationEntity currentPublication,
                                     ActivityStagePublicationEntity succeededPublication) {
        if (!activityStagePublicationRepository.transition(currentPublication, succeededPublication)) {
            throw BusinessException.of(
                    BusinessResponseCode.ACTIVITY_STAGE_PUBLICATION_CONFLICT,
                    currentPublication.publicationId(),
                    currentPublication.state().getCode(),
                    currentPublication.phase().getCode(),
                    currentPublication.attemptCount());
        }
    }
}
