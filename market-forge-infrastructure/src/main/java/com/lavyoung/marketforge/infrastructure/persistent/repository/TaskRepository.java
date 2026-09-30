package com.lavyoung.marketforge.infrastructure.persistent.repository;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.lavyoung.marketforge.domain.message.model.entity.TaskEntity;
import com.lavyoung.marketforge.domain.message.model.valobj.TaskStateVO;
import com.lavyoung.marketforge.domain.message.repository.ITaskRepository;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.TaskAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.dao.mq.ITaskDao;
import com.lavyoung.marketforge.infrastructure.persistent.po.TaskPO;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import io.github.lavyoung.lavshard.starter.internal.datasource.LavShardManagedDataSourceRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 本地消息任务仓储实现。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/29
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class TaskRepository implements ITaskRepository {

    private static final String QUERY_PUBLISHABLE_TASKS_SQL = """
            SELECT id, user_id, topic, event_id, event_type, message_body, occurred_at,
                   state, retry_count, next_retry_time, last_error, create_time, update_time
            FROM task
            WHERE state = ?
               OR (state = ? AND (next_retry_time IS NULL OR next_retry_time <= ?))
               OR (state = ? AND update_time <= ?)
            ORDER BY create_time ASC
            LIMIT ?
            """;

    private final ITaskDao taskDao;
    private final TaskAssembler taskAssembler;
    private final LavShardManagedDataSourceRegistry dataSourceRegistry;

    @Override
    public void save(TaskEntity taskEntity) {
        int rows = taskDao.insert(taskAssembler.toPO(taskEntity));
        if (rows < 1) {
            throw BusinessException.of(BusinessResponseCode.TASK_CREATE_FAILED);
        }
    }

    @Override
    public Optional<TaskEntity> queryByEventId(String userId, String eventId) {
        return taskDao.queryByEventId(userId, eventId).map(taskAssembler::toEntity);
    }

    @Override
    public List<TaskEntity> queryPublishableTasks(int limit, LocalDateTime now, LocalDateTime publishingExpiredBefore) {
        if (limit <= 0) {
            return List.of();
        }

        List<TaskEntity> candidates = new ArrayList<>();
        dataSourceRegistry.dataSources().forEach((dataSourceId, dataSource) -> {
            try {
                candidates.addAll(queryPublishableTasks(
                        dataSource, limit, now, publishingExpiredBefore));
            } catch (SQLException exception) {
                log.error("扫描本地消息分片失败 dataSourceId={}", dataSourceId, exception);
            }
        });

        return candidates.stream()
                .sorted(Comparator.comparing(
                        TaskEntity::occurredAt,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .limit(limit)
                .toList();
    }

    @Override
    public boolean markPublishing(String userId, String eventId, LocalDateTime now,
                                  LocalDateTime publishingExpiredBefore) {
        UpdateWrapper<TaskPO> updateWrapper = new UpdateWrapper<TaskPO>()
                .eq("user_id", userId)
                .eq("event_id", eventId)
                .and(wrapper -> wrapper
                        .eq("state", TaskStateVO.CREATE.getCode())
                        .or(failed -> failed
                                .eq("state", TaskStateVO.PUBLISH_FAILED.getCode())
                                .and(retry -> retry.isNull("next_retry_time").or().le("next_retry_time", now)))
                        .or(stale -> stale
                                .eq("state", TaskStateVO.PUBLISHING.getCode())
                                .le("update_time", publishingExpiredBefore)))
                .set("state", TaskStateVO.PUBLISHING.getCode())
                .set("next_retry_time", null)
                .set("last_error", null)
                .set("update_time", now);
        return taskDao.update(null, updateWrapper) == 1;
    }

    @Override
    public boolean markPublished(String userId, String eventId) {
        UpdateWrapper<TaskPO> updateWrapper = new UpdateWrapper<TaskPO>()
                .eq("user_id", userId)
                .eq("event_id", eventId)
                .eq("state", TaskStateVO.PUBLISHING.getCode())
                .set("state", TaskStateVO.PUBLISHED.getCode())
                .set("next_retry_time", null)
                .set("last_error", null);
        return taskDao.update(null, updateWrapper) == 1;
    }

    @Override
    public boolean markPublishFailed(String userId, String eventId, String errorMessage,
                                     LocalDateTime nextRetryTime) {
        UpdateWrapper<TaskPO> updateWrapper = new UpdateWrapper<TaskPO>()
                .eq("user_id", userId)
                .eq("event_id", eventId)
                .eq("state", TaskStateVO.PUBLISHING.getCode())
                .set("state", TaskStateVO.PUBLISH_FAILED.getCode())
                .set("next_retry_time", nextRetryTime)
                .set("last_error", truncate(errorMessage))
                .setSql("retry_count = retry_count + 1");
        return taskDao.update(null, updateWrapper) == 1;
    }

    private List<TaskEntity> queryPublishableTasks(DataSource dataSource, int limit, LocalDateTime now,
                                                   LocalDateTime publishingExpiredBefore) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(QUERY_PUBLISHABLE_TASKS_SQL)) {
            statement.setString(1, TaskStateVO.CREATE.getCode());
            statement.setString(2, TaskStateVO.PUBLISH_FAILED.getCode());
            statement.setTimestamp(3, Timestamp.valueOf(now));
            statement.setString(4, TaskStateVO.PUBLISHING.getCode());
            statement.setTimestamp(5, Timestamp.valueOf(publishingExpiredBefore));
            statement.setInt(6, limit);

            try (ResultSet resultSet = statement.executeQuery()) {
                List<TaskEntity> tasks = new ArrayList<>();
                while (resultSet.next()) {
                    tasks.add(toTaskEntity(resultSet));
                }
                return tasks;
            }
        }
    }

    private TaskEntity toTaskEntity(ResultSet resultSet) throws SQLException {
        return TaskEntity.builder()
                .userId(resultSet.getString("user_id"))
                .topic(resultSet.getString("topic"))
                .eventId(resultSet.getString("event_id"))
                .eventType(resultSet.getString("event_type"))
                .messageBody(resultSet.getString("message_body"))
                .occurredAt(toLocalDateTime(resultSet.getTimestamp("occurred_at")))
                .state(resultSet.getString("state"))
                .retryCount(resultSet.getInt("retry_count"))
                .nextRetryTime(toLocalDateTime(resultSet.getTimestamp("next_retry_time")))
                .lastError(resultSet.getString("last_error"))
                .build();
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private String truncate(String errorMessage) {
        if (errorMessage == null || errorMessage.length() <= 1024) {
            return errorMessage;
        }
        return errorMessage.substring(0, 1024);
    }

}
