package com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm;

import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyAwardEntity;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyProbabilityIntervalVO;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyProbabilityPreviewVO;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.AwardProbabilityWeight;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.StrategyProbabilitySpace;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 策略概率空间预览工厂。
 *
 * <p>本工厂复用正式策略装配所使用的 {@link ProbabilitySpaceFactory}，
 * 将已经完成校验和整数化处理的概率空间转换为后台可展示的连续命中区间。
 * 这样可以保证配置预览与实际抽奖使用完全相同的概率换算规则。</p>
 *
 * <p>预览区间采用左闭右开形式。例如整数概率空间总权重为 100，
 * 某奖品权重为 30，且起点为 20，则该奖品对应区间为
 * {@code [20, 50)}。正式抽奖生成的随机值落入该区间时命中该奖品。</p>
 *
 * <p>本工厂只负责模型转换，不读取数据库、不写入 Redis，也不发布
 * 概率快照。奖品查询、配置完整性检查和快照发布分别由对应领域服务负责。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
@Component
@RequiredArgsConstructor
public final class StrategyProbabilityPreviewFactory {
    /**
     * 后台展示理论概率时保留的小数位数。
     *
     * <p>整数概率空间允许的总权重最大为 {@link Integer#MAX_VALUE}。
     * 保留 12 位小数可以避免最小权重为 1 时被舍入为零。</p>
     */
    private static final int PROBABILITY_SCALE = 12;

    private final ProbabilitySpaceFactory probabilitySpaceFactory;

    /**
     * 根据策略奖品配置生成连续概率区间预览。
     *
     * <p>处理流程如下：</p>
     *
     * <ol>
     *     <li>调用正式概率空间工厂校验奖品配置。</li>
     *     <li>将十进制中奖概率转换并约分为整数权重。</li>
     *     <li>按照概率空间中的奖品顺序依次计算左闭右开区间。</li>
     *     <li>使用奖品权重除以总权重，计算理论命中概率。</li>
     *     <li>返回覆盖 {@code [0, totalWeight)} 的完整概率预览。</li>
     * </ol>
     *
     * <p>本方法不会捕获概率空间工厂抛出的业务异常。配置非法时应由上层
     * 就绪检查器将异常转换成可展示的检查问题，而不是返回一个看似有效的预览。</p>
     *
     * @param assembleKey    策略装配键，可以是策略标识或“策略标识_权重档位”
     * @param strategyAwards 按正式装配顺序排列的策略奖品配置
     * @return 与正式抽奖概率空间一致的连续区间预览
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 奖品缺失、奖品重复、概率非法或者整数概率空间超出范围时抛出
     * @throws ArithmeticException                                        计算累计区间时发生整数溢出时抛出
     */
    public StrategyProbabilityPreviewVO create(String assembleKey, List<StrategyAwardEntity> strategyAwards) {
        StrategyProbabilitySpace probabilitySpace = probabilitySpaceFactory.create(assembleKey, strategyAwards);
        BigDecimal totalWeight = BigDecimal.valueOf(probabilitySpace.totalWeight());
        List<StrategyProbabilityIntervalVO> intervals = new ArrayList<>(probabilitySpace.awardWeights().size());
        int startInclusive = 0;
        for (AwardProbabilityWeight awardWeight : probabilitySpace.awardWeights()) {
            int endExclusive = Math.addExact(startInclusive, awardWeight.weight());
            BigDecimal probability = BigDecimal.valueOf(awardWeight.weight())
                    .divide(totalWeight, PROBABILITY_SCALE, RoundingMode.HALF_UP);
            intervals.add(new StrategyProbabilityIntervalVO(
                    awardWeight.awardId(),
                    awardWeight.weight(),
                    startInclusive,
                    endExclusive,
                    probability
            ));
            startInclusive = endExclusive;
        }
        return new StrategyProbabilityPreviewVO(true, probabilitySpace.totalWeight(), intervals);
    }
}
