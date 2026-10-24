package com.lavyoung.marketforge.application.award.model;

/**
 * 奖品配置字段类型。
 *
 * <p>用于描述运营后台应当使用何种控件编辑奖品配置。该枚举表达业务配置
 * 数据的类型，不直接依赖 Ant Design、React 等具体前端框架。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/24
 */
public enum AwardConfigFieldType {

    /**
     * 整数输入字段，例如积分数量。
     */
    INTEGER,

    /**
     * 普通文本字段，例如券模板编码或商品 SKU。
     */
    TEXT,

    /**
     * 单选字段，例如供应商或发放渠道。
     */
    SELECT,

    /**
     * 布尔开关字段。
     */
    BOOLEAN
}
