package com.lavyoung.marketforge.infrastructure.persistent.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lavyoung.marketforge.infrastructure.persistent.po.mq.TaskPO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 验证用户中奖记录和消息任务持久化对象的表映射契约。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
class AdditionalPersistentObjectMappingTest {

    private static final Map<Class<? extends BasePO>, String> TABLE_MAPPINGS = Map.of(
            UserAwardRecordPO.class, "user_award_record",
            TaskPO.class, "task"
    );

    /**
     * 验证新增持久化对象声明了预期表名和自增主键映射。
     *
     * @throws NoSuchFieldException 当持久化对象缺少 id 字段时抛出
     */
    @Test
    void shouldMapPersistentObjectsAndPrimaryKeys() throws NoSuchFieldException {
        // Given
        for (Map.Entry<Class<? extends BasePO>, String> entry : TABLE_MAPPINGS.entrySet()) {
            Class<? extends BasePO> persistentObjectType = entry.getKey();

            // When
            TableName tableName = persistentObjectType.getAnnotation(TableName.class);
            Field idField = persistentObjectType.getDeclaredField("id");
            TableId tableId = idField.getAnnotation(TableId.class);

            // Then
            assertNotNull(tableName);
            assertEquals(entry.getValue(), tableName.value());
            assertNotNull(tableId);
            assertEquals(IdType.AUTO, tableId.type());
        }
    }
}
