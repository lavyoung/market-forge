package com.lavyoung.marketforge.domain.activity.service.rule.factory;

import com.lavyoung.marketforge.domain.activity.service.rule.IActionChain;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 *
 * 默认工厂
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/20
 */
@Component
public class DefaultActivityChainFactory {

    private final IActionChain actionChain;

    public DefaultActivityChainFactory(Map<String, IActionChain> actionChainMap) {
        this.actionChain = actionChainMap.get(ActionModel.ACTIVITY_BASE_ACTION.code);
        this.actionChain.appendNext(actionChainMap.get(ActionModel.ACTIVITY_SKU_STOCK_ACTION.code));
    }

    public IActionChain openActionChain() {
        return actionChain;
    }


    @Getter
    @AllArgsConstructor
    public enum ActionModel {
        ACTIVITY_BASE_ACTION("activity_base_action", "基本信息校验"),
        ACTIVITY_SKU_STOCK_ACTION("activity_sku_stock_action", "sku库存校验"),
        ;

        private final String code;
        private final String desc;
    }
}
