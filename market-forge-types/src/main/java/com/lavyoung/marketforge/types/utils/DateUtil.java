package com.lavyoung.marketforge.types.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日期时间工具类。
 * <p>
 * 统一提供项目常用日期格式化器和当前时间获取入口，避免业务代码直接散落格式常量。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/22
 */
public final class DateUtil {

    /**
     * 年月日时分秒格式化器。
     */
    public static final DateTimeFormatter yyyy_MM_dd_HH_mm_ss = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 年月日格式化器。
     */
    public static final DateTimeFormatter yyyy_MM_dd = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 年月格式化器。
     */
    public static final DateTimeFormatter yyyy_MM = DateTimeFormatter.ofPattern("yyyy-MM");

    private DateUtil() {

    }


    /**
     * 获取当前本地日期时间。
     *
     * @return 当前本地日期时间
     */
    public static LocalDateTime now() {
        return LocalDateTime.now();
    }
}
