package com.lavyoung.marketforge.domain.award.model.vo;

import java.util.Objects;

/**
 * 奖品发放配置快照。
 *
 * <p>快照保存中奖发生时确定的发奖参数和格式版本。即使运营人员随后停用奖品或创建新版本，
 * 已产生的中奖记录仍然使用原快照完成履约，避免发奖行为随配置变化。</p>
 *
 * <p>{@code content} 使用 JSON 保存，但本值对象只保证内容存在。
 * 积分数量、活动 SKU、供应商券模板等业务字段由对应的发奖处理器解析和校验。</p>
 *
 * @param content 奖品发放配置 JSON
 * @param version 配置结构版本，从 1 开始
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/21
 */
public record AwardConfigSnapshot(String content, int version) {

    /**
     * 创建并校验奖品配置快照。
     *
     * @throws NullPointerException     配置内容为空时抛出
     * @throws IllegalArgumentException 配置内容为空白或配置版本小于 1 时抛出
     */
    public AwardConfigSnapshot {
        Objects.requireNonNull(content, "award config content must not be null");
        if (content.isBlank()) {
            throw new IllegalArgumentException("award config content must not be blank");
        }
        if (version < 1) {
            throw new IllegalArgumentException("award config version must be greater than zero");
        }
    }
}
