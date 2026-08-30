package com.lavyoung.marketforge.infrastructure.persistent.dao;

import com.lavyoung.marketforge.infrastructure.persistent.po.AwardPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

/**
 * 奖品数据访问接口。
 *
 * @author lavyoung
 * @version 1.0.0-SNAPSHOT
 * @email lavyoung1325@outlook.com
 */
@Mapper
public interface IAwardDao {

    /**
     * 根据奖品业务标识查询奖品。
     *
     * @param awardKey 奖品业务标识
     * @return 奖品持久化对象；不存在时返回空
     */
    Optional<AwardPO> queryAwardByAwardKey(@Param("awardKey") String awardKey);

    /**
     * 新增奖品。
     *
     * @param award 奖品持久化对象
     * @return 受影响的记录数
     */
    int insert(AwardPO award);

    /**
     * 根据奖品业务标识更新奖品。
     *
     * @param award 包含更新内容的奖品持久化对象
     * @return 受影响的记录数
     */
    int updateByAwardKey(AwardPO award);

}
