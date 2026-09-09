package com.lavyoung.marketforge.infrastructure.persistent.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 验证 {@link RedissonService} 的 Redis 操作委托与参数边界。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0-SNAPSHOT
 */
@ExtendWith(MockitoExtension.class)
class RedissonServiceTest {

    private static final String CACHE_KEY = "cache:test";
    private static final String HASH_KEY = "hash:test";
    private static final String LIST_KEY = "list:test";
    private static final String SET_KEY = "set:test";
    private static final String QUEUE_KEY = "queue:test";
    private static final String LOCK_KEY = "lock:test";

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RBucket<Object> bucket;

    @Mock
    private RBlockingQueue<Object> blockingQueue;

    @Mock
    private RMap<Object, Object> hash;

    @Mock
    private RList<Object> list;

    @Mock
    private RSet<Object> set;

    @Mock
    private RKeys keys;

    @Mock
    private RAtomicLong atomicLong;

    @Mock
    private RLock lock;

    @Mock
    private RDelayedQueue<Object> delayedQueue;

    private RedissonService redisService;

    /**
     * 为每个测试创建服务实例。
     */
    @BeforeEach
    void setUp() {
        redisService = new RedissonService(redissonClient);
    }

    /**
     * Given 合法缓存值，When 执行读写，Then 委托给对应的 Redisson Bucket。
     */
    @Test
    void shouldSetAndGetValue() {
        when(redissonClient.getBucket(CACHE_KEY)).thenReturn(bucket);
        when(bucket.get()).thenReturn("cached-value");

        redisService.setValue(CACHE_KEY, "cached-value");
        Optional<String> result = redisService.getValue(CACHE_KEY, String.class);

        verify(bucket).set("cached-value");
        assertEquals(Optional.of("cached-value"), result);
    }

    /**
     * Given 缓存中存放列表，When 按元素类型读取，Then 返回类型安全的列表。
     */
    @Test
    void shouldGetTypedValueList() {
        when(redissonClient.getBucket(CACHE_KEY)).thenReturn(bucket);
        when(bucket.get()).thenReturn(List.of("award-1", "award-2"));

        Optional<List<String>> result = redisService.getValueList(CACHE_KEY, String.class);

        assertEquals(Optional.of(List.of("award-1", "award-2")), result);
    }

    /**
     * Given 正数 TTL，When 写入和条件写入，Then 使用 Duration 过期语义。
     */
    @Test
    void shouldSetValueWithTtlAndSetIfAbsent() {
        Duration ttl = Duration.ofMinutes(5);
        when(redissonClient.getBucket(CACHE_KEY)).thenReturn(bucket);
        when(bucket.setIfAbsent("cached-value", ttl)).thenReturn(true);

        redisService.setValue(CACHE_KEY, "cached-value", ttl);
        boolean inserted = redisService.setIfAbsent(CACHE_KEY, "cached-value", ttl);

        verify(bucket).set("cached-value", ttl);
        assertTrue(inserted);
    }

    /**
     * Given 多个缓存键和计数器，When 删除并增加计数，Then 返回 Redis 实际结果。
     */
    @Test
    void shouldDeleteKeysAndIncrementAtomically() {
        when(redissonClient.getKeys()).thenReturn(keys);
        when(keys.delete("key:1", "key:2")).thenReturn(2L);
        when(redissonClient.getAtomicLong(CACHE_KEY)).thenReturn(atomicLong);
        when(atomicLong.addAndGet(3L)).thenReturn(8L);

        long deleted = redisService.delete(List.of("key:1", "key:2"));
        long current = redisService.increment(CACHE_KEY, 3L);

        assertEquals(2L, deleted);
        assertEquals(8L, current);
    }

