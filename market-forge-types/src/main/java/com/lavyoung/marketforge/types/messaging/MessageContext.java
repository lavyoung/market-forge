package com.lavyoung.marketforge.types.messaging;

import java.util.Map;

/**
 *
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/21
 */
public record MessageContext(
        String messageId,
        String traceId,
        boolean redelivered,
        Map<String, Object> headers
) {

}
