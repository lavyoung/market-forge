package com.lavyoung.marketforge.application.award.model;

import com.lavyoung.marketforge.types.model.PageRequest;

import java.util.Objects;

/**
 * 奖品定义运营分页查询条件。
 *
 * @param awardCode 奖品业务编码；为空时不限制
 * @param awardName 奖品名称前缀；为空时不限制
 * @param awardType 发奖处理器类型；为空时不限制
 * @param state     生命周期状态；为空时不限制
 * @param page      分页参数
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
public record AwardDefinitionSearchQuery(
        String awardCode,
        String awardName,
        String awardType,
        String state,
        PageRequest page
) {

    /**
     * 规范化可选过滤条件并校验分页参数。
     *
     * @throws NullPointerException 分页参数为空时抛出
     */
    public AwardDefinitionSearchQuery {
        awardCode = normalize(awardCode);
        awardName = normalize(awardName);
        awardType = normalize(awardType);
        state = normalize(state);
        Objects.requireNonNull(page, "page must not be null");
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}