package com.lavyoung.marketforge.infrastructure.persistent.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lavyoung.marketforge.infrastructure.persistent.model.AwardDefinitionStateUpdateParam;
import com.lavyoung.marketforge.infrastructure.persistent.po.AwardPO;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

/**
 * 奖品定义数据访问接口。
 *
 * <p>提供奖品定义查询、草稿条件修改和生命周期状态条件更新。
 * 普通新增继续使用 MyBatis-Plus 的 {@link BaseMapper#insert(Object)}。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
public interface IAwardDao extends BaseMapper<AwardPO> {

    /**
     * 根据奖品业务标识查询完整奖品定义。
     *
     * @param awardId 奖品业务标识
     * @return 匹配的奖品记录；不存在时返回空
     */
    Optional<AwardPO> queryAwardByAwardId(@Param("awardId") Long awardId);

    /**
     * 根据奖品业务编码查询完整奖品定义。
     *
     * @param awardKey 奖品业务编码
     * @return 匹配的奖品记录；不存在时返回空
     */
    Optional<AwardPO> queryAwardByAwardKey(@Param("awardKey") String awardKey);

    /**
     * 根据奖品业务编码更新旧版奖品发放配置。
     *
     * <p>该方法暂时保留，用于兼容现有策略链路。</p>
     *
     * @param award 包含更新内容的奖品持久化对象
     * @return 受影响的记录数
     */
    int updateByAwardKey(AwardPO award);

    /**
     * 使用草稿状态和乐观锁版本更新奖品定义。
     *
     * <p>只有数据库当前状态为 {@code draft}，且数据库版本等于参数版本时才会更新。
     * 更新成功后数据库版本自动加一。</p>
     *
     * @param award 最新的奖品定义
     * @return 受影响的记录数；返回零表示状态或版本不匹配
     */
    int updateDraftDefinition(AwardPO award);

    /**
     * 使用当前状态和乐观锁版本更新奖品生命周期状态。
     *
     * @param stateUpdate 状态条件更新参数
     * @return 受影响的记录数；返回零表示奖品不存在、状态不匹配或版本冲突
     */
    int changeDefinitionState(AwardDefinitionStateUpdateParam stateUpdate);
}