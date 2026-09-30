package com.lavyoung.marketforge.domain.award.service.impl;

import com.lavyoung.marketforge.domain.award.model.entity.UserAwardRecordEntity;
import com.lavyoung.marketforge.domain.award.model.valobj.AwardStateVO;
import com.lavyoung.marketforge.domain.award.repository.IAwardRepository;
import com.lavyoung.marketforge.domain.award.service.IAwardStateService;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 用户奖品状态领域服务默认实现。
 * <p>
 * 使用当前状态作为并发条件推进中奖记录，重复消息或并发请求不会重复执行状态副作用。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
public class AwardStateServiceImpl implements IAwardStateService {

    private final IAwardRepository awardRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public void prepareClaim(String userId, String orderId) {
        UserAwardRecordEntity record = queryRequiredRecord(userId, orderId);
        if (record.awardState() != AwardStateVO.CREATE) {
            return;
        }

        boolean updated = awardRepository.updateAwardState(
                userId, orderId, AwardStateVO.CREATE, AwardStateVO.WAIT_CLAIM);
        if (!updated && queryRequiredRecord(userId, orderId).awardState() == AwardStateVO.CREATE) {
            throw BusinessException.of(
                    BusinessResponseCode.USER_AWARD_RECORD_UPDATE_FAILED, userId, orderId);
        }
    }

    private UserAwardRecordEntity queryRequiredRecord(String userId, String orderId) {
        return awardRepository.queryUserAwardRecord(userId, orderId)
                .orElseThrow(() -> BusinessException.of(
                        BusinessResponseCode.USER_AWARD_RECORD_NOT_FOUND, userId, orderId));
    }
}