    /**
     * Given 原子长整型计数器，When 执行赋值、CAS 和增减操作，Then 委托给同一个 Redis 原子对象并返回结果。
     */
    @Test
    void shouldOperateAtomicLong() {
        // Given
        when(redissonClient.getAtomicLong(CACHE_KEY)).thenReturn(atomicLong);
        when(atomicLong.get()).thenReturn(10L);
        when(atomicLong.getAndSet(20L)).thenReturn(10L);
        when(atomicLong.compareAndSet(20L, 30L)).thenReturn(true);
        when(atomicLong.addAndGet(-5L)).thenReturn(25L);
        when(atomicLong.incrementAndGet()).thenReturn(26L);
        when(atomicLong.decrementAndGet()).thenReturn(25L);

        // When
        long initial = redisService.getAtomicLong(CACHE_KEY);
        redisService.setAtomicLong(CACHE_KEY, 20L);
        long previous = redisService.getAndSetAtomicLong(CACHE_KEY, 20L);
        boolean updated = redisService.compareAndSetAtomicLong(CACHE_KEY, 20L, 30L);
        long added = redisService.addAndGetAtomicLong(CACHE_KEY, -5L);
        long incremented = redisService.incrementAndGetAtomicLong(CACHE_KEY);
        long decremented = redisService.decrementAndGetAtomicLong(CACHE_KEY);

        // Then
        assertAll(
                () -> assertEquals(10L, initial),
                () -> assertEquals(10L, previous),
                () -> assertTrue(updated),
                () -> assertEquals(25L, added),
                () -> assertEquals(26L, incremented),
                () -> assertEquals(25L, decremented)
        );
        verify(atomicLong).set(20L);
    }

    /**
     * Given 可用分布式锁，When 使用看门狗、固定租约和限时尝试方式加锁，Then 正确委托并安全解锁。
     *
     * @throws InterruptedException 等待锁期间当前线程被中断
     */
    @Test
    void shouldOperateDistributedLock() throws InterruptedException {
        // Given
        Duration leaseTime = Duration.ofSeconds(30);
        Duration waitTime = Duration.ofSeconds(2);
        when(redissonClient.getLock(LOCK_KEY)).thenReturn(lock);
        when(lock.tryLock()).thenReturn(true);
        when(lock.tryLock(waitTime.toNanos(), leaseTime.toNanos(), TimeUnit.NANOSECONDS)).thenReturn(true);
        when(lock.isLocked()).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        // When
        redisService.lock(LOCK_KEY);
        redisService.lock(LOCK_KEY, leaseTime);
        boolean acquiredImmediately = redisService.tryLock(LOCK_KEY);
        boolean acquiredWithinWait = redisService.tryLock(LOCK_KEY, waitTime, leaseTime);
        boolean locked = redisService.isLocked(LOCK_KEY);
        boolean heldByCurrentThread = redisService.isHeldByCurrentThread(LOCK_KEY);
        boolean unlocked = redisService.unlock(LOCK_KEY);

        // Then
        assertAll(
                () -> assertTrue(acquiredImmediately),
                () -> assertTrue(acquiredWithinWait),
                () -> assertTrue(locked),
                () -> assertTrue(heldByCurrentThread),
                () -> assertTrue(unlocked)
        );
        verify(lock).lock();
        verify(lock).lock(leaseTime.toNanos(), TimeUnit.NANOSECONDS);
        verify(lock).unlock();
    }

    /**
     * Given 当前线程未持有分布式锁，When 请求解锁，Then 返回失败且不调用底层解锁操作。
     */
    @Test
    void shouldNotUnlockLockOwnedByAnotherThread() {
        // Given
        when(redissonClient.getLock(LOCK_KEY)).thenReturn(lock);
        when(lock.isHeldByCurrentThread()).thenReturn(false);

        // When
        boolean unlocked = redisService.unlock(LOCK_KEY);

        // Then
        assertFalse(unlocked);
        verify(lock, never()).unlock();
    }

