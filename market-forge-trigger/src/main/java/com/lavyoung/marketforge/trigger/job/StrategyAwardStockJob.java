package com.lavyoung.marketforge.trigger.job;

import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyAwardStockKeyVO;
import com.lavyoung.marketforge.domain.strategy.service.IRaffleStock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 奖品库存异步同步任务。
 * <p>
 * 周期性批量拉取已经到期的库存扣减消息并同步数据库。单次执行设置处理上限，
 * 避免持续增长的队列长期占用 Spring 调度线程；数据库异常时将消息重新延迟投递。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StrategyAwardStockJob {

    /**
     * 单次调度允许处理的最大消息数。
     */
    private static final int MAX_MESSAGES_PER_RUN = 100;

    /**
     * 提供库存消息读取、重试及数据库同步能力的领域服务。
     */
    private final IRaffleStock raffleStock;

    /**
     * 非阻塞地批量处理当前已经到期的库存消息。
     * <p>
     * 默认每秒执行一次，可通过 {@code market-forge.job.award-stock.fixed-delay-ms}
     * 覆盖调度间隔。单条消息处理失败时重新入队，不影响本批次其他消息。
     */
    @Scheduled(fixedDelayString = "${market-forge.job.award-stock.fixed-delay-ms:1000}")
    public void exec() {
        for (int index = 0; index < MAX_MESSAGES_PER_RUN; index++) {
            Optional<StrategyAwardStockKeyVO> message = raffleStock.pollQueueValue();
            if (message.isEmpty()) {
                return;
            }
            processMessage(message.get());
        }
    }

    /**
     * 同步单条库存消息，并在数据库调用异常时重新入队。
     *
     * @param message 待处理的库存扣减消息
     */
    private void processMessage(StrategyAwardStockKeyVO message) {
        try {
            boolean updated = raffleStock.updateStrategyAwardStock(message.strategyId(), message.awardId());
            if (!updated) {
                log.warn("奖品库存更新任务未扣减数据库库存 strategyId={} awardId={}",
                        message.strategyId(), message.awardId());
                return;
            }
            log.info("奖品库存更新任务完成 strategyId={} awardId={}",
                    message.strategyId(), message.awardId());
        } catch (RuntimeException exception) {
            log.error("奖品库存更新任务失败，消息将重新入队 strategyId={} awardId={}",
                    message.strategyId(), message.awardId(), exception);
            raffleStock.requeueStockUpdate(message);
        }
    }
}
