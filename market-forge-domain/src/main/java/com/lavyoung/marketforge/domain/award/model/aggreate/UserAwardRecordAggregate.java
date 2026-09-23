package com.lavyoung.marketforge.domain.award.model.aggreate;

import com.lavyoung.marketforge.domain.award.model.entity.TaskEntity;
import com.lavyoung.marketforge.domain.award.model.entity.UserAwardRecordEntity;
import lombok.Builder;

/**
 *
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/23
 */
@Builder
public record UserAwardRecordAggregate(
        UserAwardRecordEntity userAwardRecordEntity,
        // 消息
        TaskEntity taskEntity
) {
}
