package com.lavyoung.marketforge.domain.activity.repository;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivitySkuDefinitionEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivitySkuStockAdjustmentEntity;

import java.util.Optional;

/**
 * 活动 SKU 运营定义仓储端口。
 *
 * <p>该端口服务于运营后台的 SKU 创建、配置修改和库存调整。运行时兑换、库存预扣和
 * 额度入账继续使用 {@link IActivityRepository}，避免把后台管理行为与高频交易行为
 * 混合在同一个仓储接口中。</p>
 *
 * <p>库存调整必须同时完成 SKU 库存原子更新和调整流水写入。实现层需要使用本地事务
 * 保证二者要么同时成功，要么同时回滚。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/26
 */
public interface IActivitySkuDefinitionRepository {

    /**
     * 按 SKU 查询运营定义。
     *
     * @param sku SKU 业务标识
     * @return 匹配的 SKU 定义；不存在时返回 {@link Optional#empty()}
     */
    Optional<ActivitySkuDefinitionEntity> queryBySku(Long sku);

    /**
     * 创建活动 SKU。
     *
     * <p>首次创建时库存总量和剩余库存必须相等，版本号必须为 1。实现层应依赖
     * SKU 唯一索引防止并发创建重复数据，并将唯一键异常转换为稳定的业务异常。</p>
     *
     * @param definition 待创建的 SKU 定义
     * @throws com.lavyoung.marketforge.types.exception.BusinessException SKU 重复或写入失败时抛出
     */
    void create(ActivitySkuDefinitionEntity definition);

    /**
     * 按 SKU 和乐观锁版本更新普通配置信息。
     *
     * <p>该方法只能更新活动关联、次数配置、积分价格、修改人和版本，禁止更新库存总量
     * 与剩余库存。若请求改变活动或次数配置，实现层还必须保证数据库中的 SKU 尚未发生
     * 库存消费，防止读取后发生并发消费却仍然修改业务归属。</p>
     *
     * <p>SQL 更新成功后应将版本号原子增加一。</p>
     *
     * @param definition 修改后的 SKU 定义
     * @return 成功更新一条记录返回 {@code true}；SKU 不存在、版本冲突或已使用时返回 {@code false}
     */
    boolean updateConfiguration(ActivitySkuDefinitionEntity definition);

    /**
     * 原子调整 SKU 库存并保存调整流水。
     *
     * <p>实现层必须在同一个本地事务中完成以下操作：</p>
     *
     * <ol>
     *     <li>按 SKU、预期版本、调整前总库存和调整前剩余库存执行条件更新。</li>
     *     <li>总库存和剩余库存同时增加 {@code quantityDelta}。</li>
     *     <li>检查调整后的库存不会小于零或破坏库存关系。</li>
     *     <li>将 SKU 版本号原子增加一。</li>
     *     <li>写入包含调整前后快照、原因和操作人的库存调整流水。</li>
     * </ol>
     *
     * <p>必须先确认库存条件更新成功，再写入调整流水。流水写入失败时事务必须回滚，
     * 不允许出现库存已经变化但没有审计记录的情况。</p>
     *
     * @param adjustment 库存调整实体
     * @return 库存更新和流水写入均成功时返回 {@code true}；版本或库存快照冲突时返回 {@code false}
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 调整流水重复或数据库写入失败时抛出
     */
    boolean adjustStockAndRecord(ActivitySkuStockAdjustmentEntity adjustment);
}
