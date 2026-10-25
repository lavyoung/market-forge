package com.lavyoung.marketforge.domain.strategy.annotation;

import com.lavyoung.marketforge.types.domain.strategy.RuleModel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 旧版规则过滤器注册注解。
 * <p>
 * 该注解只服务于旧版规则过滤器工厂的自动注册机制。当前抽奖前置规则由责任链装配，
 * 奖品命中后的规则由规则树装配，新规则不应继续使用该注解。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/04
 * @deprecated 新规则应根据执行阶段实现责任链节点或规则树节点
 */
@Deprecated(since = "1.0.0", forRemoval = false)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogicStrategy {

    /**
     * 获取当前过滤器处理的规则模型。
     *
     * @return 规则模型
     */
    RuleModel logicModel();
}
