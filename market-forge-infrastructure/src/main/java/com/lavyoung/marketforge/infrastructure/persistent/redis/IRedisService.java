package com.lavyoung.marketforge.infrastructure.persistent.redis;

import java.time.Duration;
import java.util.*;

/**
 * Redis 通用缓存服务。
 * <p>
 * 该接口抽象与业务无关的 Key/Value、Hash、List、Set 及队列能力，
 * 不暴露 Redisson 等具体客户端类型。
 * 阻塞队列方法保留线程中断语义，调用方应正确处理中断，避免业务线程无限等待。
 *
 * @author lavyoung
 * @version 1.0.0
 * @email lavyoung1325@outlook.com
 */
public interface IRedisService {

    /**
     * Redis List 查询的闭区间。
     * <p>
     * 起止下标支持 Redis 的负数语义，例如 {@code (0, -1)} 表示读取全部元素。
     *
     * @param start 起始下标
     * @param end   结束下标
     */
    record RedisListRange(int start, int end) {
    }

    /**
     * 写入不过期的缓存值。
     *
     * @param key   缓存键
     * @param value 缓存值
     * @param <T>   缓存值类型
     */
    <T> void setValue(String key, T value);

    /**
     * 写入带过期时间的缓存值。
     *
     * @param key   缓存键
     * @param value 缓存值
     * @param ttl   存活时间
     * @param <T>   缓存值类型
     */
    <T> void setValue(String key, T value, Duration ttl);

    /**
     * 获取并转换缓存值。
     *
     * @param key       缓存键
     * @param valueType 期望的值类型
     * @param <T>       缓存值类型
     * @return 缓存值；键不存在时返回空
     */
    <T> Optional<T> getValue(String key, Class<T> valueType);

    /**
     * 获取缓存中的列表值，并逐项转换为指定元素类型。
     * <p>
     * 此方法用于以单个 Key/Value 缓存整个列表的场景，不同于 Redis 原生 List 的区间读取操作。
     *
     * @param key         缓存键
     * @param elementType 列表元素的期望类型
     * @param <T>         列表元素类型
     * @return 缓存列表；键不存在时返回空
     * @throws ClassCastException 缓存值不是列表，或列表元素无法转换为期望类型
     */
    <T> Optional<List<T>> getValueList(String key, Class<T> elementType);

    /**
     * 仅在键不存在时写入带过期时间的缓存值。
     *
     * @param key   缓存键
     * @param value 缓存值
     * @param ttl   存活时间
     * @param <T>   缓存值类型
     * @return 写入成功返回 {@code true}，键已存在返回 {@code false}
     */
    <T> boolean setIfAbsent(String key, T value, Duration ttl);

    /**
     * 判断缓存键是否存在。
     *
     * @param key 缓存键
     * @return 键存在返回 {@code true}
     */
    boolean exists(String key);

    /**
     * 删除缓存键。
     *
     * @param key 缓存键
     * @return 实际删除成功返回 {@code true}
     */
    boolean delete(String key);

    /**
     * 批量删除缓存键。
     *
     * @param keys 待删除的缓存键
     * @return 实际删除的键数量
     */
    long delete(Collection<String> keys);

    /**
     * 重新设置缓存键的过期时间。
     *
     * @param key 缓存键
     * @param ttl 新的存活时间
     * @return 设置成功返回 {@code true}，键不存在返回 {@code false}
     */
    boolean expire(String key, Duration ttl);

    /**
     * 对整数缓存值执行原子增量操作。
     * <p>
     * 当键不存在时以 {@code 0} 为初始值，传入负数可实现递减。
     *
     * @param key   缓存键
     * @param delta 增量
     * @return 操作后的值
     */
    long increment(String key, long delta);

    /**
     * 写入单个 Hash 字段。
     *
     * @param key     Hash 键
     * @param hashKey Hash 字段键
     * @param value   Hash 字段值
     * @param <HK>    Hash 字段键类型
     * @param <HV>    Hash 字段值类型
     */
    <HK, HV> void putHashValue(String key, HK hashKey, HV value);

    /**
     * 批量写入 Hash 字段。
     *
     * @param key    Hash 键
     * @param values Hash 字段和值
     * @param <HK>   Hash 字段键类型
     * @param <HV>   Hash 字段值类型
     */
    <HK, HV> void putHashValues(String key, Map<HK, HV> values);

