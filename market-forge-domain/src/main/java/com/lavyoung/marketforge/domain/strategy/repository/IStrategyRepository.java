package com.lavyoung.marketforge.domain.strategy.repository;

import com.lavyoung.marketforge.domain.strategy.model.entity.AwardEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyAwardEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyRuleEntity;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyAwardRuleModelVO;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.ProbabilityAlgorithmType;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.AliasProbabilityTable;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.AwardProbabilityRange;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.ProbabilitySnapshotKey;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.ProbabilitySnapshotPointer;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 抽奖策略仓储端口。
 * <p>
 * 为策略领域服务提供策略、规则和奖品配置的读取能力，并负责持久化装配后的
 * 概率范围及奖品查找表。具体的数据来源和缓存策略由基础设施层实现。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
public interface IStrategyRepository {

    /**
     * 查询指定抽奖策略下参与抽奖的全部奖品配置。
     *
     * @param strategyId 策略标识
     * @return 策略奖品配置列表；不存在配置时返回空列表，不返回 {@code null}
     */
    List<StrategyAwardEntity> queryStrategyAwardList(Long strategyId);

    /**
     * 查询指定策略下的奖品配置。
     *
     * @param strategyId 策略标识
     * @param awardId    奖品标识
     * @return 对应奖品配置；不存在时返回空
     */
    Optional<StrategyAwardEntity> getStrategyAwardEntity(Long strategyId, Long awardId);

    /**
     * 保存装配完成的概率范围和奖品查找表。
     * <p>
     * {@code key} 可以是普通策略标识，也可以是包含权重规则值的策略装配键；
     * 查找表的键为从 {@code 0} 到 {@code rateRange - 1} 的概率下标，值为奖品标识。
     *
     * @param key                              策略装配键
     * @param rateRange                        随机取值范围，即查找表的有效下标数量
     * @param shuffleStrategyAwardSearchTables 概率下标到奖品标识的乱序映射
     */
    void storeStrategyAwardSearchTables(String key, Integer rateRange, Map<Integer, Long> shuffleStrategyAwardSearchTables);

    /**
     * 保存直接查表算法的版本化概率快照。
     *
     * @param snapshotKey               快照装配键和版本
     * @param rateRange                 随机取值范围
     * @param strategyAwardSearchTables 概率下标到奖品标识的映射
     * @throws NullPointerException 快照键为空时抛出
     */
    void storeStrategyAwardSearchTables(ProbabilitySnapshotKey snapshotKey, Integer rateRange, Map<Integer, Long> strategyAwardSearchTables);

    /**
     * 获取指定策略装配键对应的随机取值范围。
     * <p>
     * 装配键既可以是普通策略标识，也可以是“策略标识_权重档位”。
     *
     * @param key 策略装配键
     * @return 查找表的有效下标数量；策略尚未装配时返回 {@code 0}
     */
    int getRateRange(String key);

    /**
     * 获取直接查表或二分算法快照的随机取值范围。
     *
     * @param snapshotKey 快照装配键和版本
     * @return 随机取值范围；快照不存在时返回 {@code 0}
     * @throws NullPointerException 快照键为空时抛出
     */
    int getRateRange(ProbabilitySnapshotKey snapshotKey);

    /**
     * 从指定策略装配键对应的概率查找表中获取奖品标识。
     * <p>
     * 装配键既可以表示普通策略，也可以表示某个权重门槛下的策略。
     *
     * @param key     策略装配键
     * @param rateKey 概率查找表下标
     * @return 奖品标识；查找表中不存在对应下标时返回 {@code 0}
     */
    long getStrategyAwardAssemble(String key, int rateKey);

    /**
     * 获取直接查表快照中指定下标对应的奖品。
     *
     * @param snapshotKey 快照装配键和版本
     * @param rateKey     概率查找表下标
     * @return 奖品标识；快照不存在时返回 {@code 0}
     * @throws NullPointerException 快照键为空时抛出
     */
    long getStrategyAwardAssemble(ProbabilitySnapshotKey snapshotKey, int rateKey);

