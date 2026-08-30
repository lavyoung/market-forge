package com.lavyoung.marketforge.types.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 聚合根的基础抽象类型，负责暂存聚合产生的领域事件。
 *
 * @param <ID> 聚合根标识类型
 * @author lavyoung
 * @email lavyoung1325@outlook.com
 * @version 1.0.0-SNAPSHOT
 */
public abstract class AggregateRoot<ID> {

    /** 当前尚未发布的领域事件。 */
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    /**
     * 获取聚合根标识。
     *
     * @return 聚合根标识
     */
    public abstract ID getId();

    /**
     * 注册由当前聚合产生的领域事件。
     *
     * @param event 待发布的领域事件
     */
    protected void registerEvent(DomainEvent event) {
        domainEvents.add(event);
    }

    /**
     * 获取当前尚未发布的领域事件只读视图。
     *
     * @return 不可修改的领域事件列表
     */
    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    /**
     * 清除已经发布的领域事件。
     */
    public void clearDomainEvents() {
        domainEvents.clear();
    }

}
