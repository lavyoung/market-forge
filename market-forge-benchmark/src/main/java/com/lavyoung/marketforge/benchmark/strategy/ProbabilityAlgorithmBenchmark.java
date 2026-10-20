package com.lavyoung.marketforge.benchmark.strategy;

import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.AliasProbabilityTableFactory;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.IProbabilityAlgorithm;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.impl.AliasMethodProbabilityAlgorithm;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.impl.BinarySearchProbabilityAlgorithm;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.impl.DirectLookupProbabilityAlgorithm;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.AwardProbabilityWeight;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.StrategyProbabilitySpace;
import org.openjdk.jmh.annotations.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 直接查表、累计区间二分与 Alias Method 的 JMH 基准。
 *
 * <p>奖品统一使用权重 {@code 1}，使奖品数量同时等于直接查找表长度。该模型
 * 便于观察候选数量增长时，三种算法在装配成本、空间分配和派发延迟上的变化。</p>
 *
 * <p>基准只使用内存仓储，不包含 Redis 网络和序列化成本。生产环境的最终
 * 算法阈值还需要结合端到端压测结果确定。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@Warmup(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(2)
@Threads(1)
public class ProbabilityAlgorithmBenchmark {

    /**
     * 测量直接查找表的单次奖品派发延迟。
     *
     * @param state 已完成三种算法装配的线程隔离状态
     * @return 随机命中的奖品标识
     */
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public long directLookupDispatch(BenchmarkState state) {
        return state.directLookupAlgorithm.dispatch(BenchmarkState.DIRECT_LOOKUP_KEY);
    }

    /**
     * 测量累计概率区间二分查找的单次奖品派发延迟。
     *
     * @param state 已完成三种算法装配的线程隔离状态
     * @return 随机命中的奖品标识
     */
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public long binarySearchDispatch(BenchmarkState state) {
        return state.binarySearchAlgorithm.dispatch(BenchmarkState.BINARY_SEARCH_KEY);
    }

    /**
     * 测量 Alias Method 的单次奖品派发延迟。
     *
     * @param state 已完成三种算法装配的线程隔离状态
     * @return 随机命中的奖品标识
     */
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public long aliasMethodDispatch(BenchmarkState state) {
        return state.aliasMethodAlgorithm.dispatch(BenchmarkState.ALIAS_METHOD_KEY);
    }

    /**
     * 测量直接查找表的完整装配成本。
     *
     * @param state 包含当前奖品规模和内存仓储的线程隔离状态
     * @return 本次装配保存的查找表元素数量
     */
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public int directLookupAssembly(BenchmarkState state) {
        state.directLookupAlgorithm.assemble(state.directLookupProbabilitySpace);
        return state.directLookupRepository.storedElementCount(BenchmarkState.DIRECT_LOOKUP_KEY);
    }

    /**
     * 测量累计概率区间的完整装配成本。
     *
     * @param state 包含当前奖品规模和内存仓储的线程隔离状态
     * @return 本次装配保存的累计概率区间数量
     */
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public int binarySearchAssembly(BenchmarkState state) {
        state.binarySearchAlgorithm.assemble(state.binarySearchProbabilitySpace);
        return state.binarySearchRepository.storedElementCount(BenchmarkState.BINARY_SEARCH_KEY);
    }

    /**
     * 测量 Alias Method 概率表的完整装配成本。
     *
     * @param state 包含当前奖品规模和内存仓储的线程隔离状态
     * @return 本次装配保存的 Alias 概率表列数
     */
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public int aliasMethodAssembly(BenchmarkState state) {
        state.aliasMethodAlgorithm.assemble(state.aliasMethodProbabilitySpace);
        return state.aliasMethodRepository.storedElementCount(BenchmarkState.ALIAS_METHOD_KEY);
    }

    /**
     * 每个 JMH 工作线程独享的算法状态。
     *
     * <p>{@link Level#Trial} 初始化不计入基准时间。派发测试复用预先装配的数据，
     * 装配测试则在每次基准调用中覆盖对应内存仓储数据。</p>
     */
    @State(Scope.Thread)
    public static class BenchmarkState {

        private static final String DIRECT_LOOKUP_KEY = "direct_lookup";
        private static final String BINARY_SEARCH_KEY = "binary_search";
        private static final String ALIAS_METHOD_KEY = "alias_method";

        /**
         * 当前概率空间包含的奖品数量。
         */
        @Param({"10", "100", "1000", "10000", "100000"})
        public int awardCount;

        private InMemoryStrategyRepository directLookupRepository;
        private InMemoryStrategyRepository binarySearchRepository;
        private InMemoryStrategyRepository aliasMethodRepository;
        private IProbabilityAlgorithm directLookupAlgorithm;
        private IProbabilityAlgorithm binarySearchAlgorithm;
        private IProbabilityAlgorithm aliasMethodAlgorithm;
        private StrategyProbabilitySpace directLookupProbabilitySpace;
        private StrategyProbabilitySpace binarySearchProbabilitySpace;
        private StrategyProbabilitySpace aliasMethodProbabilitySpace;

        /**
         * 创建等权奖品空间并预先装配派发基准需要的数据。
         */
        @Setup(Level.Trial)
        public void setUp() {
            directLookupRepository = new InMemoryStrategyRepository();
            binarySearchRepository = new InMemoryStrategyRepository();
            aliasMethodRepository = new InMemoryStrategyRepository();
            directLookupAlgorithm = new DirectLookupProbabilityAlgorithm(directLookupRepository, Integer.MAX_VALUE);
            binarySearchAlgorithm = new BinarySearchProbabilityAlgorithm(binarySearchRepository);
            aliasMethodAlgorithm = new AliasMethodProbabilityAlgorithm(aliasMethodRepository, new AliasProbabilityTableFactory(), 1);
            directLookupProbabilitySpace = createProbabilitySpace(DIRECT_LOOKUP_KEY, awardCount);
            binarySearchProbabilitySpace = createProbabilitySpace(BINARY_SEARCH_KEY, awardCount);
            aliasMethodProbabilitySpace = createProbabilitySpace(ALIAS_METHOD_KEY, awardCount);
            directLookupAlgorithm.assemble(directLookupProbabilitySpace);
            binarySearchAlgorithm.assemble(binarySearchProbabilitySpace);
            aliasMethodAlgorithm.assemble(aliasMethodProbabilitySpace);
        }

        private StrategyProbabilitySpace createProbabilitySpace(String assembleKey, int count) {
            List<AwardProbabilityWeight> awardWeights = new ArrayList<>(count);
            for (int index = 0; index < count; index++) {
                awardWeights.add(new AwardProbabilityWeight(index + 1L, 1));
            }
            return new StrategyProbabilitySpace(assembleKey, count, awardWeights);
        }
    }
}
