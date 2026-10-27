package com.lavyoung.marketforge.application.activity.service;

import com.lavyoung.marketforge.application.activity.model.ActivitySkuDefinitionResult;
import com.lavyoung.marketforge.application.activity.model.AdjustActivitySkuStockCommand;
import com.lavyoung.marketforge.application.activity.model.CreateActivitySkuDefinitionCommand;
import com.lavyoung.marketforge.application.activity.model.UpdateActivitySkuDefinitionCommand;

/**
 * 活动 SKU 运营写用例。
 *
 * <p>负责创建 SKU、读取最新定义、修改普通配置和调整库存。所有写操作均需要校验
 * 活动生命周期、次数配置引用和乐观锁版本。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/26
 */
public interface IActivitySkuDefinitionApplicationService {

    /**
     * 创建活动 SKU。
     *
     * @param command 创建命令
     * @return 创建后的 SKU 定义
     */
    ActivitySkuDefinitionResult create(CreateActivitySkuDefinitionCommand command);

    /**
     * 查询 SKU 最新定义。
     *
     * @param sku SKU 业务标识
     * @return SKU 运营定义
     */
    ActivitySkuDefinitionResult queryBySku(Long sku);

    /**
     * 修改 SKU 普通配置。
     *
     * @param command 修改命令
     * @return 修改后的 SKU 定义
     */
    ActivitySkuDefinitionResult updateConfiguration(UpdateActivitySkuDefinitionCommand command);

    /**
     * 调整 SKU 库存。
     *
     * @param command 库存调整命令
     * @return 调整后的 SKU 定义
     */
    ActivitySkuDefinitionResult adjustStock(AdjustActivitySkuStockCommand command);
}