    /**
     * 保存二分算法使用的累计概率区间。
     * <p>
     * 区间必须按照累计上界升序排列，最后一个区间的累计上界必须等于
     * {@code totalWeight}。装配键可以表示普通策略或权重奖池。
     *
     * @param key               策略装配键
     * @param totalWeight       随机数上界
     * @param probabilityRanges 按累计上界升序排列的奖品概率区间
     */
    void storeStrategyAwardProbabilityRanges(String key, int totalWeight, List<AwardProbabilityRange> probabilityRanges);

    /**
     * 保存二分算法使用的版本化累计概率区间。
     *
     * @param snapshotKey       快照装配键和版本
     * @param totalWeight       随机取值上界
     * @param probabilityRanges 按累计上界升序排列的概率区间
     * @throws NullPointerException 快照键或概率区间为空时抛出
     */
    void storeStrategyAwardProbabilityRanges(ProbabilitySnapshotKey snapshotKey, int totalWeight, List<AwardProbabilityRange> probabilityRanges);

    /**
     * 获取二分算法使用的累计概率区间。
     *
     * @param key 策略装配键
     * @return 按累计上界升序排列的不可变区间列表；没有装配数据时返回空列表
     */
    List<AwardProbabilityRange> getStrategyAwardProbabilityRanges(String key);

    /**
     * 获取二分算法使用的版本化累计概率区间。
     *
     * @param snapshotKey 快照装配键和版本
     * @return 不可变概率区间列表；快照不存在时返回空列表
     * @throws NullPointerException 快照键为空时抛出
     */
    List<AwardProbabilityRange> getStrategyAwardProbabilityRanges(ProbabilitySnapshotKey snapshotKey);

    /**
     * 保存 Alias Method 使用的别名概率表。
     *
     * <p>概率表包含奖品列、原奖品接受概率和候补别名列。装配实现必须先完整
     * 保存概率表，再更新算法类型元数据，避免抽奖阶段读取到尚未写完的数据。</p>
     *
     * @param key        普通策略或权重奖池使用的策略装配键
     * @param aliasTable 已完成配平且通过结构校验的不可变别名概率表
     * @throws NullPointerException 别名概率表为空时抛出
     */
    void storeStrategyAwardAliasTable(String key, AliasProbabilityTable aliasTable);

    /**
     * 保存 Alias Method 使用的版本化概率表。
     *
     * @param snapshotKey 快照装配键和版本
     * @param aliasTable  完整且不可变的 Alias 概率表
     * @throws NullPointerException 快照键或概率表为空时抛出
     */
    void storeStrategyAwardAliasTable(ProbabilitySnapshotKey snapshotKey, AliasProbabilityTable aliasTable);

    /**
     * 查询 Alias Method 使用的别名概率表。
     *
     * <p>抽奖阶段必须读取装配阶段生成的完整概率快照，不能根据数据库权重临时
     * 重建概率表。这样可以保证一次抽奖只使用同一个版本的奖品、接受概率和
     * 别名下标，避免策略重装过程中出现概率数据混用。</p>
     *
     * <p>缓存不存在代表当前装配键尚未完成 Alias Method 装配。调用方应将其
     * 识别为策略未装配或装配数据不完整，而不是使用空概率表继续抽奖。</p>
     *
     * @param key 普通策略或权重奖池使用的策略装配键
     * @return 已装配的不可变别名概率表；缓存不存在时返回空
     */
    Optional<AliasProbabilityTable> getStrategyAwardAliasTable(String key);

    /**
     * 获取 Alias Method 使用的版本化概率表。
     *
     * @param snapshotKey 快照装配键和版本
     * @return 指定版本概率表；快照不存在时返回空
     * @throws NullPointerException 快照键为空时抛出
     */
    Optional<AliasProbabilityTable> getStrategyAwardAliasTable(ProbabilitySnapshotKey snapshotKey);

