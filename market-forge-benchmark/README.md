# 抽奖算法性能基准

该模块使用 JMH 测量概率算法的装配成本和派发延迟，不启动 Spring，也不连接 Redis 或 MySQL。

首次完整运行的环境、数据和分析记录在 [BASELINE.md](BASELINE.md)。

## 构建

在项目根目录执行：

```powershell
mvn -pl market-forge-benchmark -am clean package -DskipTests
```

构建产物为：

```text
market-forge-benchmark/target/benchmarks.jar
```

## 快速冒烟

只使用 10 个奖品、1 次预热、1 次测量和 1 个 fork，适合验证基准是否可以运行：

```powershell
java -jar market-forge-benchmark/target/benchmarks.jar ProbabilityAlgorithmBenchmark -p awardCount=10 -wi 1 -i 1 -f 1 -w 200ms -r 200ms
```

## 完整基准

执行类中声明的全部奖品规模，并采集 GC 和内存分配信息：

```powershell
java -jar market-forge-benchmark/target/benchmarks.jar ProbabilityAlgorithmBenchmark -prof gc -rf json -rff market-forge-benchmark/target/probability-algorithm-with-alias-result.json
```

只比较派发延迟：

```powershell
java -jar market-forge-benchmark/target/benchmarks.jar ".*Dispatch" -prof gc
```

只比较装配成本：

```powershell
java -jar market-forge-benchmark/target/benchmarks.jar ".*Assembly" -prof gc
```

## 指标解释

- 派发基准单位为 `ns/op`，数值越低越好。
- 装配基准单位为 `ms/op`，数值越低越好。
- `gc.alloc.rate.norm` 表示每次操作分配的字节数，可用于判断空间成本。
- 当前结果只反映内存数据结构性能，不包含 Redis 网络和序列化成本。
- Alias Method 需要两次随机决策，必须同时比较派发延迟和装配空间，不能只看 O (1) 复杂度。
- 算法选择阈值应以同一台机器、相同 JVM 参数下的多次完整结果为依据。

基准运行期间应关闭占用大量 CPU 的应用，并避免在 IntelliJ IDEA 内直接运行完整基准。
