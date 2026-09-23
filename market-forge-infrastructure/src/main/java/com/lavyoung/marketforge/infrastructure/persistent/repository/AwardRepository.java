package com.lavyoung.marketforge.infrastructure.persistent.repository;

import com.lavyoung.marketforge.domain.award.model.aggreate.UserAwardRecordAggregate;
import com.lavyoung.marketforge.domain.award.model.entity.TaskEntity;
import com.lavyoung.marketforge.domain.award.model.entity.UserAwardRecordEntity;
import com.lavyoung.marketforge.domain.award.repository.IAwardRepository;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.TaskAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.UserAwardRecordAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.dao.IUserAwardRecordDao;
import com.lavyoung.marketforge.infrastructure.persistent.dao.mq.ITaskDao;
import com.lavyoung.marketforge.infrastructure.persistent.po.UserAwardRecordPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

/**
 *
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/23
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class AwardRepository implements IAwardRepository {

    private final IUserAwardRecordDao userAwardRecordDao;
    private final ITaskDao taskDao;

    private final UserAwardRecordAssembler userAwardRecordAssembler;
    private final TaskAssembler taskAssembler;


    @Override
    public void saveUserAwardRecord(UserAwardRecordAggregate aggregate) {
        TaskEntity taskEntity = aggregate.taskEntity();
        UserAwardRecordEntity userAwardRecordEntity = aggregate.userAwardRecordEntity();

        // 保存发奖记录
        UserAwardRecordPO po = userAwardRecordAssembler.toPO(userAwardRecordEntity);
        userAwardRecordDao.insert(po);

        // 发出mq消息记录


    }
}
