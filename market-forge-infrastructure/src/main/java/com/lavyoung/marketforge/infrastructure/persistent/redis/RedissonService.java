package com.lavyoung.marketforge.infrastructure.persistent.redis;

import org.redisson.api.*;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redisson 的 Redis 通用服务实现。
 *
 * @author lavyoung
 * @version 1.0.0
 * @email lavyoung1325@outlook.com
 */
@Service("redissonService")
public class RedissonService implements IRedisService {

    /**
     * Redisson 客户端。
     */
    private final RedissonClient redissonClient;

    /**
     * 创建 Redis 通用服务。
     *
     * @param redissonClient Redisson 客户端
     */
    public RedissonService(RedissonClient redissonClient) {
        this.redissonClient = Objects.requireNonNull(
                redissonClient,
                "redissonClient must not be null"
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> void setValue(String key, T value) {
        getBucket(key).set(requireValue(value));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> void setValue(String key, T value, Duration ttl) {
        getBucket(key).set(requireValue(value), validateTtl(ttl));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Optional<T> getValue(String key, Class<T> valueType) {
        return castNullable(getBucket(key).get(), valueType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Optional<List<T>> getValueList(String key, Class<T> elementType) {
        Objects.requireNonNull(elementType, "elementType must not be null");
        return Optional.ofNullable(this.<Object>getBucket(key).get())
                .map(value -> castValueList(value, elementType));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> boolean setIfAbsent(String key, T value, Duration ttl) {
        return getBucket(key).setIfAbsent(requireValue(value), validateTtl(ttl));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists(String key) {
        return getBucket(key).isExists();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean delete(String key) {
        return getBucket(key).delete();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long delete(Collection<String> keys) {
        Objects.requireNonNull(keys, "keys must not be null");
        if (keys.isEmpty()) {
            return 0L;
        }
        String[] validatedKeys = keys.stream()
                .map(this::validateKey)
                .toArray(String[]::new);
        return redissonClient.getKeys().delete(validatedKeys);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean expire(String key, Duration ttl) {
        return getBucket(key).expire(validateTtl(ttl));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long increment(String key, long delta) {
        return redissonClient.getAtomicLong(validateKey(key)).addAndGet(delta);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <HK, HV> void putHashValue(String key, HK hashKey, HV value) {
        RMap<HK, HV> hash = getHash(key);
        hash.put(requireValue(hashKey), requireValue(value));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <HK, HV> void putHashValues(String key, Map<HK, HV> values) {
        Objects.requireNonNull(values, "values must not be null");
        if (values.isEmpty()) {
            return;
        }
        Map<HK, HV> validatedValues = new LinkedHashMap<>(values.size());
        values.forEach((hashKey, value) -> validatedValues.put(
                requireValue(hashKey),
                requireValue(value)
        ));
        getHash(key).putAll(validatedValues);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <HK, HV> Optional<HV> getHashValue(
            String key,
            HK hashKey,
            Class<HV> valueType) {
        RMap<HK, Object> hash = getHash(key);
        return castNullable(hash.get(requireValue(hashKey)), valueType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <HK, HV> Map<HK, HV> getHashEntries(
            String key,
            Class<HK> hashKeyType,
            Class<HV> valueType) {
        Objects.requireNonNull(hashKeyType, "hashKeyType must not be null");
        Objects.requireNonNull(valueType, "valueType must not be null");
        Map<Object, Object> entries = this.<Object, Object>getHash(key).readAllMap();
        Map<HK, HV> result = new LinkedHashMap<>(entries.size());
        entries.forEach((hashKey, value) -> result.put(
                hashKeyType.cast(hashKey),
                valueType.cast(value)
        ));
        return Map.copyOf(result);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <HK> boolean containsHashKey(String key, HK hashKey) {
        return getHash(key).containsKey(requireValue(hashKey));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <HK> long deleteHashValues(String key, Collection<HK> hashKeys) {
        List<HK> validatedKeys = validateValues(hashKeys);
        if (validatedKeys.isEmpty()) {
            return 0L;
        }
        return this.<Object, Object>getHash(key).fastRemove(validatedKeys.toArray());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashSize(String key) {
        return getHash(key).size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> void addListFirst(String key, T value) {
        getList(key).add(0, requireValue(value));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> void addListLast(String key, T value) {
        getList(key).add(requireValue(value));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> long addListValues(String key, Collection<T> values) {
        List<T> validatedValues = validateValues(values);
        if (validatedValues.isEmpty()) {
            return 0L;
        }
        getList(key).addAll(validatedValues);
        return validatedValues.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Optional<T> getListValue(String key, int index, Class<T> valueType) {
        if (index < 0) {
            throw new IllegalArgumentException("index must not be negative");
        }
        return castNullable(getList(key).get(index), valueType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> List<T> getListRange(
            String key,
            RedisListRange range,
            Class<T> valueType) {
        Objects.requireNonNull(range, "range must not be null");
        List<Object> values = this.<Object>getList(key).range(range.start(), range.end());
        return castValues(values, valueType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> boolean removeListValue(String key, T value) {
        return getList(key).remove(requireValue(value));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int listSize(String key) {
        return getList(key).size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> boolean addSetValue(String key, T value) {
        return getSet(key).add(requireValue(value));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> long addSetValues(String key, Collection<T> values) {
        List<T> validatedValues = validateValues(values);
        if (validatedValues.isEmpty()) {
            return 0L;
        }
        return getSet(key).addAllCounted(validatedValues);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> boolean containsSetValue(String key, T value) {
        return getSet(key).contains(requireValue(value));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Set<T> getSetValues(String key, Class<T> valueType) {
        Set<Object> values = this.<Object>getSet(key).readAll();
        return Set.copyOf(castValues(values, valueType));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> long removeSetValues(String key, Collection<T> values) {
        List<T> validatedValues = validateValues(values);
        if (validatedValues.isEmpty()) {
            return 0L;
        }
        return getSet(key).removeAllCounted(validatedValues);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Optional<T> popSetValue(String key, Class<T> valueType) {
        return castNullable(getSet(key).removeRandom(), valueType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int setSize(String key) {
        return getSet(key).size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> boolean offer(String queueKey, T value) {
        return getBlockingQueue(queueKey).offer(requireValue(value));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> void put(String queueKey, T value) throws InterruptedException {
        getBlockingQueue(queueKey).put(requireValue(value));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> long offerAll(String queueKey, Collection<T> values) {
        Objects.requireNonNull(values, "values must not be null");
        if (values.isEmpty()) {
            return 0L;
        }
        List<T> validatedValues = values.stream()
                .map(this::requireValue)
                .toList();
        getBlockingQueue(queueKey).addAll(validatedValues);
        return validatedValues.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Optional<T> peek(String queueKey, Class<T> valueType) {
        return castNullable(getBlockingQueue(queueKey).peek(), valueType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Optional<T> poll(String queueKey, Class<T> valueType) {
        return castNullable(getBlockingQueue(queueKey).poll(), valueType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Optional<T> poll(String queueKey, Class<T> valueType, Duration timeout)
            throws InterruptedException {
        validateTimeout(timeout);
        Object value = getBlockingQueue(queueKey).poll(timeout.toNanos(), TimeUnit.NANOSECONDS);
        return castNullable(value, valueType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T take(String queueKey, Class<T> valueType) throws InterruptedException {
        return valueType.cast(getBlockingQueue(queueKey).take());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> List<T> drain(String queueKey, int maxElements, Class<T> valueType) {
        if (maxElements <= 0) {
            throw new IllegalArgumentException("maxElements must be greater than zero");
        }
        List<Object> values = new ArrayList<>(maxElements);
        getBlockingQueue(queueKey).drainTo(values, maxElements);
        return values.stream().map(valueType::cast).toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int queueSize(String queueKey) {
        return getBlockingQueue(queueKey).size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isQueueEmpty(String queueKey) {
        return getBlockingQueue(queueKey).isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean deleteQueue(String queueKey) {
        return getBlockingQueue(queueKey).delete();
    }

    /**
     * 获取指定名称的阻塞队列。
     *
     * @param queueKey 队列键
     * @param <T>      元素类型
     * @return Redisson 阻塞队列
     */
    private <T> RBlockingQueue<T> getBlockingQueue(String queueKey) {
        return redissonClient.getBlockingQueue(validateKey(queueKey));
    }

    /**
     * 获取指定键的值容器。
     *
     * @param key 缓存键
     * @param <T> 缓存值类型
     * @return Redisson 值容器
     */
    private <T> RBucket<T> getBucket(String key) {
        return redissonClient.getBucket(validateKey(key));
    }

    /**
     * 获取指定键的 Hash 容器。
     *
     * @param key  Hash 键
     * @param <HK> Hash 字段键类型
     * @param <HV> Hash 字段值类型
     * @return Redisson Hash 容器
     */
    private <HK, HV> RMap<HK, HV> getHash(String key) {
        return redissonClient.getMap(validateKey(key));
    }

    /**
     * 获取指定键的 List 容器。
     *
     * @param key List 键
     * @param <T> 元素类型
     * @return Redisson List 容器
     */
    private <T> RList<T> getList(String key) {
        return redissonClient.getList(validateKey(key));
    }

    /**
     * 获取指定键的 Set 容器。
     *
     * @param key Set 键
     * @param <T> 元素类型
     * @return Redisson Set 容器
     */
    private <T> RSet<T> getSet(String key) {
        return redissonClient.getSet(validateKey(key));
    }

    /**
     * 将可能为空的队列元素安全转换为目标类型。
     *
     * @param value     队列元素
     * @param valueType 目标类型
     * @param <T>       元素类型
     * @return 转换后的元素；值为空时返回空
     */
    private <T> Optional<T> castNullable(Object value, Class<T> valueType) {
        Objects.requireNonNull(valueType, "valueType must not be null");
        return Optional.ofNullable(value).map(valueType::cast);
    }

    /**
     * 将缓存值校验为列表，并将每个元素转换为指定类型。
     *
     * @param value       缓存值
     * @param elementType 列表元素目标类型
     * @param <T>         列表元素类型
     * @return 转换后的不可修改列表
     * @throws ClassCastException 缓存值不是列表，或列表元素无法转换为目标类型
     */
    private <T> List<T> castValueList(Object value, Class<T> elementType) {
        if (!(value instanceof List<?> values)) {
            throw new ClassCastException("cached value must be a List");
        }
        return castValues(values, elementType);
    }

    /**
     * 将集合元素转换为指定类型。
     *
     * @param values    待转换元素
     * @param valueType 目标类型
     * @param <T>       元素类型
     * @return 转换后的不可修改列表
     */
    private <T> List<T> castValues(Collection<?> values, Class<T> valueType) {
        Objects.requireNonNull(valueType, "valueType must not be null");
        return values.stream().map(valueType::cast).toList();
    }

    /**
     * 校验批量操作的元素。
     *
     * @param values 待校验元素
     * @param <T>    元素类型
     * @return 校验后的不可修改列表
     */
    private <T> List<T> validateValues(Collection<T> values) {
        Objects.requireNonNull(values, "values must not be null");
        return values.stream().map(this::requireValue).toList();
    }

    /**
     * 校验阻塞等待时间。
     *
     * @param timeout 最长等待时间
     * @throws IllegalArgumentException 等待时间为负数
     */
    private void validateTimeout(Duration timeout) {
        Objects.requireNonNull(timeout, "timeout must not be null");
        if (timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must not be negative");
        }
    }

    /**
     * 校验缓存键。
     *
     * @param key 缓存键
     * @return 校验通过的缓存键
     * @throws IllegalArgumentException 缓存键为空白字符串
     */
    private String validateKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key must not be blank");
        }
        return key;
    }

    /**
     * 校验缓存值。
     *
     * @param value 缓存值
     * @param <T>   缓存值类型
     * @return 校验通过的缓存值
     * @throws NullPointerException 缓存值为空
     */
    private <T> T requireValue(T value) {
        return Objects.requireNonNull(value, "value must not be null");
    }

    /**
     * 校验缓存存活时间。
     *
     * @param ttl 存活时间
     * @return 校验通过的存活时间
     * @throws IllegalArgumentException 存活时间为零或负数
     */
    private Duration validateTtl(Duration ttl) {
        Objects.requireNonNull(ttl, "ttl must not be null");
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("ttl must be greater than zero");
        }
        return ttl;
    }
}
