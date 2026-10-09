package com.lavyoung.marketforge.infrastructure.persistent.assembler.behavior;

import com.lavyoung.marketforge.domain.behavior.model.entity.UserBehaviorRebateOrderEntity;
import com.lavyoung.marketforge.domain.behavior.model.vo.BehaviorTypeVO;
import com.lavyoung.marketforge.domain.behavior.model.vo.RebateTypeVO;
import com.lavyoung.marketforge.infrastructure.persistent.po.behavior.UserBehaviorRebateOrderPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * 用户行为返利订单持久化对象与领域实体转换器。
 *
 * <p>返利订单写入数据库时，需要把领域枚举转换为稳定字符串编码。
 * 例如 {@code BehaviorTypeVO.SIGN} 保存为 {@code sign}，
 * {@code RebateTypeVO.SKU} 保存为 {@code sku}。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/08
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserBehaviorRebateOrderAssembler {

    /**
     * 将用户行为返利订单领域实体转换为待新增的持久化对象。
     *
     * <p>数据库主键和审计时间由 MyBatis-Plus 自动填充，因此这里忽略。</p>
     *
     * @param source 用户行为返利订单领域实体
     * @return 用户行为返利订单持久化对象
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    UserBehaviorRebateOrderPO toPO(UserBehaviorRebateOrderEntity source);

    /**
     * 将行为类型枚举转换为数据库编码。
     *
     * @param behaviorType 行为类型枚举
     * @return 行为类型编码；枚举为空时返回 {@code null}
     */
    default String fromBehaviorTypeVO(BehaviorTypeVO behaviorType) {
        return behaviorType == null ? null : behaviorType.getCode();
    }

    /**
     * 将返利类型枚举转换为数据库编码。
     *
     * @param rebateType 返利类型枚举
     * @return 返利类型编码；枚举为空时返回 {@code null}
     */
    default String fromRebateTypeVO(RebateTypeVO rebateType) {
        return rebateType == null ? null : rebateType.getCode();
    }
}
