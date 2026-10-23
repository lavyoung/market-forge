package com.lavyoung.marketforge.application.award.grant;

import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 奖品发放处理器注册表。
 *
 * <p>注册表在 Spring 容器启动时收集全部处理器，并按奖品类型建立不可变索引。
 * 同一类型存在多个处理器时立即阻止应用启动，避免运行时随机选择实现。</p>
 *
 * <p>应用服务只依赖本注册表，不直接持有积分、券码或实物处理器，
 * 从而遵循开闭原则：增加奖品类型时新增实现即可。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/23
 */
@Component
public class AwardGrantHandlerRegistry {

    private final Map<String, AwardGrantHandler> handlers;

    /**
     * 收集并校验 Spring 容器中的发奖处理器。
     *
     * @param handlerList Spring 容器发现的全部发奖处理器
     * @throws NullPointerException  处理器集合或其中的处理器为空时抛出
     * @throws IllegalStateException 处理器类型为空白或同一类型重复注册时抛出
     */
    public AwardGrantHandlerRegistry(List<AwardGrantHandler> handlerList) {
        Objects.requireNonNull(handlerList, "handlerList must not be null");
        Map<String, AwardGrantHandler> registeredHandlers = new HashMap<>();

        for (AwardGrantHandler handler : handlerList) {
            Objects.requireNonNull(handler, "award grant handler must not be null");

            String awardType = handler.awardType();
            if (awardType == null || awardType.isBlank()) {
                throw new IllegalStateException("award grant handler type must not be blank");
            }
            AwardGrantHandler previous = registeredHandlers.putIfAbsent(awardType, handler);
            if (previous != null) {
                throw new IllegalStateException("duplicate award grant handler type: " + awardType);
            }
        }
        this.handlers = Map.copyOf(registeredHandlers);
    }

    /**
     * 获取指定奖品类型的处理器。
     *
     * @param awardType 奖品类型编码
     * @return 对应的发奖处理器
     * @throws BusinessException 奖品类型为空或没有注册对应处理器时抛出
     */
    public AwardGrantHandler getRequired(String awardType) {
        if (awardType == null || awardType.isBlank()) {
            throw BusinessException.of(BusinessResponseCode.AWARD_GRANT_NOT_SUPPORTED, awardType);
        }
        AwardGrantHandler awardGrantHandler = handlers.get(awardType);
        if (awardGrantHandler == null) {
            throw BusinessException.of(BusinessResponseCode.AWARD_GRANT_NOT_SUPPORTED, awardType);
        }
        return awardGrantHandler;
    }

    /**
     * 判断应用是否具备指定类型的真实履约能力。
     *
     * <p>该方法后续用于奖品启用校验，防止运营人员启用系统尚未实现的奖品类型。</p>
     *
     * @param awardType 奖品类型编码
     * @return 已注册对应处理器时返回 {@code true}
     */
    public boolean supports(String awardType) {
        return awardType != null && !awardType.isBlank() && handlers.containsKey(awardType);
    }

    /**
     * 校验奖品类型和领取模式是否具备真实履约能力。
     *
     * <p>该方法用于奖品启用前检查。先确认奖品类型存在处理器，再确认处理器
     * 支持对应领取模式，避免不可履约的奖品进入抽奖策略。</p>
     *
     * @param awardType 奖品类型编码
     * @param claimMode 领取模式编码
     * @throws BusinessException 处理器不存在或不支持领取模式时抛出
     */
    public void validateCapability(String awardType, String claimMode) {
        AwardGrantHandler handler = getRequired(awardType);
        if (claimMode == null || claimMode.isBlank() || !handler.supportsClaimMode(claimMode)) {
            throw BusinessException.of(BusinessResponseCode.AWARD_GRANT_NOT_SUPPORTED, awardType, claimMode);
        }
    }
}
