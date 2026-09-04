package com.lavyoung.marketforge.infrastructure.persistent.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 持久化对象的审计字段基类。
 * <p>
 * 所有数据库实体均应继承该类，由 MyBatis-Plus 在新增和更新时自动维护审计时间。
 *
 * @author lavyoung
 * @version 1.0.0
 * @email lavyoung1325@outlook.com
 * @date 2026/09/03
 */
@Getter
@Setter
public class BasePO {

    /**
     * 数据创建时间，在新增数据时自动填充。
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 数据最后更新时间，在新增和更新数据时自动填充。
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
