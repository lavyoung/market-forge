package com.lavyoung.marketforge.domain.behavior.service;

import com.lavyoung.marketforge.domain.behavior.model.entity.BehaviorEntity;

import java.util.List;

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
     * @return 创建成功的返利订单号集合；没有返利配置时返回空集合
     * @throws NullPointerException 当 {@code behavior} 或必要字段为空时抛出
     */
    List<String> createOrder(BehaviorEntity behavior);
}
