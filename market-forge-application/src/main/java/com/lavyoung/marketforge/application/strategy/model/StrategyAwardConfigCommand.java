package com.lavyoung.marketforge.application.strategy.model;

import java.math.BigDecimal;

/**
 * 策略奖品配置命令项。
 *
 * <p>该模型作为创建和修改策略定义命令的子配置使用，不单独构成一个应用用例。
 * 策略标识由应用服务根据父命令统一写入，调用方不能为子配置指定其他策略。</p>
 *
 * <p>命令只接收库存总量，不接收剩余库存。创建新策略或复制历史策略时，
 * 剩余库存统一初始化为库存总量，避免运行时库存被运营请求覆盖。</p>
 *
 * @param awardId       奖品业务标识，必须引用已经启用的奖品定义
 * @param awardTitle    奖品在当前策略中的展示标题
 * @param awardSubtitle 奖品展示副标题；可以为空
 * @param awardCount    当前策略为该奖品配置的库存总量
 * @param awardRate     当前策略中该奖品的概率权重，必须大于零
 * @param ruleModels    奖品级规则模型编码；多个编码使用项目统一分隔符连接
 * @param sort          奖品展示和规则处理顺序
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
public record StrategyAwardConfigCommand(
        Long awardId,
        String awardTitle,
        String awardSubtitle,
        Integer awardCount,
        BigDecimal awardRate,
        String ruleModels,
        Integer sort
) {
}
