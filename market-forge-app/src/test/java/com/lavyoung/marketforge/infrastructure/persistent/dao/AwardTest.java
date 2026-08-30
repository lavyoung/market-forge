package com.lavyoung.marketforge.infrastructure.persistent.dao;


import com.lavyoung.marketforge.infrastructure.persistent.po.AwardPO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Optional;

/**
 * 验证奖品数据访问接口与 MyBatis 映射配置。
 *
 * @author lavyoung
 * @version 1.0.0-SNAPSHOT
 * @email lavyoung1325@outlook.com
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class AwardTest {

    /** 被测奖品数据访问接口。 */
    @Resource
    private IAwardDao iAwardDao;

    /**
     * 验证不存在的奖品业务标识返回空结果。
     */
    @Test
    public void test_queryAwardList() {
        Optional<AwardPO> awardPO = iAwardDao.queryAwardByAwardKey("1");
        Assert.assertNotEquals(awardPO.isPresent(), Boolean.TRUE);
    }
}
