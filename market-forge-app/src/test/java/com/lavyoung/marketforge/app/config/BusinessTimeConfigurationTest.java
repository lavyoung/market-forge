package com.lavyoung.marketforge.app.config;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 业务时间配置测试。
 */
class BusinessTimeConfigurationTest {

    @Test
    void shouldCreateAsiaShanghaiBusinessClock() {
        // Given
        BusinessTimeConfiguration configuration = new BusinessTimeConfiguration();

        // When
        Clock clock = configuration.businessClock();

        // Then
        assertEquals(ZoneId.of("Asia/Shanghai"), clock.getZone());
    }
}
