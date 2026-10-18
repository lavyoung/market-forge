package com.lavyoung.marketforge.app;

import com.lavyoung.marketforge.application.activity.model.ActivityAwardListCommand;
import com.lavyoung.marketforge.application.activity.model.ActivityAwardResult;
import com.lavyoung.marketforge.application.activity.service.IActivityRaffleApplicationService;
import com.lavyoung.marketforge.types.common.Constants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 使用开发环境基础设施验证活动奖品展示查询。
 * <p>
 * 测试会删除指定活动的一条详情缓存，强制执行普通表数据库回源，随后验证活动配置、
 * 策略奖品和用户解锁状态能够被应用服务完整聚合。该过程只读业务数据，不创建抽奖
 * 订单，也不扣减用户次数或奖品库存。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0-SNAPSHOT
 */
@SpringBootTest(classes = MarketForgeApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named = "RUN_DEV_INTEGRATION_TESTS", matches = "true")
class ActivityAwardListDevIntegrationTest {

    private static final String USER_ID = "user-001";
    private static final long ACTIVITY_ID = 100_301L;

    @Autowired
    private IActivityRaffleApplicationService activityRaffleApplicationService;

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 验证缓存缺失时能够从普通活动表回源，并返回可供活动页展示的奖品列表。
     */
    @Test
    void shouldQueryActivityAwardListAfterDatabaseFallback() {
        redissonClient.getBucket(Constants.RedisKeys.ACTIVITY_DETAIL_KEY + ACTIVITY_ID).delete();

        List<ActivityAwardResult> results = activityRaffleApplicationService.queryAwardList(
                new ActivityAwardListCommand(USER_ID, ACTIVITY_ID)
        );

        assertFalse(results.isEmpty());
        results.forEach(result -> assertAll(
                () -> assertNotNull(result.awardId()),
                () -> assertNotNull(result.awardTitle()),
                () -> assertEquals("total", result.awardUnlockScope())
        ));
    }
}
