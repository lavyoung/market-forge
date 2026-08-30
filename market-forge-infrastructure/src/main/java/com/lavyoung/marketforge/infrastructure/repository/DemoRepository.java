package com.lavyoung.marketforge.infrastructure.repository;

import com.lavyoung.marketforge.domain.demo.model.entity.DemoEntity;
import com.lavyoung.marketforge.domain.demo.repository.IDemoRepository;
import com.lavyoung.marketforge.infrastructure.dao.DemoMapper;
import com.lavyoung.marketforge.infrastructure.po.DemoPO;
import org.springframework.stereotype.Repository;

/**
 * 演示领域仓储的基础设施实现。
 *
 * @author lavyoung
 * @email lavyoung1325@outlook.com
 * @version 1.0.0-SNAPSHOT
 */
@Repository
public class DemoRepository implements IDemoRepository {

    private final DemoMapper demoMapper;

    public DemoRepository(DemoMapper demoMapper) {
        this.demoMapper = demoMapper;
    }

    @Override
    public DemoEntity findById(Long id) {

        DemoPO po = demoMapper.selectById(id);

        if (po == null) {
            return null;
        }

        return new DemoEntity(
                po.getId(),
                po.getName()
        );
    }

    @Override
    public void save(DemoEntity entity) {

        DemoPO po = new DemoPO();

        po.setId(entity.getId());
        po.setName(entity.getName());

        demoMapper.insert(po);
    }

}
