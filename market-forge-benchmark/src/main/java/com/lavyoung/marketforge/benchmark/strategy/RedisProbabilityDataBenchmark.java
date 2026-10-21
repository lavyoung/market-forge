package com.lavyoung.marketforge.benchmark.strategy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.AliasProbabilityTable;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.AwardProbabilityRange;
import org.openjdk.jmh.annotations.*;
import org.redisson.Redisson;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.codec.TypedJsonJacksonCodec;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis 网络与概率快照数据反序列化的端到端 JMH 基准。
 *
 * <p>该基准不测算法计算本身，而是测量生产路径中无法通过纯内存基准覆盖的
 * Redis 往返、数据传输和 JSON 反序列化成本。字符串读取提供网络往返下界，
 * 另外三个用例分别读取直接查找表、累计概率区间和 Alias 概率表。</p>
 *
 * <p>Redis 连接参数只从环境变量读取。基准数据使用随机命名空间，并在每个
 * Trial 结束后删除，不会覆盖业务 Redis Key。此基准应连接专用压测 Redis，
 * 不得对生产实例执行。</p>
 *
 * <p>这里使用带明确目标类型的 {@link TypedJsonJacksonCodec}，用于稳定测量
 * 数据结构的 JSON 成本。它不承诺与应用自定义 codec 产生完全相同的字节数，
 * 因而结果用于判断数量级和变化趋势，不作为生产容量承诺。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@Threads(1)
public class RedisProbabilityDataBenchmark {

    /**
     * 测量只包含短字符串时的 Redis 单次读取，作为网络往返与协议处理下界。
     *
     * @param state 已连接 Redis 并写入隔离测试数据的基准状态
     * @return 读取到的字符串长度
     */
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public int redisStringRead(RedisState state) {
        return Objects.requireNonNull(state.stringBucket.get(), "benchmark string must exist").length();
    }

    /**
     * 测量完整直接查找表的 Redis 读取与 JSON 反序列化成本。
     *
     * @param state 已连接 Redis 并写入隔离测试数据的基准状态
     * @return 反序列化后的查找表元素数量
     */
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public int redisDirectLookupTableRead(RedisState state) {
        return Objects.requireNonNull(state.directLookupBucket.get(), "direct lookup table must exist").size();
    }

    /**
     * 测量累计概率区间的 Redis 读取与 JSON 反序列化成本。
     *
     * @param state 已连接 Redis 并写入隔离测试数据的基准状态
     * @return 反序列化后的累计概率区间数量
     */
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public int redisProbabilityRangesRead(RedisState state) {
        return Objects.requireNonNull(state.probabilityRangesBucket.get(), "probability ranges must exist").size();
    }

    /**
     * 测量 Alias 概率表的 Redis 读取与 JSON 反序列化成本。
     *
     * @param state 已连接 Redis 并写入隔离测试数据的基准状态
     * @return 反序列化后的 Alias 概率表列数
     */
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public int redisAliasTableRead(RedisState state) {
        return Objects.requireNonNull(state.aliasTableBucket.get(), "alias table must exist").size();
    }

    /**
     * 每个 Trial 共享的 Redis 连接和隔离概率数据。
     */
    @State(Scope.Benchmark)
    public static class RedisState {

        private static final String HOST_ENV = "MARKET_FORGE_BENCHMARK_REDIS_HOST";
        private static final String PORT_ENV = "MARKET_FORGE_BENCHMARK_REDIS_PORT";
        private static final String PASSWORD_ENV = "MARKET_FORGE_BENCHMARK_REDIS_PASSWORD";
        private static final String DATABASE_ENV = "MARKET_FORGE_BENCHMARK_REDIS_DATABASE";

        /**
         * 当前概率数据包含的奖品数量。
         */
        @Param({"10", "1000", "100000"})
        public int awardCount;

        private RedissonClient redissonClient;
        private String[] benchmarkKeys;
        private RBucket<String> stringBucket;
        private RBucket<Map<Integer, Long>> directLookupBucket;
        private RBucket<List<AwardProbabilityRange>> probabilityRangesBucket;
        private RBucket<AliasProbabilityTable> aliasTableBucket;

