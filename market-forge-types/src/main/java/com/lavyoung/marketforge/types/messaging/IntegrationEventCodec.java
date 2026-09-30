package com.lavyoung.marketforge.types.messaging;

/**
 * 通用集成事件编解码器。
 * <p>
 * 负责在本地消息任务表的字符串消息体和运行期事件对象之间转换。该接口位于 types 模块，
 * 便于应用层依赖抽象，具体 JSON 工具和事件类型注册由基础设施模块实现。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/30
 */
public interface IntegrationEventCodec {

    /**
     * 将集成事件序列化为可持久化的消息体。
     *
     * @param event 集成事件
     * @return 可写入任务表的消息体
     * @throws RuntimeException 事件无法序列化时抛出
     */
    String serialize(IntegrationEvent event);

    /**
     * 按事件类型将消息体反序列化为集成事件。
     *
     * @param eventType   事件类型或路由键
     * @param messageBody 任务表中的消息体
     * @return 反序列化后的集成事件
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 事件类型不支持或消息格式非法时抛出
     */
    IntegrationEvent deserialize(String eventType, String messageBody);
}
