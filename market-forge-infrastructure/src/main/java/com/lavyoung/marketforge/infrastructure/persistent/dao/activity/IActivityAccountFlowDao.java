package com.lavyoung.marketforge.infrastructure.persistent.dao.activity;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivityAccountFlowPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

/**
 *
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/18
 */
@Mapper
public interface IActivityAccountFlowDao extends BaseMapper<ActivityAccountFlowPO> {

    /**
     * 根据唯一流水标识查询账户流水。
     *
     * @param flowId 流水标识
     * @return 账户流水持久化对象；不存在时返回 {@link Optional#empty()}
     */
    Optional<ActivityAccountFlowPO> queryByFlowId(@Param("flowId") String flowId);

    /**
     * 根据外部业务标识查询账户流水，用于幂等校验。
     *
     * @param bizId 外部业务标识
     * @return 账户流水持久化对象；不存在时返回 {@link Optional#empty()}
     */
    Optional<ActivityAccountFlowPO> queryByBizId(@Param("bizId") String bizId);
}
