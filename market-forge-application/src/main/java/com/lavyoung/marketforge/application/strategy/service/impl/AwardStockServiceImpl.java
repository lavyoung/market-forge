package com.lavyoung.marketforge.application.strategy.service.impl;

import com.lavyoung.marketforge.application.strategy.service.IAwardStockService;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyAwardStockKeyVO;
import com.lavyoung.marketforge.domain.strategy.service.IRaffleStock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 奖品库存异步同步用例的默认实现。
 * <p>
 * 限制单次批处理数量，并保证单条消息处理失败时重新入队。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AwardStockServiceImpl implements IAwardStockService {

    private static final int MAX_MESSAGES_PER_RUN = 100;

    /**
     * 提供库存消息读取、同步及重试能力的领域服务。
     */
    private final IRaffleStock raffleStock;

    /**
     * {@inheritDoc}
     */
    @Override
    public void synchronizePendingStock() {
        for (int index = 0; index < MAX_MESSAGES_PER_RUN; index++) {
            Optional<StrategyAwardStockKeyVO> message = raffleStock.pollQueueValue();
            if (message.isEmpty()) {
                return;
            }
            synchronize(message.get());
        }
    }

    /**
     * 同步单条库存消息，并在数据库调用异常时重新入队。
     *
     * @param message 待同步的库存消息
     */
    private void synchronize(StrategyAwardStockKeyVO message) {
        try {
            boolean updated = raffleStock.updateStrategyAwardStock(message.strategyId(), message.awardId());
            if (!updated) {
                log.warn("奖品库存同步未扣减数据库库存 strategyId={} awardId={}",
                        message.strategyId(), message.awardId());
                return;
            }
            log.info("奖品库存同步完成 strategyId={} awardId={}", message.strategyId(), message.awardId());
        } catch (RuntimeException exception) {
            log.error("奖品库存同步失败，消息将重新入队 strategyId={} awardId={}",
                    message.strategyId(), message.awardId(), exception);
            raffleStock.requeueStockUpdate(message);
        }
    }
}
