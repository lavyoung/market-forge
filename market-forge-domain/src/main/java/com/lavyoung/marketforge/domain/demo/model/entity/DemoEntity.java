package com.lavyoung.marketforge.domain.demo.model.entity;

import com.lavyoung.marketforge.types.exception.BusinessException;

/**
 * 演示聚合根实体。
 *
 * @author lavyoung
 * @email lavyoung1325@outlook.com
 * @version 1.0.0-SNAPSHOT
 */
public class DemoEntity {

    private Long id;

    private String name;

    public DemoEntity(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public static DemoEntity create(String name) {

        if (name == null || name.isBlank()) {
            throw new BusinessException(
                    "DEMO_001",
                    "Demo name cannot be blank"
            );
        }

        return new DemoEntity(null, name);
    }

    public void rename(String newName) {

        if (newName == null || newName.isBlank()) {
            throw new BusinessException(
                    "DEMO_002",
                    "Demo name cannot be blank"
            );
        }

        this.name = newName;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

}
