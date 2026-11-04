package com.lavyoung.marketforge.domain.activity.service;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivityDefinitionEntity;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivityReadinessReportVO;

import java.time.LocalDateTime;

/**
 * 活动定义开放就绪检查器。
 *
 * <p>该领域服务负责检查一个活动定义及其关联的次数配置、活动 SKU 和抽奖策略，
 * 判断活动是否具备从草稿状态切换为开放状态的完整业务条件。</p>
 *
 * <p>检查结果使用报告形式返回，而不是遇到第一个配置错误就抛出异常。这样运营人员
 * 可以一次看到活动当前存在的全部阻断错误、风险提示和配置建议，减少反复修改和检查。</p>
 *
 * <p>该检查器只执行只读校验，不修改活动状态、不预热抽奖策略、不写入 Redis，
 * 也不切换活动展台。真正的状态迁移和发布编排仍由 Application Service 负责。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/04
 */
public interface IActivityDefinitionReadinessChecker {

    /**
     * 检查活动是否满足开放条件。
     *
     * <p>实现需要完成以下检查：</p>
     *
     * <ol>
     *     <li>活动结束时间是否晚于本次检查时间。</li>
     *     <li>活动引用的次数配置是否存在。</li>
     *     <li>活动是否至少配置一个可用 SKU。</li>
     *     <li>每个 SKU 的活动归属、库存和积分价格是否满足开放要求。</li>
     *     <li>活动引用的抽奖策略是否存在且允许被活动引用。</li>
     *     <li>抽奖策略的奖品、概率空间和规则配置是否通过策略就绪检查。</li>
     * </ol>
     *
     * <p>业务配置问题应转换成 {@link ActivityReadinessReportVO} 中的错误项；
     * 数据库不可用等系统异常继续向上抛出，不能伪装成普通运营配置问题。</p>
     *
     * @param definition 待检查的活动定义
     * @param checkedAt  本次检查采用的业务时间
     * @return 包含错误、警告和建议的活动就绪检查报告
     * @throws NullPointerException 活动定义或检查时间为空时抛出
     * @throws RuntimeException     Repository 或其他系统依赖发生故障时抛出
     */
    ActivityReadinessReportVO check(ActivityDefinitionEntity definition, LocalDateTime checkedAt);
}
