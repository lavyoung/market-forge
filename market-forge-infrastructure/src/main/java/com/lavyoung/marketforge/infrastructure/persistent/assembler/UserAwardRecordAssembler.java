package com.lavyoung.marketforge.infrastructure.persistent.assembler;

import com.lavyoung.marketforge.domain.award.model.entity.UserAwardRecordEntity;
import com.lavyoung.marketforge.infrastructure.persistent.po.UserAwardRecordPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * 用户中奖记录持久化对象与领域实体转换器。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface UserAwardRecordAssembler {

    /**
     * 将用户中奖记录持久化对象转换为领域实体。
     *
     * @param source 用户中奖记录持久化对象
     * @return 用户中奖记录领域实体
     */
    UserAwardRecordEntity toEntity(UserAwardRecordPO source);

    /**
     * 批量将用户中奖记录持久化对象转换为领域实体。
     *
     * @param sources 用户中奖记录持久化对象列表
     * @return 用户中奖记录领域实体列表
     */
    List<UserAwardRecordEntity> toEntities(List<UserAwardRecordPO> sources);

    /**
     * 将用户中奖记录领域实体转换为待新增的持久化对象。
     *
     * @param source 用户中奖记录领域实体
     * @return 忽略数据库生成字段后的用户中奖记录持久化对象
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    UserAwardRecordPO toPO(UserAwardRecordEntity source);

    /**
     * 批量将用户中奖记录领域实体转换为待新增的持久化对象。
     *
     * @param sources 用户中奖记录领域实体列表
     * @return 忽略数据库生成字段后的用户中奖记录持久化对象列表
     */
    List<UserAwardRecordPO> toPOs(List<UserAwardRecordEntity> sources);
}