    /**
     * 获取单个 Hash 字段值。
     *
     * @param key       Hash 键
     * @param hashKey   Hash 字段键
     * @param valueType 期望的字段值类型
     * @param <HK>      Hash 字段键类型
     * @param <HV>      Hash 字段值类型
     * @return 字段值；字段不存在时返回空
     */
    <HK, HV> Optional<HV> getHashValue(String key, HK hashKey, Class<HV> valueType);

    /**
     * 获取 Hash 的全部字段和值。
     *
     * @param key         Hash 键
     * @param hashKeyType 期望的字段键类型
     * @param valueType   期望的字段值类型
     * @param <HK>        Hash 字段键类型
     * @param <HV>        Hash 字段值类型
     * @return Hash 内容；键不存在时返回空映射
     */
    <HK, HV> Map<HK, HV> getHashEntries(
            String key,
            Class<HK> hashKeyType,
            Class<HV> valueType);

    /**
     * 判断 Hash 字段是否存在。
     *
     * @param key     Hash 键
     * @param hashKey Hash 字段键
     * @param <HK>    Hash 字段键类型
     * @return 字段存在返回 {@code true}
     */
    <HK> boolean containsHashKey(String key, HK hashKey);

    /**
     * 删除一个或多个 Hash 字段。
     *
     * @param key      Hash 键
     * @param hashKeys 待删除的字段键
     * @param <HK>     Hash 字段键类型
     * @return 实际删除的字段数量
     */
    <HK> long deleteHashValues(String key, Collection<HK> hashKeys);

    /**
     * 获取 Hash 字段数量。
     *
     * @param key Hash 键
     * @return 字段数量
     */
    int hashSize(String key);

    /**
     * 向 List 头部添加元素。
     *
     * @param key   List 键
     * @param value 待添加元素
     * @param <T>   元素类型
     */
    <T> void addListFirst(String key, T value);

    /**
     * 向 List 尾部添加元素。
     *
     * @param key   List 键
     * @param value 待添加元素
     * @param <T>   元素类型
     */
    <T> void addListLast(String key, T value);

    /**
     * 批量向 List 尾部添加元素。
     *
     * @param key    List 键
     * @param values 待添加元素
     * @param <T>    元素类型
     * @return 实际添加的元素数量
     */
    <T> long addListValues(String key, Collection<T> values);

    /**
     * 根据下标获取 List 元素。
     *
     * @param key       List 键
     * @param index     元素下标
     * @param valueType 期望的元素类型
     * @param <T>       元素类型
     * @return 指定元素；下标超出范围时返回空
     */
    <T> Optional<T> getListValue(String key, int index, Class<T> valueType);

    /**
     * 按闭区间读取 List 元素。
     *
     * @param key       List 键
     * @param range     查询区间
     * @param valueType 期望的元素类型
     * @param <T>       元素类型
     * @return 区间内的元素；键不存在时返回空列表
     */
    <T> List<T> getListRange(String key, RedisListRange range, Class<T> valueType);

    /**
     * 删除 List 中首次出现的指定元素。
     *
     * @param key   List 键
     * @param value 待删除元素
     * @param <T>   元素类型
     * @return 删除成功返回 {@code true}
     */
    <T> boolean removeListValue(String key, T value);

    /**
     * 获取 List 元素数量。
     *
     * @param key List 键
     * @return 元素数量
     */
    int listSize(String key);

    /**
     * 向 Set 添加单个元素。
     *
     * @param key   Set 键
     * @param value 待添加元素
     * @param <T>   元素类型
     * @return 新增成功返回 {@code true}，元素已存在返回 {@code false}
     */
    <T> boolean addSetValue(String key, T value);

    /**
     * 批量向 Set 添加元素。
     *
     * @param key    Set 键
     * @param values 待添加元素
     * @param <T>    元素类型
     * @return 实际新增的元素数量
     */
    <T> long addSetValues(String key, Collection<T> values);

    /**
     * 判断 Set 是否包含指定元素。
     *
     * @param key   Set 键
     * @param value 待判断元素
     * @param <T>   元素类型
     * @return 包含该元素返回 {@code true}
     */
    <T> boolean containsSetValue(String key, T value);

