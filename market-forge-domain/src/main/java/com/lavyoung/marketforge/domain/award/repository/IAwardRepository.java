package com.lavyoung.marketforge.domain.award.repository;

import com.lavyoung.marketforge.domain.award.model.aggreate.UserAwardRecordAggregate;

/**
 *
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/23
 */
public interface IAwardRepository {

    void saveUserAwardRecord(UserAwardRecordAggregate aggregate);
}
