package com.lavyoung.marketforge.domain.award.service.impl;

import com.lavyoung.marketforge.domain.award.model.vo.AwardUnlockCountQueryVO;
import com.lavyoung.marketforge.domain.award.model.vo.AwardUnlockResultVO;
import com.lavyoung.marketforge.domain.award.repository.IAwardRepository;
import com.lavyoung.marketforge.domain.award.service.IAwardUnlockService;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.CommonResponseCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 奖品解锁领域服务默认实现。
 *
 * <p>该服务通过奖品仓储查询用户已经完成的抽奖次数，
 * 再由解锁结果值对象统一计算解锁状态和剩余次数。</p>
 *
 * <p>该类只负责奖品解锁规则，不承担接口参数转换、
 * 活动查询或前端响应对象组装。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/17
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AwardUnlockServiceImpl implements IAwardUnlockService {

    private final IAwardRepository awardRepository;

    @Override
    public AwardUnlockResultVO evaluateUnlock(AwardUnlockCountQueryVO queryVO, int requiredCount) {
        return evaluateUnlocks(queryVO, Set.of(requiredCount)).get(requiredCount);
    }

    @Override
    public Map<Integer, AwardUnlockResultVO> evaluateUnlocks(AwardUnlockCountQueryVO queryVO, Set<Integer> requiredCounts) {
        Objects.requireNonNull(queryVO, "queryVO must not be null");
        Objects.requireNonNull(requiredCounts, "requiredCounts must not be null");

        if (requiredCounts.isEmpty()) {
            return Map.of();
        }

        for (Integer requiredCount : requiredCounts) {
            if (requiredCount == null || requiredCount < 0) {
                throw BusinessException.of(CommonResponseCode.PARAM_INVALID, requiredCount);
            }
        }

        long completedCount = awardRepository.countCompletedRaffles(queryVO);
        if (completedCount < 0) {
            throw BusinessException.of(CommonResponseCode.SYSTEM_ERROR, queryVO.activityId(), queryVO.userId());
        }

        return requiredCounts.stream()
                .collect(Collectors.toUnmodifiableMap(
                        Function.identity(),
                        requiredCount -> AwardUnlockResultVO.evaluate(
                                requiredCount,
                                completedCount
                        )
                ));
    }
}
