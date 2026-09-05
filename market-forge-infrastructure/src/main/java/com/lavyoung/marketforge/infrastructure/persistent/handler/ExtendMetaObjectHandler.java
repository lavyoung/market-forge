package com.lavyoung.marketforge.infrastructure.persistent.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 审计字段自动填充处理器。
 * <p>
 * 新增数据时填充创建时间和更新时间，更新数据时刷新更新时间。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0-SNAPSHOT
 */
@Component
public class ExtendMetaObjectHandler implements MetaObjectHandler {

    /**
     * 为新增的持久化对象填充创建时间和更新时间。
     *
     * @param metaObject MyBatis 封装的新增参数对象
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
    }

    /**
     * 为更新的持久化对象刷新更新时间。
     *
     * @param metaObject MyBatis 封装的更新参数对象
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        setFieldValByName("updateTime", LocalDateTime.now(), metaObject);
    }
}
