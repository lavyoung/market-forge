import {request} from '@umijs/max';
import {getAdminCsrfToken} from '../auth/auth';
import type {ApiResponse, PageResult} from '../types';
import {requireResponseData} from '../types';
import type {
    StrategyDefinition,
    StrategyDefinitionEditorValues,
    StrategyDefinitionPage,
    StrategyDefinitionPayload,
    StrategyDefinitionQuery,
    StrategyDefinitionState,
    StrategyDefinitionSummary,
    StrategyRuleEditorValues,
    StrategyRulePayload,
} from './types';

/** 策略定义运营管理接口地址。 */
const STRATEGY_DEFINITION_PATH = '/api/admin/strategies';

/**
 * 分页查询策略定义摘要。
 *
 * @param params ProTable 分页参数和策略筛选条件
 * @returns ProTable 可以直接消费的分页结果
 */
export async function queryStrategyDefinitions(params: StrategyDefinitionQuery): Promise<StrategyDefinitionPage> {
    const response = await request<ApiResponse<PageResult<StrategyDefinitionSummary>>>(STRATEGY_DEFINITION_PATH, {
        method: 'GET',
        params: {
            strategyId: normalizeText(params.strategyId),
            strategyDesc: normalizeText(params.strategyDesc),
            state: params.state,
            pageNumber: params.current ?? 1,
            pageSize: params.pageSize ?? 20,
        },
        withCredentials: true,
    });
    const pageResult = requireResponseData(response);
    return {data: pageResult.records, total: pageResult.total, success: true};
}

/**
 * 查询包含奖品和规则的完整策略定义。
 *
 * @param strategyId 策略业务标识
 * @returns 完整策略定义
 */
export async function getStrategyDefinition(strategyId: string): Promise<StrategyDefinition> {
    const response = await request<ApiResponse<StrategyDefinition>>(
        `${STRATEGY_DEFINITION_PATH}/${encodeURIComponent(strategyId)}`,
        {method: 'GET', withCredentials: true},
    );
    return requireResponseData(response);
}

/**
 * 创建策略草稿。
 *
 * @param values 结构化策略编辑字段
 * @returns 创建后的完整策略定义
 */
export async function createStrategyDefinition(values: StrategyDefinitionEditorValues): Promise<StrategyDefinition> {
    const response = await writeStrategyDefinition<StrategyDefinition>(STRATEGY_DEFINITION_PATH, 'POST', toPayload(values));
    return requireResponseData(response);
}

/**
 * 修改策略草稿完整配置。
 *
 * @param strategyId 策略业务标识
 * @param expectedVersion 页面最近读取到的乐观锁版本
 * @param values 结构化策略编辑字段
 * @returns 修改后的完整策略定义
 */
export async function updateStrategyDefinition(strategyId: string, expectedVersion: number, values: StrategyDefinitionEditorValues): Promise<StrategyDefinition> {
    const response = await writeStrategyDefinition<StrategyDefinition>(
        `${STRATEGY_DEFINITION_PATH}/${encodeURIComponent(strategyId)}`,
        'PUT',
        {...toPayload(values), expectedVersion},
    );
    return requireResponseData(response);
}

/**
 * 将历史策略复制成新的独立草稿。
 *
 * @param strategyId 来源策略业务标识
 * @returns 新生成的策略草稿
 */
export async function copyStrategyDefinition(strategyId: string): Promise<StrategyDefinition> {
    const response = await writeStrategyDefinition<StrategyDefinition>(
        `${STRATEGY_DEFINITION_PATH}/${encodeURIComponent(strategyId)}/copies`,
        'POST',
    );
    return requireResponseData(response);
}

/**
 * 发布或停用策略定义。
 *
 * @param strategyId 策略业务标识
 * @param targetState 目标生命周期状态
 * @param expectedVersion 页面最近读取到的乐观锁版本
 * @returns 状态变更后的完整策略定义
 */
export async function changeStrategyDefinitionState(strategyId: string, targetState: Exclude<StrategyDefinitionState, 'draft'>, expectedVersion: number): Promise<StrategyDefinition> {
    const response = await writeStrategyDefinition<StrategyDefinition>(
        `${STRATEGY_DEFINITION_PATH}/${encodeURIComponent(strategyId)}/state`,
        'PATCH',
        {targetState, expectedVersion},
    );
    return requireResponseData(response);
}

async function writeStrategyDefinition<T>(path: string, method: 'POST' | 'PUT' | 'PATCH', data?: unknown): Promise<ApiResponse<T>> {
    const csrfToken = await getAdminCsrfToken();
    return request<ApiResponse<T>>(path, {
        method,
        data,
        headers: {[csrfToken.headerName]: csrfToken.token},
        withCredentials: true,
    });
}

/**
 * 将页面数组型规则字段转换成后端分隔字符串契约。
 *
 * @param values 页面结构化编辑字段
 * @returns 可直接提交给后端的策略完整快照
 */
function toPayload(values: StrategyDefinitionEditorValues): StrategyDefinitionPayload {
    return {
        strategyDesc: values.strategyDesc.trim(),
        ruleModels: joinModels(values.ruleModels, ','),
        awards: values.awards.map((award) => ({
            ...award,
            awardTitle: award.awardTitle.trim(),
            awardSubtitle: normalizeText(award.awardSubtitle),
            ruleModels: joinModels(award.ruleModels, '/'),
        })),
        rules: values.rules.map(toRulePayload),
    };
}

/**
 * 将规则编辑字段转换为与规则模型匹配的结构化写请求。
 *
 * @param rule 页面中的单条规则配置
 * @returns 仅携带当前规则所需字段的 API 请求
 */
function toRulePayload(rule: StrategyRuleEditorValues): StrategyRulePayload {
    const ruleDesc = rule.ruleDesc.trim();
    switch (rule.ruleModel) {
        case 'rule_blacklist':
            return {
                ruleModel: rule.ruleModel,
                blacklist: {
                    fallbackAwardId: normalizeText(rule.blacklistFallbackAwardId) ?? '',
                    userIds: rule.blacklistUserIds?.map((userId) => userId.trim()).filter(Boolean) ?? [],
                },
                ruleDesc,
            };
        case 'rule_weight':
            return {
                ruleModel: rule.ruleModel,
                weight: {
                    tiers: rule.weightTiers?.map((tier) => ({
                        threshold: tier.threshold,
                        awardIds: tier.awardIds.map((awardId) => awardId.trim()),
                    })) ?? [],
                },
                ruleDesc,
            };
        case 'rule_lock':
            return {
                awardId: normalizeText(rule.awardId),
                ruleModel: rule.ruleModel,
                lock: {requiredCount: rule.lockRequiredCount ?? 0},
                ruleDesc,
            };
    }
}

function joinModels(models: string[] | undefined, separator: ',' | '/'): string | undefined {
    const normalized = models?.map((model) => model.trim()).filter(Boolean);
    return normalized?.length ? normalized.join(separator) : undefined;
}

function normalizeText(value?: string): string | undefined {
    const normalized = value?.trim();
    return normalized || undefined;
}
