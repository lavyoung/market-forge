package com.lavyoung.marketforge.domain.award.service;

import com.lavyoung.marketforge.domain.award.model.vo.AwardUnlockCountQueryVO;
import com.lavyoung.marketforge.domain.award.model.vo.AwardUnlockResultVO;
import com.lavyoung.marketforge.types.exception.BusinessException;

import java.util.Map;
import java.util.Set;

/**
 * 奖品解锁领域服务。
 *
 * <p>负责根据活动配置的统计范围查询用户已完成的抽奖次数，
 * 并统一计算奖品是否解锁以及剩余需要完成的次数。</p>
 *
 * <p>页面展示和抽奖规则校验必须复用该服务，避免不同业务入口
 * 对日次数和累计次数产生不同解释。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/17
 */
public interface IAwardUnlockService {

    /**
     * 判定用户是否满足指定奖品的解锁次数要求。
     *
     * @param queryVO       用户、活动、统计范围和统计日期组成的查询条件
     * @param requiredCount 奖品要求完成的抽奖次数
     * @return 包含要求次数、已完成次数、解锁状态和剩余次数的判定结果
     * @throws NullPointerException                                       查询条件为空时抛出
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 要求次数为负数，或者仓储返回非法统计结果时抛出
     */
    AwardUnlockResultVO evaluateUnlock(AwardUnlockCountQueryVO queryVO, int requiredCount);

    /**
     * 一次查询用户已完成次数，并批量计算多个奖品门槛的解锁状态。
     *
     * <p>该方法用于活动奖品列表。无论列表包含多少个奖品，
     * 都只允许执行一次已完成抽奖次数查询，避免产生 N+1 查询。</p>
     *
     * @param queryVO        用户、活动、统计范围和统计日期组成的查询条件
     * @param requiredCounts 奖品解锁门槛集合
     * @return 以解锁门槛为键的判定结果；门槛集合为空时返回空 Map
     * @throws NullPointerException 查询条件或门槛集合为空时抛出
     * @throws BusinessException    门槛非法或仓储返回非法次数时抛出
     */
    Map<Integer, AwardUnlockResultVO> evaluateUnlocks(AwardUnlockCountQueryVO queryVO, Set<Integer> requiredCounts);
}
