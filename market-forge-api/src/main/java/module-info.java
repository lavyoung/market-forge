/**
 * Market Forge 远程 API 契约模块。
 * <p>
 * 仅导出稳定的接口、请求及响应模型，避免调用方依赖内部实现。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/09
 */
module market.forge.api {
    requires transitive jakarta.validation;
    requires transitive market.forge.types;
    requires transitive spring.web;

    exports com.lavyoung.marketforge.api.strategy.request;
    exports com.lavyoung.marketforge.api.strategy.response;
    exports com.lavyoung.marketforge.api.strategy;

    opens com.lavyoung.marketforge.api.strategy.request;
    opens com.lavyoung.marketforge.api.strategy.response;
}