    /**
     * Given 锁在持有检查后租约到期，When 底层解锁报告所有权丢失，Then 返回失败而不向业务层泄漏客户端异常。
     */
    @Test
    void shouldReturnFalseWhenLockLeaseExpiresBeforeUnlock() {
        // Given
        when(redissonClient.getLock(LOCK_KEY)).thenReturn(lock);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        doThrow(new IllegalMonitorStateException()).when(lock).unlock();

        // When
        boolean unlocked = redisService.unlock(LOCK_KEY);

        // Then
        assertFalse(unlocked);
    }

    /**
     * Given 同名阻塞队列作为投递目标，When 操作延时队列，Then 正确完成投递、查询、移除和资源销毁。
     */
    @Test
    void shouldOperateDelayedQueue() {
        // Given
        Duration delay = Duration.ofSeconds(5);
        when(redissonClient.getBlockingQueue(QUEUE_KEY)).thenReturn(blockingQueue);
        when(redissonClient.getDelayedQueue(blockingQueue)).thenReturn(delayedQueue);
        when(delayedQueue.contains("job-1")).thenReturn(true);
        when(delayedQueue.remove("job-1")).thenReturn(true);
        when(delayedQueue.size()).thenReturn(1);

        // When
        redisService.offerDelayed(QUEUE_KEY, "job-1", delay);
        boolean contained = redisService.containsDelayed(QUEUE_KEY, "job-1");
        int size = redisService.delayedQueueSize(QUEUE_KEY);
        boolean removed = redisService.removeDelayed(QUEUE_KEY, "job-1");
        redisService.destroyDelayedQueue(QUEUE_KEY);

        // Then
        assertAll(
                () -> assertTrue(contained),
                () -> assertEquals(1, size),
                () -> assertTrue(removed)
        );
        verify(delayedQueue).offer("job-1", delay.toNanos(), TimeUnit.NANOSECONDS);
        verify(delayedQueue).destroy();
    }

    /**
     * Given 延时消息已进入目标阻塞队列，When 立即、限时或持续等待获取，Then 返回对应的已到期消息。
     *
     * @throws InterruptedException 等待消息期间当前线程被中断
     */
    @Test
    void shouldGetReadyDelayedValues() throws InterruptedException {
        // Given
        Duration timeout = Duration.ofSeconds(2);
        when(redissonClient.getBlockingQueue(QUEUE_KEY)).thenReturn(blockingQueue);
        when(blockingQueue.poll()).thenReturn("job-1");
        when(blockingQueue.poll(timeout.toNanos(), TimeUnit.NANOSECONDS)).thenReturn("job-2");
        when(blockingQueue.take()).thenReturn("job-3");

        // When
        Optional<String> immediate = redisService.pollDelayed(QUEUE_KEY, String.class);
        Optional<String> waited = redisService.pollDelayed(QUEUE_KEY, String.class, timeout);
        String taken = redisService.takeDelayed(QUEUE_KEY, String.class);

        // Then
        assertAll(
                () -> assertEquals(Optional.of("job-1"), immediate),
                () -> assertEquals(Optional.of("job-2"), waited),
                () -> assertEquals("job-3", taken)
        );
    }

    /**
     * Given Hash 字段，When 执行写入和读取，Then 返回类型安全的字段内容。
     */
    @Test
    void shouldOperateHashValues() {
        when(redissonClient.getMap(HASH_KEY)).thenReturn(hash);
        when(hash.get("field-1")).thenReturn("value-1");
        when(hash.readAllMap()).thenReturn(Map.of("field-1", "value-1"));

        redisService.putHashValue(HASH_KEY, "field-1", "value-1");
        Optional<String> value = redisService.getHashValue(
                HASH_KEY,
                "field-1",
                String.class
        );
        Map<String, String> entries = redisService.getHashEntries(
                HASH_KEY,
                String.class,
                String.class
        );

        verify(hash).put("field-1", "value-1");
        assertEquals(Optional.of("value-1"), value);
        assertEquals(Map.of("field-1", "value-1"), entries);
    }

