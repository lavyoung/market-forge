package com.lavyoung.marketforge.domain.behavior.service;

import com.lavyoung.marketforge.domain.behavior.model.entity.BehaviorEntity;
import com.lavyoung.marketforge.domain.behavior.model.entity.BehaviorRebateOrderResult;

/**
 * 用户行为返利领域服务。
 *
 * <p>负责把一次用户行为转换为一批返利订单。
 * 例如用户签到后，系统根据返利配置生成 SKU 返利订单和积分返利订单。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/08
 */
public interface IBehaviorRebateService {

    /**
     * 根据用户行为创建返利订单。
     *
     * <p>
     * 先查询行为返利配置，再为每条配置生成一笔用户返利订单，最后交给仓储保存。
     * 如果该行为没有配置返利，则返回空集合。</p>
     *
     * @param behavior 用户行为实体，包含用户标识、行为类型和外部业务幂等号
     * @return 返利订单创建结果，包含订单号和待投递事件标识；没有返利配置时返回空结果
     * @throws NullPointerException 当 {@code behavior} 或必要字段为空时抛出
     */
    BehaviorRebateOrderResult createOrder(BehaviorEntity behavior);

    /**
     * 判断用户指定行为是否已经处理过指定外部业务号。
     *
     * <p>典型场景是查询“今日是否已签到”：
     * application 层按当天日期生成签到幂等号，领域服务再判断该幂等号下是否已经存在返利订单。</p>
     *
     * @param behavior 用户行为实体，包含用户标识、行为类型和外部业务幂等号
     * @return 已存在返利订单返回 {@code true}，否则返回 {@code false}
     * @throws NullPointerException 当 {@code behavior} 或必要字段为空时抛出
     */
    boolean existsOrder(BehaviorEntity behavior);
}
