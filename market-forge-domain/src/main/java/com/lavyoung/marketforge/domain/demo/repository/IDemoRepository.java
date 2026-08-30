package com.lavyoung.marketforge.domain.demo.repository;

import com.lavyoung.marketforge.domain.demo.model.entity.DemoEntity;

/**
 * 演示聚合的领域仓储契约。
 *
 * @author lavyoung
 * @email lavyoung1325@outlook.com
 * @version 1.0.0-SNAPSHOT
 */
public interface IDemoRepository {

    DemoEntity findById(Long id);

    void save(DemoEntity entity);

}
