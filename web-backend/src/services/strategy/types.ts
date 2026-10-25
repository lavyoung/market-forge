/**
 * 策略定义生命周期状态。
 */
export type StrategyDefinitionState = 'draft' | 'active' | 'disabled';

/**
 * 策略定义列表摘要。
 */
export interface StrategyDefinitionSummary {
    /** 策略业务标识，使用字符串避免 JavaScript 整数精度丢失。 */
    strategyId: string;
    /** 策略运营描述。 */
    strategyDesc: string;
    /** 使用逗号分隔的策略级规则模型。 */
    ruleModels: string | null;
    /** 策略生命周期状态。 */
    state: StrategyDefinitionState;
    /** 乐观锁版本。 */
    version: number;
    /** 创建人。 */
    createdBy: string;
    /** 最后修改人。 */
    updatedBy: string;
    /** 创建时间。 */
    createTime: string;
    /** 最后修改时间。 */
    updateTime: string;
}

/**
 * 策略奖品配置。
 */
export interface StrategyAwardConfig {
    /** 引用的奖品业务标识。 */
    awardId: string;
    /** 当前策略中的奖品标题。 */
    awardTitle: string;
    /** 奖品副标题。 */
    awardSubtitle: string | null;
    /** 配置库存总量。 */
    awardCount: number;
    /** 当前剩余库存。 */
    awardCountSurplus: number;
    /** 概率权重。 */
    awardRate: number;
    /** 使用斜杠分隔的奖品级规则模型。 */
    ruleModels: string | null;
    /** 奖品展示顺序。 */
    sort: number;
    /** 创建时间。 */
    createTime: string;
    /** 最后修改时间。 */
    updateTime: string;
}

/**
 * 策略规则配置。
 */
export interface StrategyRuleConfig {
    /** 奖品业务标识；策略级规则为空。 */
    awardId: string | null;
    /** 规则类型：1-策略规则，2-奖品规则。 */
    ruleType: 1 | 2;
    /** 规则模型编码。 */
    ruleModel: string;
    /** 规则配置值。 */
    ruleValue: string;
    /** 规则运营说明。 */
    ruleDesc: string;
    /** 创建时间。 */
    createTime: string;
    /** 最后修改时间。 */
    updateTime: string;
}

/**
 * 策略完整定义。
 */
export interface StrategyDefinition extends StrategyDefinitionSummary {
    /** 完整策略奖品集合。 */
    awards: StrategyAwardConfig[];
    /** 完整策略规则集合。 */
    rules: StrategyRuleConfig[];
}

/**
 * ProTable 提交的策略查询条件。
 */
export interface StrategyDefinitionQuery {
    current?: number;
    pageSize?: number;
    strategyId?: string;
    strategyDesc?: string;
    state?: StrategyDefinitionState;
}

/**
 * ProTable 所需的策略分页结果。
 */
export interface StrategyDefinitionPage {
    data: StrategyDefinitionSummary[];
    total: number;
    success: boolean;
}

/**
 * 策略奖品编辑字段。
 */
export interface StrategyAwardEditorValues {
    awardId: string;
    awardTitle: string;
    awardSubtitle?: string;
    awardCount: number;
    awardRate: number;
    ruleModels?: string[];
    sort: number;
}

/**
 * 策略规则编辑字段。
 */
export interface StrategyRuleEditorValues {
    awardId?: string;
    ruleType: 1 | 2;
    ruleModel: string;
    ruleValue: string;
    ruleDesc: string;
}

/**
 * 创建或编辑策略定义的结构化表单字段。
 */
export interface StrategyDefinitionEditorValues {
    strategyDesc: string;
    ruleModels?: string[];
    awards: StrategyAwardEditorValues[];
    rules: StrategyRuleEditorValues[];
}

/**
 * 服务端策略定义写请求。
 */
export interface StrategyDefinitionPayload {
    strategyDesc: string;
    ruleModels?: string;
    awards: Array<Omit<StrategyAwardEditorValues, 'ruleModels'> & { ruleModels?: string }>;
    rules: StrategyRuleEditorValues[];
}
