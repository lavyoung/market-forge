package com.lavyoung.marketforge.types.utils;

import org.slf4j.MDC;

/**
 *
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/21
 */
public final class MdcUtil {

    private static final String TRACE_ID_KEY = "traceId";

    public static String getTraceId() {
        return MDC.get(TRACE_ID_KEY);
    }

    public static String getTraceIdKey() {
        return TRACE_ID_KEY;
    }

    /**
     * 把 traceId 写入当前线程的日志上下文。
     */
    public static void putTraceId(String traceId) {
        if (traceId != null) {
            MDC.put(TRACE_ID_KEY, traceId);
        }
    }

    /**
     * 清除当前线程的 traceId，避免线程池复用导致串味。
     */
    public static void clearTraceId() {
        MDC.remove(TRACE_ID_KEY);
    }

    private MdcUtil() {
    }

}
