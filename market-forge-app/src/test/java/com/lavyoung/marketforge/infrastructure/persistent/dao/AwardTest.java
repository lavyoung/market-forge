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

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class AwardTest {

    @Resource
    private IAwardDao iAwardDao;

    @Test
    public void test_queryAwardList() {
        Optional<AwardPO> awardPO = iAwardDao.queryAwardByAwardKey("1");
        Assert.assertNotEquals(awardPO.isPresent(), Boolean.TRUE);
    }
}