    /**
     * 保存策略装配使用的概率算法类型。
     *
     * <p>算法数据写入完成后再保存该元数据，使抽奖阶段能够选择与装配阶段
     * 相同的查询算法。</p>
     *
     * @param key           策略装配键
     * @param algorithmType 本次装配使用的算法类型
     * @throws NullPointerException 算法类型为空时抛出
     */
    void storeStrategyProbabilityAlgorithm(String key, ProbabilityAlgorithmType algorithmType);

    /**
     * 查询策略装配使用的概率算法类型。
     *
     * @param key 策略装配键
     * @return 已保存的算法类型；策略尚未装配或编码无法识别时返回空
     */
    Optional<ProbabilityAlgorithmType> getStrategyProbabilityAlgorithm(String key);

    /**
     * 为指定概率空间生成下一个快照版本。
     *
     * <p>版本由共享存储原子递增，同一装配键下单调增长，但不保证连续。
     * 装配中途失败时可以放弃已经申请的版本，不得回退版本计数器。</p>
     *
     * @param key 普通策略或权重奖池使用的装配键
     * @return 大于零的新快照版本
     * @throws IllegalArgumentException 装配键为空白时抛出
     */
    long nextProbabilitySnapshotVersion(String key);

    /**
     * 尝试发布新的生效概率快照指针。
     *
     * <p>只有待发布版本严格大于当前生效版本时才能发布成功。该约束用于阻止
     * 执行较慢的旧装配任务在新装配完成后覆盖新版本。</p>
     *
     * @param pointer 已完成概率数据保存、等待生效的快照指针
     * @return 发布成功返回 {@code true}；当前版本更新或相同时返回 {@code false}
     * @throws NullPointerException 快照指针为空时抛出
     */
    boolean publishProbabilitySnapshotPointer(ProbabilitySnapshotPointer pointer);

    /**
     * 查询指定概率空间当前生效的快照指针。
     *
     * @param key 普通策略或权重奖池使用的装配键
     * @return 当前生效指针；尚未发布任何版本时返回空
     * @throws IllegalArgumentException 装配键为空白时抛出
     */
    Optional<ProbabilitySnapshotPointer> getProbabilitySnapshotPointer(String key);

    /**
     * 将指定概率快照标记为退役状态。
     *
     * <p>退役不是立即删除。基础设施应为该快照包含的全部概率数据设置宽限期，
     * 使已经读取该快照指针的在途抽奖请求仍能完成查询。宽限期结束后，
     * Redis 自动回收对应数据。</p>
     *
     * <p>当前生效快照不得调用该方法。调用方只有在新指针发布成功、装配失败
     * 或候选版本发布被拒绝时才能退役相应快照。</p>
     *
     * @param pointer 等待回收的快照指针
     * @throws NullPointerException 快照指针为空时抛出
     */
    void retireProbabilitySnapshot(ProbabilitySnapshotPointer pointer);

    /**
     * 查询指定策略的基础配置。
     *
     * @param strategyId 策略标识
     * @return 策略实体；策略不存在时返回 {@code null}
     */
    StrategyEntity queryStrategyEntityByStrategyId(Long strategyId);

    /**
     * 查询指定策略下某一规则模型的规则配置。
     *
     * @param strategyId 策略标识
     * @param ruleModel  需要查询的规则模型
     * @return 策略规则实体；规则不存在时返回 {@code null}
     */
    StrategyRuleEntity getStrategyRule(Long strategyId, RuleModel ruleModel);

    /**
     * 查询策略级或奖品级规则的配置值。
     * <p>
     * 当 {@code awardId} 为空时查询策略级规则，否则同时按奖品标识过滤。
     *
     * @param strategyId 策略标识
     * @param awardId    奖品标识；查询策略级规则时可为空
     * @param ruleModel  规则模型编码
     * @return 规则配置值；未找到匹配规则时返回 {@code null}
     */
    String queryStrategyRuleValue(Long strategyId, Long awardId, String ruleModel);

