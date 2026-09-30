package com.lavyoung.marketforge.domain.award.model.aggreate;

import com.lavyoung.marketforge.domain.award.event.SendAwardRecordEvent;
import com.lavyoung.marketforge.domain.award.model.entity.UserAwardRecordEntity;
import lombok.Builder;

/**
 * 用户中奖记录聚合。
 * <p>
 * 聚合一次中奖结果落库所需的中奖记录实体和待投递集成事件。仓储实现应保证两者
 * 在同一本地事务内持久化，避免中奖记录存在但可靠消息任务缺失。
 *
 * @param userAwardRecordEntity 用户中奖记录实体
 * @param sendAwardRecordEvent  用户中奖记录创建事件
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/23
 */
@Builder
public record UserAwardRecordAggregate(
        UserAwardRecordEntity userAwardRecordEntity,
        SendAwardRecordEvent sendAwardRecordEvent
) {
}
