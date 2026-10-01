package com.lavyoung.marketforge.application.activity.model;

import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.CommonResponseCode;
import org.apache.commons.lang3.StringUtils;

/**
 * 活动奖品列表查询命令。
 * <p>
 * 应用层使用该命令承接接口层入参，并在进入活动奖品展示用例前完成基础校验。
 * 查询结果与用户当日参与次数有关，因此需要同时携带用户标识和活动标识。
 *
 * @param userId     用户标识
 * @param activityId 抽奖活动标识
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/01
 */
public record ActivityAwardListCommand(
        String userId,
        Long activityId
) {

    /**
     * 校验活动奖品列表查询命令。
     *
     * @throws BusinessException 用户标识为空白、活动标识为空或非正数时抛出
     */
    public ActivityAwardListCommand {
        if (StringUtils.isBlank(userId)) {
            throw BusinessException.of(CommonResponseCode.PARAM_INVALID, "userId");
        }
        if (activityId == null || activityId <= 0) {
            throw BusinessException.of(CommonResponseCode.PARAM_INVALID, "activityId");
        }
    }
}
