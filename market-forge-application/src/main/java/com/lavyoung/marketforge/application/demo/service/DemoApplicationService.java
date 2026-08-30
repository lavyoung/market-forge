package com.lavyoung.marketforge.application.demo.service;

import com.lavyoung.marketforge.application.demo.command.CreateDemoCommand;
import com.lavyoung.marketforge.application.demo.result.DemoResult;
import com.lavyoung.marketforge.domain.demo.model.entity.DemoEntity;
import com.lavyoung.marketforge.domain.demo.repository.IDemoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 协调演示聚合用例的应用服务。
 *
 * @author lavyoung
 * @email lavyoung1325@outlook.com
 * @version 1.0.0-SNAPSHOT
 */
@Service
public class DemoApplicationService {

    private final IDemoRepository demoRepository;

    public DemoApplicationService(IDemoRepository demoRepository) {
        this.demoRepository = demoRepository;
    }

    @Transactional
    public DemoResult create(CreateDemoCommand command) {

        DemoEntity entity = DemoEntity.create(command.name());

        demoRepository.save(entity);

        return new DemoResult(
                entity.getId(),
                entity.getName()
        );
    }

}
