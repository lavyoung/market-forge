package com.lavyoung.marketforge.infrastructure.persistent.po.activity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lavyoung.marketforge.infrastructure.persistent.po.BasePO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 活动域持久化对象与数据库表结构的映射测试。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/18
 */
class ActivityPersistentObjectMappingTest {

    private static final Map<Class<? extends BasePO>, String> TABLE_MAPPINGS = Map.of(
            ActivityPO.class, "raffle_activity",
            ActivityCountPO.class, "raffle_activity_count",
            ActivityAccountPO.class, "raffle_activity_account",
            ActivityAccountDayPO.class, "raffle_activity_account_day",
            ActivityAccountMonthPO.class, "raffle_activity_account_month",
            ActivitySkuPO.class, "raffle_activity_sku",
            ActivityOrderPO.class, "raffle_activity_order"
    );

    private static final Map<Class<? extends BasePO>, Set<String>> TABLE_FIELDS = Map.of(
            ActivityPO.class, Set.of(
                    "id", "activityId", "activityName", "activityDesc", "beginDateTime", "endDateTime",
                    "stockCount", "stockCountSurplus", "activityCountId", "strategyId", "state"
            ),
            ActivityCountPO.class, Set.of(
                    "id", "activityCountId", "totalCount", "dayCount", "monthCount"
            ),
            ActivityAccountPO.class, Set.of(
                    "id", "userId", "activityId", "totalCount", "totalCountSurplus", "dayCount",
                    "dayCountSurplus", "monthCount", "monthCountSurplus", "version"
            ),
            ActivityAccountDayPO.class, Set.of(
                    "id", "userId", "activityId", "dayCount", "dayCountSurplus", "day", "version"
            ),
            ActivityAccountMonthPO.class, Set.of(
                    "id", "userId", "activityId", "monthCount", "monthCountSurplus", "month", "version"
            ),
            ActivitySkuPO.class, Set.of(
                    "id", "sku", "activityId", "activityCountId", "stockCount"
            ),
            ActivityOrderPO.class, Set.of(
                    "id", "userId", "activityId", "sku", "activityName", "strategyId", "orderId", "orderTime",
                    "totalCount", "dayCount", "monthCount", "state", "outBusinessNo"
            )
    );

    /**
     * 验证活动域持久化对象声明的表名与数据库表结构保持一致。
     */
    @Test
    void shouldMapPersistentObjectsToExpectedTables() {
        // Given
        TABLE_MAPPINGS.forEach((persistentObjectType, expectedTableName) -> {
            // When
            TableName tableName = persistentObjectType.getAnnotation(TableName.class);

            // Then
            assertNotNull(tableName, () -> persistentObjectType.getSimpleName() + " 缺少 @TableName");
            assertEquals(expectedTableName, tableName.value());
            assertTrue(BasePO.class.isAssignableFrom(persistentObjectType));
        });
    }

    /**
     * 验证活动域持久化对象的主键字段使用数据库自增策略。
     *
     * @throws NoSuchFieldException 当持久化对象缺少 id 字段时抛出
     */
    @Test
    void shouldDeclareAutoIncrementPrimaryKeys() throws NoSuchFieldException {
        // Given
        for (Class<? extends BasePO> persistentObjectType : TABLE_MAPPINGS.keySet()) {
            Field idField = persistentObjectType.getDeclaredField("id");

            // When
            TableId tableId = idField.getAnnotation(TableId.class);

            // Then
            assertNotNull(tableId, () -> persistentObjectType.getSimpleName() + ".id 缺少 @TableId");
            assertEquals(IdType.AUTO, tableId.type());
        }
    }

    /**
     * 验证活动域持久化对象声明的业务字段与表结构保持一致。
     */
    @Test
    void shouldContainAllActivityTableFields() {
        // Given
        TABLE_FIELDS.forEach((persistentObjectType, expectedFields) -> {
            // When
            Set<String> actualFields = Arrays.stream(persistentObjectType.getDeclaredFields())
                    .filter(field -> !field.isSynthetic())
                    .map(Field::getName)
                    .collect(Collectors.toUnmodifiableSet());

            // Then
            assertEquals(expectedFields, actualFields, () -> persistentObjectType.getSimpleName() + " 字段与表结构不一致");
        });
    }
}
