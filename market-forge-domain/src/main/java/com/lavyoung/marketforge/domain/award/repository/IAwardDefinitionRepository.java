package com.lavyoung.marketforge.domain.award.repository;

import com.lavyoung.marketforge.domain.award.model.entity.AwardDefinitionEntity;
import com.lavyoung.marketforge.domain.award.model.entity.AwardDefinitionStateChange;

import java.util.List;
import java.util.Optional;

/**
 * 奖品定义仓储端口。
 *
 * <p>负责奖品基础定义的查询、创建、草稿修改和状态条件更新。
 * 具体 MyBatis、数据库表和乐观锁 SQL 由基础设施层实现。</p>
 *
 * <p>用户中奖记录继续由 {@link IAwardRepository} 管理，避免把奖品配置和用户履约记录
 * 混入同一个仓储实现。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/21
 */
public interface IAwardDefinitionRepository {

    /**
     * 根据奖品标识查询奖品定义。
     *
     * @param awardId 奖品业务标识
     * @return 匹配的奖品定义；不存在时返回空
     */
    Optional<AwardDefinitionEntity> queryByAwardId(Long awardId);

    /**
     * 批量查询奖品定义。
     *
     * <p>该方法用于策略发布、活动发布等需要一次校验多个奖品的场景。
     * 实现方必须使用一条批量 SQL 完成读取，禁止循环调用单奖品查询产生 N+1。</p>
     *
     * @param awardIds 奖品业务标识集合
     * @return 查询到的奖品定义；不存在的标识不会产生占位元素
     * @throws NullPointerException 集合或集合中的奖品标识为空时抛出
     */
    List<AwardDefinitionEntity> queryByAwardIds(List<Long> awardIds);

    /**
     * 根据奖品业务编码查询奖品定义。
     *
     * @param awardCode 奖品业务编码
     * @return 匹配的奖品定义；不存在时返回空
     */
    Optional<AwardDefinitionEntity> queryByAwardCode(String awardCode);

    /**
     * 创建一条草稿奖品定义。
     *
     * @param definition 待创建的奖品定义
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 奖品标识或业务编码重复、写入失败时抛出
     */
    void create(AwardDefinitionEntity definition);

    /**
     * 使用奖品标识、草稿状态和乐观锁版本更新奖品定义。
     *
     * <p>实现方只能更新草稿状态的数据，并在成功后将版本加一。
     * 已启用或已停用配置不得通过该方法修改。</p>
     *
     * @param definition 包含最新配置和期望版本的奖品定义
     * @return 更新成功时返回 {@code true}；状态或版本不匹配时返回 {@code false}
     */
    boolean updateDraft(AwardDefinitionEntity definition);

    /**
     * 使用当前状态和乐观锁版本推进奖品定义状态。
     *
     * @param stateChange 状态变更请求
     * @return 状态更新成功时返回 {@code true}；当前状态或版本不匹配时返回 {@code false}
     */
    boolean changeState(AwardDefinitionStateChange stateChange);
}
