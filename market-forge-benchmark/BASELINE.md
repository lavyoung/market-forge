# 概率算法性能基线

## 测试环境

- 执行日期：2026-10-20
- 操作系统：Windows 11
- 处理器：12th Gen Intel Core i7-12700，12 核 20 线程
- JVM：Oracle GraalVM 17.0.15
- JMH：1.37
- 工作线程：1
- Fork：2
- 预热：3 轮，每轮 500 ms
- 测量：5 轮，每轮 500 ms
- 奖品权重：全部为 1，因此奖品数量等于总权重

执行命令：

```powershell
java -jar market-forge-benchmark/target/benchmarks.jar ProbabilityAlgorithmBenchmark -prof gc -rf json -rff market-forge-benchmark/target/probability-algorithm-with-alias-result.json
```

Alias Method 合并随机采样后，只复测三种算法的派发延迟：

```powershell
java -jar market-forge-benchmark/target/benchmarks.jar ".*Dispatch" -prof gc -rf json -rff market-forge-benchmark/target/probability-dispatch-single-sample-result.json
```

Redis 端到端基准必须连接专用开发或压测实例，连接参数通过环境变量注入：

```powershell
$env:MARKET_FORGE_BENCHMARK_REDIS_HOST="127.0.0.1"
$env:MARKET_FORGE_BENCHMARK_REDIS_PORT="6379"
$env:MARKET_FORGE_BENCHMARK_REDIS_DATABASE="0"
$env:MARKET_FORGE_BENCHMARK_REDIS_PASSWORD="<redis-password>"

java -jar market-forge-benchmark/target/benchmarks.jar RedisProbabilityDataBenchmark -prof gc -rf json -rff market-forge-benchmark/target/redis-probability-data-result.json
```

该命令使用随机 Key 命名空间并在每轮 Trial 后清理数据。不得连接生产 Redis。

## 派发结果

数值为平均单次操作耗时，越低越好。

| 奖品数量 |     直接查表 |     二分查找 | Alias Method |
|---------:|-------------:|-------------:|-------------:|
|       10 | 264.40 ns/op | 287.64 ns/op | 582.77 ns/op |
|      100 | 270.83 ns/op | 304.39 ns/op | 530.65 ns/op |
|    1,000 | 269.46 ns/op | 337.81 ns/op | 521.68 ns/op |
|   10,000 | 301.76 ns/op | 353.21 ns/op | 532.17 ns/op |
|  100,000 | 339.74 ns/op | 421.70 ns/op | 570.83 ns/op |

本轮内存仓储基准中，Alias Method 用一个 `[0, n)` 随机样本的整数部分选列、小数部分判断接受率。
合并随机采样后，派发延迟从优化前的 777 至 857 ns/op 降至 522 至 583 ns/op，提升约 27% 至 35%。
单次分配量从 504 至 560 B/op 降至 288 至 336 B/op。奖品数量增长到 100,000 时延迟仍基本稳定，
说明 O (1) 特性已生效，但当前结果仍慢于直接查表和二分查找。

## 装配结果

| 奖品数量 |    直接查表耗时 |    直接查表分配 |   二分装配耗时 |   二分装配分配 | Alias 装配耗时 | Alias 装配分配 |
|---------:|----------------:|----------------:|---------------:|---------------:|---------------:|---------------:|
|       10 |  0.002465 ms/op |      1,800 B/op | 0.000047 ms/op |       432 B/op | 0.000112 ms/op |       728 B/op |
|      100 |  0.026855 ms/op |     18,979 B/op | 0.000366 ms/op |     3,672 B/op | 0.000961 ms/op |     5,408 B/op |
|    1,000 |  0.266572 ms/op |    223,048 B/op | 0.004106 ms/op |    36,088 B/op | 0.011187 ms/op |    73,160 B/op |
|   10,000 |  2.713987 ms/op |  2,260,397 B/op | 0.040678 ms/op |   360,088 B/op | 0.109915 ms/op |   757,176 B/op |
|  100,000 | 49.344715 ms/op | 23,043,681 B/op | 0.549161 ms/op | 3,600,089 B/op | 1.271672 ms/op | 7,597,209 B/op |

直接查表需要建立列表、执行安全随机打乱并转换为 HashMap，因此装配耗时和内存分配明显高于累计区间。
在 100,000 个奖品时，直接查表单次装配约分配 23 MB，约为二分区间的 6.4 倍。Alias 表分配约 7.6 MB，
明显低于直接展开表，但仍约为二分累计区间的 2.1 倍。

## 当前结论

1. 当前 `direct-lookup-max-size=10000` 以总权重而非奖品数量为判断依据，仍是合理的保守阈值。
2. 小型概率空间使用直接查表能够获得更稳定的低延迟派发。
3. 总权重超过 10,000 后使用二分算法，可显著降低装配内存、Redis 存储量和初始化成本。
4. Alias Method 合并随机采样的优化已被基准数据证明有效，但它仍未取得端到端优势。
5. `alias-min-award-count=1000` 只是课程实验值；在完成本地快照和 Redis 端到端压测前，不应直接作为生产阈值。
6. 三种生产实现每次都会从仓储读取完整的概率数据；如果奖品规模继续扩大，应增加带版本的本地不可变快照，避免 Redis
   网络和反序列化成本掩盖算法差异。
7. 本轮派发结果只包含安全随机数、内存仓储访问和算法查找成本，不能替代 Redis 端到端压测。

该基线用于观察趋势，不应直接作为不同机器、JDK 或生产环境的性能承诺。