        /**
         * 创建 Redisson 客户端，并写入三种结构等价、数量一致的概率数据。
         *
         * <p>每个奖品权重均为 {@code 1}。直接查找表按下标保存奖品标识，
         * 二分结构保存连续累计上界，Alias 表的接受概率全部为 {@code 1}。
         * 随机命名空间保证重复运行和并行开发不会相互覆盖。</p>
         *
         * @throws IllegalArgumentException 端口、数据库编号或奖品数量不合法时抛出
         * @throws IllegalStateException    Redis 连接不可用或基准数据写入失败时抛出
         */
        @Setup(Level.Trial)
        public void setUp() {
            String host = environment(HOST_ENV, "127.0.0.1");
            int port = positiveEnvironmentInt(PORT_ENV, 6379);
            int database = nonNegativeEnvironmentInt(DATABASE_ENV, 0);
            String password = System.getenv(PASSWORD_ENV);

            Config config = new Config();
            SingleServerConfig server = config.useSingleServer()
                    .setAddress("redis://" + host + ":" + port)
                    .setDatabase(database)
                    .setConnectionMinimumIdleSize(1)
                    .setConnectionPoolSize(4)
                    .setConnectTimeout(3000)
                    .setTimeout(3000)
                    .setRetryAttempts(1);
            if (password != null && !password.isBlank()) {
                server.setPassword(password);
            }

            redissonClient = Redisson.create(config);
            String keyPrefix = "benchmark:probability:" + UUID.randomUUID() + ":" + awardCount + ":";
            benchmarkKeys = new String[]{
                    keyPrefix + "string",
                    keyPrefix + "direct",
                    keyPrefix + "binary",
                    keyPrefix + "alias"
            };

            stringBucket = redissonClient.getBucket(benchmarkKeys[0], StringCodec.INSTANCE);
            directLookupBucket = redissonClient.getBucket(benchmarkKeys[1],
                    new TypedJsonJacksonCodec(new TypeReference<Map<Integer, Long>>() {
                    }));
            probabilityRangesBucket = redissonClient.getBucket(benchmarkKeys[2],
                    new TypedJsonJacksonCodec(new TypeReference<List<AwardProbabilityRange>>() {
                    }));
            aliasTableBucket = redissonClient.getBucket(benchmarkKeys[3],
                    new TypedJsonJacksonCodec(AliasProbabilityTable.class));

            stringBucket.set("ready");
            directLookupBucket.set(createDirectLookupTable());
            probabilityRangesBucket.set(createProbabilityRanges());
            aliasTableBucket.set(createAliasTable());
        }

        /**
         * 删除本轮隔离数据并关闭 Redis 连接，避免 JMH fork 结束后遗留连接和 Key。
         */
        @TearDown(Level.Trial)
        public void tearDown() {
            if (redissonClient == null) {
                return;
            }
            if (benchmarkKeys != null) {
                redissonClient.getKeys().delete(benchmarkKeys);
            }
            redissonClient.shutdown();
        }

        private Map<Integer, Long> createDirectLookupTable() {
            Map<Integer, Long> table = new LinkedHashMap<>(awardCount);
            for (int index = 0; index < awardCount; index++) {
                table.put(index, index + 1L);
            }
            return table;
        }

        private List<AwardProbabilityRange> createProbabilityRanges() {
            List<AwardProbabilityRange> ranges = new ArrayList<>(awardCount);
            for (int index = 0; index < awardCount; index++) {
                ranges.add(new AwardProbabilityRange(index + 1, index + 1L));
            }
            return ranges;
        }

        private AliasProbabilityTable createAliasTable() {
            List<Long> awardIds = new ArrayList<>(awardCount);
            double[] probabilities = new double[awardCount];
            int[] aliases = new int[awardCount];
            for (int index = 0; index < awardCount; index++) {
                awardIds.add(index + 1L);
                probabilities[index] = 1D;
                aliases[index] = index;
            }
            return new AliasProbabilityTable(awardIds, probabilities, aliases);
        }

        private String environment(String name, String defaultValue) {
            String value = System.getenv(name);
            return value == null || value.isBlank() ? defaultValue : value;
        }

        private int positiveEnvironmentInt(String name, int defaultValue) {
            int value = Integer.parseInt(environment(name, String.valueOf(defaultValue)));
            if (value <= 0) {
                throw new IllegalArgumentException(name + " must be greater than zero");
            }
            return value;
        }

        private int nonNegativeEnvironmentInt(String name, int defaultValue) {
            int value = Integer.parseInt(environment(name, String.valueOf(defaultValue)));
            if (value < 0) {
                throw new IllegalArgumentException(name + " must not be negative");
            }
            return value;
        }
    }
}
