package com.lavyoung.marketforge.application.award.service;

import com.lavyoung.marketforge.application.award.model.AwardDefinitionResult;
import com.lavyoung.marketforge.application.award.model.ChangeAwardDefinitionStateCommand;
import com.lavyoung.marketforge.application.award.model.CreateAwardDefinitionCommand;
import com.lavyoung.marketforge.application.award.model.UpdateAwardDefinitionCommand;

/**
 * 奖品定义应用服务。
 *
 * <p>负责运营后台奖品定义的创建、查询、草稿修改和生命周期状态变更。
 * 应用服务负责编排用例，奖品生命周期合法性由领域状态对象判断，
 * 持久化并发安全由仓储端口负责。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/21
 */
public interface IAwardDefinitionApplicationService {

    /**
     * 创建草稿奖品定义。
     *
     * @param command 创建奖品命令
     * @return 创建后的奖品定义
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 配置非法、标识重复或数据库写入失败时抛出
     */
    AwardDefinitionResult create(CreateAwardDefinitionCommand command);

    /**
     * 查询指定奖品定义。
     *
     * @param awardId 奖品业务标识
     * @return 奖品定义
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 奖品不存在时抛出
     */
    AwardDefinitionResult queryByAwardId(Long awardId);

    /**
     * 修改草稿奖品定义。
     *
     * @param command 修改草稿命令
     * @return 修改后的奖品定义
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 奖品不存在、状态不允许修改或版本冲突时抛出
     */
    AwardDefinitionResult updateDraft(UpdateAwardDefinitionCommand command);

    /**
     * 变更奖品定义生命周期状态。
     *
     * @param command 状态变更命令
     * @return 状态变更后的奖品定义
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 奖品不存在、状态流转非法或版本冲突时抛出
     */
    AwardDefinitionResult changeState(ChangeAwardDefinitionStateCommand command);
}
