package com.lavyoung.marketforge.infrastructure.persistent.assembler.activity;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivityAccountMonthEntity;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivityAccountMonthPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.time.YearMonth;
import java.util.List;

/**
 * 用户活动月账户持久化对象与领域实体转换器。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface ActivityAccountMonthAssembler {

    /**
     * 将用户活动月账户持久化对象转换为领域实体。
     *
     * @param source 用户活动月账户持久化对象
     * @return 用户活动月账户领域实体
     */
    ActivityAccountMonthEntity toEntity(ActivityAccountMonthPO source);

    /**
     * 批量将用户活动月账户持久化对象转换为领域实体。
     *
     * @param sources 用户活动月账户持久化对象列表
     * @return 用户活动月账户领域实体列表
     */
    List<ActivityAccountMonthEntity> toEntities(List<ActivityAccountMonthPO> sources);

    /**
     * 将用户活动月账户领域实体转换为待新增的持久化对象。
     *
     * @param source 用户活动月账户领域实体
     * @return 忽略数据库生成字段后的用户活动月账户持久化对象
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    ActivityAccountMonthPO toPO(ActivityAccountMonthEntity source);

    /**
     * 批量将用户活动月账户领域实体转换为待新增的持久化对象。
     *
     * @param sources 用户活动月账户领域实体列表
     * @return 忽略数据库生成字段后的用户活动月账户持久化对象列表
     */
    List<ActivityAccountMonthPO> toPOs(List<ActivityAccountMonthEntity> sources);

    /**
     * 将数据库月份字符串转换为领域层月份值。
     *
     * @param value yyyy-MM 格式的月份字符串
     * @return 月份值；当入参为空时返回 null
     */
    default YearMonth toYearMonth(String value) {
        return value == null ? null : YearMonth.parse(value);
    }

    /**
     * 将领域层月份值转换为数据库月份字符串。
     *
     * @param value 月份值
     * @return yyyy-MM 格式的月份字符串；当入参为空时返回 null
     */
    default String toString(YearMonth value) {
        return value == null ? null : value.toString();
    }
}
