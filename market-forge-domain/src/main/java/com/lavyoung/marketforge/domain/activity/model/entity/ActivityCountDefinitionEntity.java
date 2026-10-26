package com.lavyoung.marketforge.domain.activity.model.entity;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 活动次数配置运营定义实体。
 *
 * <p>该实体描述一次活动 SKU 入账能够给用户增加的总、月、日抽奖额度，
 * 同时维护后台配置所需的名称、乐观锁版本和审计信息。</p>
 *
 * <p>运行时额度入账继续使用 {@link ActivityCountEntity}。本实体只服务于运营配置，
 * 避免把名称、版本和审计字段带入高频抽奖与额度入账链路。</p>
 *
 * @param activityCountId   活动次数配置业务标识
 * @param activityCountName 活动次数配置名称，用于后台识别和选择
 * @param totalCount        一次入账增加的总抽奖次数
 * @param dayCount          一次入账增加的当日抽奖次数
 * @param monthCount        一次入账增加的当月抽奖次数
 * @param version           乐观锁版本，从 1 开始
 * @param createdBy         创建人标识
 * @param updatedBy         最后修改人标识
 * @param createTime        创建时间；首次持久化前允许为空
 * @param updateTime        最后修改时间；首次持久化前允许为空
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/26
 */
public record ActivityCountDefinitionEntity(
        Long activityCountId,
        String activityCountName,
        int totalCount,
        int dayCount,
        int monthCount,
        int version,
        String createdBy,
        String updatedBy,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {

    /**
     * 创建并校验活动次数配置。
     *
     * <p>次数配置代表实际入账额度，因此不接受用 {@code 0} 表示“不限”。
     * 日额度不得超过月额度，月额度不得超过总额度，否则用户账户可能出现
     * 总额度尚未耗尽但日/月额度关系不一致的问题。</p>
     *
     * @throws NullPointerException     配置标识、名称或审计人员为空时抛出
     * @throws IllegalArgumentException 配置标识、名称、次数关系或版本非法时抛出
     */
    public ActivityCountDefinitionEntity {
        Objects.requireNonNull(activityCountId, "activityCountId must not be null");
        requireText(activityCountName, "activityCountName", 64);
        requireText(createdBy, "createdBy", 64);
        requireText(updatedBy, "updatedBy", 64);

        if (activityCountId <= 0) {
            throw new IllegalArgumentException("activityCountId must be positive");
        }
        if (dayCount < 1) {
            throw new IllegalArgumentException("dayCount must be greater than zero");
        }
        if (monthCount < dayCount) {
            throw new IllegalArgumentException("monthCount must be greater than or equal to dayCount");
        }
        if (totalCount < monthCount) {
            throw new IllegalArgumentException("totalCount must be greater than or equal to monthCount");
        }
        if (version < 1) {
            throw new IllegalArgumentException("version must be greater than zero");
        }
    }

    /**
     * 生成修改后的次数配置。
     *
     * <p>本方法返回新的不可变实体，不主动增加版本号。持久化更新时使用当前
     * {@code version} 作为 SQL 条件，更新成功后由数据库将版本原子增加一，
     * 防止两个运营人员同时编辑时发生静默覆盖。</p>
     *
     * @param newActivityCountName 新的配置名称
     * @param newTotalCount        新的总入账次数
     * @param newDayCount          新的日入账次数
     * @param newMonthCount        新的月入账次数
     * @param operator             本次修改的运营人员
     * @return 保留业务标识、创建信息和当前版本的新配置实体
     * @throws NullPointerException     操作人或配置名称为空时抛出
     * @throws IllegalArgumentException 配置名称、次数关系或操作人非法时抛出
     */
    public ActivityCountDefinitionEntity update(String newActivityCountName, int newTotalCount, int newDayCount, int newMonthCount, String operator) {
        return new ActivityCountDefinitionEntity(activityCountId, newActivityCountName, newTotalCount, newDayCount,
                newMonthCount, version, createdBy, operator, createTime, updateTime);
    }

    private static void requireText(String value, String fieldName, int maximumLength) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        if (value.length() > maximumLength) {
            throw new IllegalArgumentException(fieldName + " length must not exceed " + maximumLength);
        }
    }
}
