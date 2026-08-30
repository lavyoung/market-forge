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

    Optional<AwardPO> queryAwardByAwardKey(@Param("awardKey") String awardKey);

    int insert(AwardPO award);

    int updateByAwardKey(AwardPO award);

}
