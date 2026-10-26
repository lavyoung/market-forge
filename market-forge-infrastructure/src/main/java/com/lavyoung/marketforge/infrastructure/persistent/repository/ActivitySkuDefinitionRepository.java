package com.lavyoung.marketforge.infrastructure.persistent.repository;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivitySkuDefinitionEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivitySkuStockAdjustmentEntity;
import com.lavyoung.marketforge.domain.activity.repository.IActivitySkuDefinitionRepository;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.activity.ActivitySkuDefinitionAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.activity.ActivitySkuStockAdjustmentAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.dao.activity.IActivitySkuDao;
import com.lavyoung.marketforge.infrastructure.persistent.dao.activity.IActivitySkuStockAdjustmentDao;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivitySkuPO;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivitySkuStockAdjustmentPO;
import com.lavyoung.marketforge.infrastructure.persistent.redis.IRedisService;
import com.lavyoung.marketforge.types.common.Constants;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

/**
 * 活动 SKU 运营定义仓储实现。
 *
 * <p>负责 SKU 运营写模型与持久化对象之间的转换、唯一键异常翻译、乐观锁条件更新、
 * 库存调整流水事务以及运行时缓存失效。</p>
 *
 * <p>库存调整必须先成功更新 SKU 库存，再写入调整流水。两个数据库操作处于同一个
 * 本地事务中，任何一步失败都会回滚，避免库存变化后缺少审计记录。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/26
 */
@Repository
@RequiredArgsConstructor
public class ActivitySkuDefinitionRepository implements IActivitySkuDefinitionRepository {

    private final IActivitySkuDao activitySkuDao;
    private final IActivitySkuStockAdjustmentDao activitySkuStockAdjustmentDao;
    private final ActivitySkuDefinitionAssembler activitySkuDefinitionAssembler;
    private final ActivitySkuStockAdjustmentAssembler activitySkuStockAdjustmentAssembler;
    private final IRedisService redisService;

    @Override
    public Optional<ActivitySkuDefinitionEntity> queryBySku(Long sku) {
        Objects.requireNonNull(sku, "sku must not be null");
        return activitySkuDao.queryBySku(sku).map(activitySkuDefinitionAssembler::toEntity);
    }

    @Override
    public void create(ActivitySkuDefinitionEntity definition) {
        Objects.requireNonNull(definition, "definition must not be null");
        ActivitySkuPO po = activitySkuDefinitionAssembler.toPO(definition);
        try {
            if (activitySkuDao.insert(po) != 1) {
                throw BusinessException.of(BusinessResponseCode.ACTIVITY_SKU_DEFINITION_CREATE_FAILED, definition.sku());
            }
        } catch (DuplicateKeyException exception) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_SKU_DEFINITION_DUPLICATED, exception, definition.sku());
        }
    }

    @Override
    public boolean updateConfiguration(ActivitySkuDefinitionEntity definition) {
        Objects.requireNonNull(definition, "definition must not be null");
        boolean updated = activitySkuDao.updateConfiguration(activitySkuDefinitionAssembler.toPO(definition)) == 1;
        if (updated) {
            evictSkuDefinition(definition.sku());
        }
        return updated;
    }

    /**
     * 原子调整库存并写入调整流水。
     *
     * <p>方法返回 {@code false} 代表版本或库存快照冲突，此时不会写入调整流水。
     * 调整流水写入数量异常或业务标识重复时抛出业务异常，Spring 会回滚此前的库存更新。</p>
     *
     * @param adjustment 库存调整实体
     * @return 库存和流水均写入成功时返回 {@code true}；条件更新失败时返回 {@code false}
     * @throws NullPointerException 调整实体为空时抛出
     * @throws BusinessException    调整流水重复或写入失败时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean adjustStockAndRecord(ActivitySkuStockAdjustmentEntity adjustment) {
        Objects.requireNonNull(adjustment, "adjustment must not be null");
        ActivitySkuStockAdjustmentPO adjustmentPO = activitySkuStockAdjustmentAssembler.toPO(adjustment);
        if (activitySkuDao.adjustStock(adjustmentPO) != 1) {
            return false;
        }
        insertAdjustmentRecord(adjustmentPO);
        evictSkuCaches(adjustment.sku());
        return true;
    }

    /**
     * 写入库存调整流水并转换唯一键异常。
     *
     * @param adjustmentPO 库存调整流水持久化对象
     * @throws BusinessException 调整标识重复或流水写入失败时抛出
     */
    private void insertAdjustmentRecord(ActivitySkuStockAdjustmentPO adjustmentPO) {
        try {
            if (activitySkuStockAdjustmentDao.insertAdjustment(adjustmentPO) != 1) {
                throw BusinessException.of(BusinessResponseCode.ACTIVITY_SKU_STOCK_ADJUSTMENT_FAILED,
                        adjustmentPO.getSku(), adjustmentPO.getAdjustmentId());
            }
        } catch (DuplicateKeyException exception) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_SKU_STOCK_ADJUSTMENT_DUPLICATED, exception,
                    adjustmentPO.getSku(), adjustmentPO.getAdjustmentId());
        }
    }

    private void evictSkuDefinition(Long sku) {
        redisService.delete(Constants.RedisKeys.ACTIVITY_SKU_KEY + sku);
    }

    private void evictSkuCaches(Long sku) {
        redisService.delete(Constants.RedisKeys.ACTIVITY_SKU_KEY + sku);
        redisService.delete(Constants.RedisKeys.ACTIVITY_SKU_STOCK_COUNT_KEY + sku);
    }
}
