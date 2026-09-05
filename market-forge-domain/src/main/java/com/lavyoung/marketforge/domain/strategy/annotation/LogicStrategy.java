package com.lavyoung.marketforge.domain.strategy.annotation;

import com.lavyoung.marketforge.types.domain.strategy.RuleModel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标识规则过滤器所对应的规则模型。
 * <p>
 * 规则工厂在启动时读取该注解，并以规则模型为键注册过滤器实现。
 *
 * @author lavyoung
 * @version 1.0.0
 * @email lavyoung1325@outlook.com
 * @date 2026/09/04
 */
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
