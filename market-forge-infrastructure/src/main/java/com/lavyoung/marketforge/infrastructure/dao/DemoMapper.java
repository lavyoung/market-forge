package com.lavyoung.marketforge.infrastructure.dao;

import com.lavyoung.marketforge.infrastructure.po.DemoPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
/**
 * 演示对象的 MyBatis 数据访问映射器。
 *
 * @author lavyoung
 * @email lavyoung1325@outlook.com
 * @version 1.0.0-SNAPSHOT
 */
public interface DemoMapper {

    DemoPO selectById(@Param("id") Long id);

    int insert(DemoPO po);

}
