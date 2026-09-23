package com.lavyoung.marketforge.infrastructure.persistent.assembler;

import cn.hutool.json.JSONException;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.lavyoung.marketforge.domain.award.event.SendAwardRecordEvent;
import com.lavyoung.marketforge.domain.award.model.entity.TaskEntity;
import com.lavyoung.marketforge.infrastructure.persistent.po.TaskPO;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.mapstruct.*;

import java.time.DateTimeException;
import java.time.Instant;
import java.util.List;

/**
 * 消息任务持久化对象与领域实体转换器。
 * <p>
 * 领域层保留结构化的发奖事件对象，持久化层只保存序列化后的消息体字符串。
 * 本转换器负责隔离这两个模型差异，避免 JSON 编解码逻辑泄漏到领域服务。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface TaskAssembler {

    /**
     * 将消息任务持久化对象转换为领域实体。
     *
     * @param source 消息任务持久化对象
     * @return 消息任务领域实体
     * @throws NullPointerException 当 {@code source} 为空时抛出
     * @throws BusinessException    当消息体 JSON 无法还原为发奖事件时抛出
     */
    @Mapping(target = "messageBody", source = "messageBody", qualifiedByName = "readMessageBody")
    TaskEntity toEntity(TaskPO source);

    /**
     * 批量将消息任务持久化对象转换为领域实体。
     *
     * @param sources 消息任务持久化对象列表
     * @return 消息任务领域实体列表
     * @throws NullPointerException 当 {@code sources} 为空时抛出
     */
    List<TaskEntity> toEntities(List<TaskPO> sources);

    /**
     * 将消息任务领域实体转换为待新增的持久化对象。
     *
     * @param source 消息任务领域实体
     * @return 忽略数据库生成字段后的消息任务持久化对象
     * @throws NullPointerException 当 {@code source} 为空时抛出
     * @throws BusinessException    当发奖事件无法序列化为 JSON 时抛出
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "messageBody", source = "messageBody", qualifiedByName = "writeMessageBody")
    TaskPO toPO(TaskEntity source);

    /**
     * 批量将消息任务领域实体转换为待新增的持久化对象。
     *
     * @param sources 消息任务领域实体列表
     * @return 忽略数据库生成字段后的消息任务持久化对象列表
     * @throws NullPointerException 当 {@code sources} 为空时抛出
     */
    List<TaskPO> toPOs(List<TaskEntity> sources);

    @Named("writeMessageBody")
    default String writeMessageBody(SendAwardRecordEvent messageBody) {
        try {
            return JSONUtil.toJsonStr(messageBody);
        } catch (JSONException exception) {
            throw BusinessException.of(BusinessResponseCode.MESSAGE_FORMAT_INVALID, exception, messageBody.eventId(), messageBody.eventType());
        }
    }

    @Named("readMessageBody")
    default SendAwardRecordEvent readMessageBody(String messageBody) {
        try {
            JSONObject jsonObject = JSONUtil.parseObj(messageBody);
            return SendAwardRecordEvent.builder()
                    .eventId(jsonObject.getStr("eventId"))
                    .occurredAt(parseOccurredAt(jsonObject.getStr("occurredAt")))
                    .userId(jsonObject.getStr("userId"))
                    .awardId(jsonObject.getLong("awardId"))
                    .awardTitle(jsonObject.getStr("awardTitle"))
                    .build();
        } catch (JSONException | DateTimeException | NumberFormatException exception) {
            throw BusinessException.of(BusinessResponseCode.MESSAGE_FORMAT_INVALID, exception);
        }
    }

    default Instant parseOccurredAt(String occurredAt) {
        if (occurredAt.chars().allMatch(Character::isDigit)) {
            return Instant.ofEpochMilli(Long.parseLong(occurredAt));
        }
        return Instant.parse(occurredAt);
    }
}
