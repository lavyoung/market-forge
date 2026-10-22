package com.lavyoung.marketforge.domain.activity.repository;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivityStageEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityStageStateChange;

import java.util.List;
import java.util.Optional;

/**
 * 活动展台仓储端口。
 *
 * <p>负责活动展台的创建、草稿修改、状态条件更新和当前生效活动查询。
 * MyBatis、数据库行锁和唯一键异常转换由基础设施实现。</p>
 *
 * <p>该端口与 {@link IActivityRepository} 分离，避免把投放渠道管理和用户活动账户、
 * 订单、库存等交易能力混入同一个仓储。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
public interface IActivityStageRepository {

    /**
     * 根据展台业务标识查询配置。
     *
     * @param stageId 展台业务标识
     * @return 匹配的展台配置；不存在时返回空
     */
    Optional<ActivityStageEntity> queryByStageId(Long stageId);

    /**
     * 查询指定渠道和来源当前生效的展台配置。
     *
     * @param channel 投放渠道
     * @param source  流量来源
     * @return 当前生效配置；不存在时返回空
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 数据库存在多个生效配置时抛出
     */
    Optional<ActivityStageEntity> queryActive(String channel, String source);

    /**
     * 对指定渠道和来源的全部展台记录加排他锁并返回当前快照。
     *
     * <p>发布事务通过该方法串行化同一 SC 的状态切换，防止两个并发请求同时成为生效配置。
     * 实现方必须使用稳定顺序锁定记录，降低死锁概率。</p>
     *
     * @param channel 投放渠道
     * @param source  流量来源
     * @return 已锁定的展台记录；不存在时返回空集合
     */
    List<ActivityStageEntity> lockByChannelAndSource(String channel, String source);

    /**
     * 创建一条待上架活动展台配置。
     *
     * @param stage 待创建的展台配置
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 配置重复或写入失败时抛出
     */
    void create(ActivityStageEntity stage);

    /**
     * 使用待上架状态和乐观锁版本修改展台配置。
     *
     * @param stage 最新展台配置
     * @return 更新成功返回 {@code true}；状态或版本不匹配时返回 {@code false}
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 修改后配置违反唯一键时抛出
     */
    boolean updateDraft(ActivityStageEntity stage);

    /**
     * 使用当前状态和版本条件推进展台生命周期。
     *
     * @param stateChange 状态变更参数
     * @return 更新成功返回 {@code true}；状态或版本不匹配时返回 {@code false}
     */
    boolean changeState(ActivityStageStateChange stateChange);
}
