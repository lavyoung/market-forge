package com.lavyoung.marketforge.application.strategy.model;

import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyDefinitionStateVO;
import com.lavyoung.marketforge.types.model.PageRequest;

import java.util.Objects;

/**
 * 策略定义运营分页查询条件。
 *
 * <p>该模型属于 Application 层，不包含 HTTP 注解、MyBatis 参数或持久化对象。
 * 策略标识用于精确查询，策略描述采用前缀匹配，状态用于筛选指定生命周期的策略。</p>
 *
 * @param strategyId   策略业务标识；为空时不限制
 * @param strategyDesc 策略描述前缀；为空时不限制
 * @param state        生命周期状态；为空时不限制
 * @param page         分页参数
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
public record StrategyDefinitionSearchQuery(Long strategyId, String strategyDesc, String state, PageRequest page) {

    /**
     * 规范化查询条件并校验生命周期状态。
     *
     * <p>空白字符串被转换为空，避免 SQL 将空字符串解释成有效筛选条件。
     * 状态必须来自策略生命周期枚举，防止非 HTTP 调用方绕过接口参数校验。</p>
     *
     * @throws NullPointerException     分页参数为空时抛出
     * @throws IllegalArgumentException 生命周期状态未知时抛出
     */
    public StrategyDefinitionSearchQuery {
        strategyDesc = normalize(strategyDesc);
        state = normalize(state);
        Objects.requireNonNull(page, "page must not be null");
        if (state != null && StrategyDefinitionStateVO.of(state).isEmpty()) {
            throw new IllegalArgumentException("unknown strategy definition state: " + state);
        }
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