    /**
     * 获取 Set 的全部元素。
     *
     * @param key       Set 键
     * @param valueType 期望的元素类型
     * @param <T>       元素类型
     * @return Set 元素；键不存在时返回空集合
     */
    <T> Set<T> getSetValues(String key, Class<T> valueType);

    /**
     * 从 Set 删除一个或多个元素。
     *
     * @param key    Set 键
     * @param values 待删除元素
     * @param <T>    元素类型
     * @return 实际删除的元素数量
     */
    <T> long removeSetValues(String key, Collection<T> values);

    /**
     * 随机弹出并删除一个 Set 元素。
     *
     * @param key       Set 键
     * @param valueType 期望的元素类型
     * @param <T>       元素类型
     * @return 被弹出的元素；Set 为空时返回空
     */
    <T> Optional<T> popSetValue(String key, Class<T> valueType);

    /**
     * 获取 Set 元素数量。
     *
     * @param key Set 键
     * @return 元素数量
     */
    int setSize(String key);

    /**
     * 将元素加入队尾。
     *
     * @param queueKey 队列键
     * @param value    待入队元素
     * @param <T>      元素类型
     * @return 入队成功返回 {@code true}
     */
    <T> boolean offer(String queueKey, T value);

    /**
     * 以可中断方式将元素加入队尾。
     *
     * @param queueKey 队列键
     * @param value    待入队元素
     * @param <T>      元素类型
     * @throws InterruptedException 等待期间当前线程被中断
     */
    <T> void put(String queueKey, T value) throws InterruptedException;

    /**
     * 批量将元素加入队尾。
     *
     * @param queueKey 队列键
     * @param values   待入队元素
     * @param <T>      元素类型
     * @return 实际入队的元素数量
     */
    <T> long offerAll(String queueKey, Collection<T> values);

    /**
     * 查看队首元素但不移除。
     *
     * @param queueKey  队列键
     * @param valueType 期望的元素类型
     * @param <T>       元素类型
     * @return 队首元素；队列为空时返回空
     */
    <T> Optional<T> peek(String queueKey, Class<T> valueType);

    /**
     * 立即获取并移除队首元素。
     *
     * @param queueKey  队列键
     * @param valueType 期望的元素类型
     * @param <T>       元素类型
     * @return 队首元素；队列为空时返回空
     */
    <T> Optional<T> poll(String queueKey, Class<T> valueType);

    /**
     * 在指定时间内等待、获取并移除队首元素。
     *
     * @param queueKey  队列键
     * @param valueType 期望的元素类型
     * @param timeout   最长等待时间
     * @param <T>       元素类型
     * @return 队首元素；等待超时返回空
     * @throws InterruptedException 等待期间当前线程被中断
     */
    <T> Optional<T> poll(String queueKey, Class<T> valueType, Duration timeout)
            throws InterruptedException;

    /**
     * 持续等待，直到获取并移除队首元素。
     *
     * @param queueKey  队列键
     * @param valueType 期望的元素类型
     * @param <T>       元素类型
     * @return 获取到的队首元素
     * @throws InterruptedException 等待期间当前线程被中断
     */
    <T> T take(String queueKey, Class<T> valueType) throws InterruptedException;

    /**
     * 从队首批量移除并返回不超过指定数量的元素。
     *
     * @param queueKey    队列键
     * @param maxElements 最大获取数量
     * @param valueType   期望的元素类型
     * @param <T>         元素类型
     * @return 已移除的元素；队列为空时返回空列表
     */
    <T> List<T> drain(String queueKey, int maxElements, Class<T> valueType);

    /**
     * 获取当前队列长度。
     *
     * @param queueKey 队列键
     * @return 队列中的元素数量
     */
    int queueSize(String queueKey);

    /**
     * 判断队列是否为空。
     *
     * @param queueKey 队列键
     * @return 队列为空返回 {@code true}
     */
    boolean isQueueEmpty(String queueKey);

    /**
     * 删除整个队列及其中的元素。
     *
     * @param queueKey 队列键
     * @return 实际删除成功返回 {@code true}
     */
    boolean deleteQueue(String queueKey);

}
