package com.lavyoung.marketforge.domain.activity.service.quota.rule.factory;

import com.lavyoung.marketforge.domain.activity.service.quota.rule.IActionChain;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * 默认活动规则责任链工厂。
 * <p>
 * 按固定顺序装配活动基础校验和 SKU 库存校验节点，为额度订单创建流程提供统一规则入口。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/20
 */
@Component
public class DefaultActivityChainFactory {

    private final IActionChain actionChain;

    /**
     * 创建默认活动责任链工厂。
     * <p>
     * 从 Spring 容器注入的责任链节点映射中取出基础校验节点和 SKU 库存节点，并按固定顺序组装。
     * 构造阶段会校验必要节点是否存在，避免运行期才出现空指针。
     *
     * @param actionChainMap Spring 注入的责任链节点映射，键为 {@link ActionModel#code}
     * @throws NullPointerException 当责任链映射或必要节点缺失时抛出
     */
    public DefaultActivityChainFactory(Map<String, IActionChain> actionChainMap) {
        Objects.requireNonNull(actionChainMap, "actionChainMap must not be null.");
        this.actionChain = actionChainMap.get(ActionModel.ACTIVITY_BASE_ACTION.code);
        Objects.requireNonNull(this.actionChain, "activity_base_action must not be null.");
        IActionChain chain = actionChainMap.get(ActionModel.ACTIVITY_SKU_STOCK_ACTION.code);
        Objects.requireNonNull(chain, "activity_sku_stock_action must not be null.");
        this.actionChain.appendNext(chain);
    }

    /**
     * 打开活动规则责任链。
     *
     * @return 责任链头节点
     */
    public IActionChain openActionChain() {
        return actionChain;
    }


    /**
     * 活动规则责任链节点模型。
     */
    @Getter
    @AllArgsConstructor
    public enum ActionModel {

        /**
         * 活动基础信息校验节点。
         */
        ACTIVITY_BASE_ACTION("activity_base_action", "基本信息校验"),

        /**
         * 活动 SKU 库存校验节点。
         */
        ACTIVITY_SKU_STOCK_ACTION("activity_sku_stock_action", "sku库存校验"),
        ;

        /**
         * Spring Bean 名称。
         */
        private final String code;

        /**
         * 节点说明。
         */
        private final String desc;
    }
}