    /**
     * 查询指定策略的策略级规则配置值。
     * <p>
     * 该方法不限定奖品标识，适用于黑名单、权重等策略级规则。
     *
     * @param strategyId 策略标识
     * @param ruleModel  规则模型编码
     * @return 规则配置值；未找到匹配规则时返回 {@code null}
     */
    String queryStrategyRuleValue(Long strategyId, String ruleModel);

    /**
     * 查询指定策略奖品配置的规则模型。
     *
     * @param strategyId 策略标识
     * @param awardId    奖品标识
     * @return 策略奖品规则模型值对象；未找到奖品配置时返回 {@code null}
     */
    StrategyAwardRuleModelVO queryStrategyAwardRuleModels(Long strategyId, long awardId);

    /**
     * 初始化策略奖品库存缓存。
     *
     * @param strategyId 策略标识
     * @param awardId    奖品标识
     * @param stock      初始库存
     */
    void cacheStrategyAwardStock(Long strategyId, Long awardId, int stock);

    /**
     * 扣减策略奖品缓存库存。
     *
     * @param strategyId  策略标识
     * @param awardId     奖品标识
     * @param stock       扣减数量，必须大于零
     * @param endDateTime 活动结束时间，用于计算库存锁租约；为空时使用默认短租约
     * @return 库存充足且扣减成功返回 {@code true}
     * @throws NullPointerException     当策略标识或奖品标识为空时抛出
     * @throws IllegalArgumentException 当扣减数量小于等于零时抛出
     */
    boolean subtractAwardStock(Long strategyId, Long awardId, int stock, LocalDateTime endDateTime);

    /**
     * 扣减策略奖品缓存库存。
     * <p>
     * 兼容不携带活动结束时间的调用，下游会使用默认短租约保护 Redis 锁。
     *
     * @param strategyId 策略标识
     * @param awardId    奖品标识
     * @param stock      扣减数量，必须大于零
     * @return 库存充足且扣减成功返回 {@code true}
     */
    default boolean subtractAwardStock(Long strategyId, Long awardId, int stock) {
        return subtractAwardStock(strategyId, awardId, stock, null);
    }

    /**
     * 原子扣减数据库中的策略奖品剩余库存。
     *
     * @param strategyId 策略标识
     * @param awardId    奖品标识
     * @return 成功扣减返回 {@code true}，记录不存在或库存不足返回 {@code false}
     */
    boolean updateStrategyAwardStock(Long strategyId, Long awardId);

    /**
     * 查询用户在指定策略下已经完成的抽奖次数。
     *
     * @param userId     用户标识
     * @param strategyId 策略标识
     * @return 已完成抽奖次数
     */
    long queryUserRaffleCount(String userId, Long strategyId);

    /**
     * 根据奖品标识查询奖品基础配置。
     * <p>
     * 抽奖流程只产出命中奖品 ID，应用层需要通过该方法补齐奖品业务键和发放配置，
     * 后续发奖消费者会根据奖品业务键选择积分、实物或优惠券等不同发奖策略。
     *
     * @param awardId 奖品标识
     * @return 匹配的奖品基础配置；不存在时返回空
     */
    Optional<AwardEntity> queryAwardEntity(Long awardId);

    /**
     * 批量查询指定策略下各奖品的次数锁门槛。
     *
     * <p>次数锁属于奖品级策略规则，规则值来自 {@code strategy_rule}，
     * 通过策略标识、奖品标识和 {@link RuleModel#LOCK} 唯一确定。</p>
     *
     * @param strategyId 策略标识
     * @param awardIds   配置了次数锁的奖品标识
     * @return 奖品标识到非负解锁门槛的不可变映射
     * @throws NullPointerException                                       策略标识或奖品集合为空时抛出
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 次数锁规则缺失、重复或格式非法时抛出
     */
    Map<Long, Integer> queryAwardRuleLockCounts(Long strategyId, List<Long> awardIds);
}
