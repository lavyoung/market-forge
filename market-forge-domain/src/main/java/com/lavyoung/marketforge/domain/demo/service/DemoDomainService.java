package com.lavyoung.marketforge.domain.demo.service;

import com.lavyoung.marketforge.domain.demo.model.entity.DemoEntity;
import com.lavyoung.marketforge.domain.demo.repository.IDemoRepository;

/**
 * 演示聚合的领域服务。
 *
 * @author lavyoung
 * @email lavyoung1325@outlook.com
 * @version 1.0.0-SNAPSHOT
 */
public class DemoDomainService {

    private final IDemoRepository demoRepository;

    public DemoDomainService(IDemoRepository demoRepository) {
        this.demoRepository = demoRepository;
    }

    public DemoEntity createDemo(String name) {

        DemoEntity entity = DemoEntity.create(name);

        demoRepository.save(entity);

        return entity;
    }

}
