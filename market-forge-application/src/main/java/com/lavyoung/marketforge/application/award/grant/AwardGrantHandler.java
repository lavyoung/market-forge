package com.lavyoung.marketforge.application.award.grant;

import com.lavyoung.marketforge.application.award.model.GrantAwardCommand;

/**
 * 奖品发放处理器。
 *
 * <p>每个实现只负责一种 {@code awardType}。新增奖品类型时应新增处理器，
 * 不应修改通用发奖应用服务中的条件分支。</p>
 *
 * <p>处理器必须使用 {@link GrantAwardCommand#orderId()} 作为履约幂等业务号。
 * 如果下游返回重复处理且历史结果一致，应视为幂等成功。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/23
 */
public interface AwardGrantHandler {

    /**
     * 返回当前处理器支持的奖品类型。
     *
     * @return 稳定的奖品类型编码
     */
    String awardType();

    /**
     * 判断当前处理器是否支持指定领取模式。
     *
     * <p>自动发放和人工领取通常拥有不同的业务入口。奖品启用前必须确认
     * 处理器具备对应领取模式，避免中奖后找不到可执行流程。</p>
     *
     * @param claimMode 领取模式编码
     * @return 支持该领取模式时返回 {@code true}
     */
    boolean supportsClaimMode(String claimMode);

    /**
     * 执行一次幂等奖品发放。
     *
     * @param command 包含用户、订单和不可变履约快照的发奖命令
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 配置非法、渠道失败或履约结果不一致时抛出
     */
    void grant(GrantAwardCommand command);
}
