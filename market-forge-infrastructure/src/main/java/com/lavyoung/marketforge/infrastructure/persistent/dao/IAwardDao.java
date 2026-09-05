package com.lavyoung.marketforge.infrastructure.persistent.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lavyoung.marketforge.infrastructure.persistent.po.AwardPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

/**
 * 奖品数据访问接口。
 * <p>
 * 在 MyBatis-Plus 通用 CRUD 能力之外，提供按奖品业务标识查询和更新的方法。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0-SNAPSHOT
 */
@Mapper
public interface IAwardDao extends BaseMapper<AwardPO> {

    /**
     * 根据奖品业务标识查询奖品。
     *
     * @param awardKey 奖品业务标识
     * @return 奖品持久化对象；不存在时返回 {@link Optional#empty()}
     */
    Optional<AwardPO> queryAwardByAwardKey(@Param("awardKey") String awardKey);

    /**
     * 根据奖品业务标识更新奖品。
     *
     * @param award 包含更新内容的奖品持久化对象
     * @return 受影响的记录数
     */
    int updateByAwardKey(AwardPO award);

}
