package com.lavyoung.marketforge.infrastructure.persistent.dao.behavior;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lavyoung.marketforge.infrastructure.persistent.po.behavior.BehaviorRebateConfigPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户行为返利配置数据访问接口。
 *
 * <p>只负责 {@code behavior_rebate_config} 表的基础查询，不承载返利订单生成规则。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/08
 */
@Mapper
public interface IBehaviorRebateConfigDao extends BaseMapper<BehaviorRebateConfigPO> {

    /**
     * 按行为类型查询返利配置。
     *
     * @param behaviorType 行为类型编码，例如 {@code sign}
     * @return 返利配置集合；没有配置时返回空集合
     */
    List<BehaviorRebateConfigPO> queryByBehaviorType(@Param("behaviorType") String behaviorType);
}