    /**
     * Given List 元素，When 执行首尾写入和区间读取，Then 保持元素顺序。
     */
    @Test
    void shouldOperateListValues() {
        IRedisService.RedisListRange range = new IRedisService.RedisListRange(0, -1);
        when(redissonClient.getList(LIST_KEY)).thenReturn(list);
        when(list.range(0, -1)).thenReturn(List.of("first", "last"));

        redisService.addListFirst(LIST_KEY, "first");
        redisService.addListLast(LIST_KEY, "last");
        List<String> values = redisService.getListRange(LIST_KEY, range, String.class);

        verify(list).add(0, "first");
        verify(list).add("last");
        assertEquals(List.of("first", "last"), values);
    }

    /**
     * Given Set 元素，When 批量添加并读取，Then 返回去重后的集合。
     */
    @Test
    void shouldOperateSetValues() {
        when(redissonClient.getSet(SET_KEY)).thenReturn(set);
        when(set.addAllCounted(List.of("A", "B"))).thenReturn(2);
        when(set.readAll()).thenReturn(Set.of("A", "B"));

        long added = redisService.addSetValues(SET_KEY, List.of("A", "B"));
        Set<String> values = redisService.getSetValues(SET_KEY, String.class);

        assertEquals(2L, added);
        assertEquals(Set.of("A", "B"), values);
    }

    /**
     * Given 阻塞队列已有数据，When 执行入队和超时出队，Then 返回对应队列元素。
     *
     * @throws InterruptedException 阻塞等待被中断
     */
    @Test
    void shouldOfferAndPollBlockingQueue() throws InterruptedException {
        Duration timeout = Duration.ofSeconds(2);
        when(redissonClient.getBlockingQueue(QUEUE_KEY)).thenReturn(blockingQueue);
        when(blockingQueue.offer("job-1")).thenReturn(true);
        when(blockingQueue.poll(timeout.toNanos(), TimeUnit.NANOSECONDS)).thenReturn("job-1");

        boolean offered = redisService.offer(QUEUE_KEY, "job-1");
        Optional<String> value = redisService.poll(QUEUE_KEY, String.class, timeout);

        assertTrue(offered);
        assertEquals(Optional.of("job-1"), value);
    }

    /**
     * Given 空队列，When 立即出队，Then 返回空 Optional。
     */
    @Test
    void shouldReturnEmptyWhenQueueHasNoElement() {
        when(redissonClient.getBlockingQueue(QUEUE_KEY)).thenReturn(blockingQueue);
        when(blockingQueue.poll()).thenReturn(null);

        Optional<String> value = redisService.poll(QUEUE_KEY, String.class);

        assertFalse(value.isPresent());
    }

    /**
     * Given 非法参数，When 调用 Redis 操作，Then 在访问客户端前拒绝请求。
     */
    @Test
    void shouldRejectInvalidArguments() {
        assertThrows(IllegalArgumentException.class,
                () -> redisService.setValue(" ", "value"));
        assertThrows(NullPointerException.class,
                () -> redisService.setValue(CACHE_KEY, null));
        assertThrows(IllegalArgumentException.class,
                () -> redisService.setValue(CACHE_KEY, "value", Duration.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> redisService.poll(QUEUE_KEY, String.class, Duration.ofSeconds(-1)));
        assertThrows(IllegalArgumentException.class,
                () -> redisService.drain(QUEUE_KEY, 0, String.class));
        assertThrows(IllegalArgumentException.class,
                () -> redisService.lock(LOCK_KEY, Duration.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> redisService.tryLock(LOCK_KEY, Duration.ofSeconds(-1), Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class,
                () -> redisService.tryLock(LOCK_KEY, Duration.ZERO, Duration.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> redisService.offerDelayed(QUEUE_KEY, "job-1", Duration.ofSeconds(-1)));
        assertThrows(NullPointerException.class,
                () -> redisService.offerDelayed(QUEUE_KEY, null, Duration.ofSeconds(1)));
        assertThrows(NullPointerException.class,
                () -> redisService.offerDelayed(QUEUE_KEY, "job-1", null));
    }

}
