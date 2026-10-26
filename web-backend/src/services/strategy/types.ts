/**
 * 策略定义生命周期状态。
 */
export type StrategyDefinitionState = 'draft' | 'active' | 'disabled';

/** 支持运营端结构化编辑的规则模型。 */
export type ConfigurableRuleModel = 'rule_blacklist' | 'rule_weight' | 'rule_lock';

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

/** 策略发布就绪检查发现的问题。 */
export interface StrategyReadinessIssue {
    /** 稳定的问题编码。 */
    code: string;
    /** 问题对应的策略配置路径。 */
    path: string;
    /** 面向运营人员的问题说明。 */
    message: string;
}

/** 奖品在整数概率空间中的命中区间。 */
export interface StrategyProbabilityInterval {
    /** 奖品业务标识。 */
    awardId: string;
    /** 约简后的整数权重。 */
    weight: number;
    /** 左闭区间起点。 */
    startInclusive: number;
    /** 右开区间终点。 */
    endExclusive: number;
    /** 理论命中概率。 */
    probability: number;
}

/** 策略整数概率空间预览。 */
export interface StrategyProbabilityPreview {
    /** 是否成功生成概率预览。 */
    available: boolean;
    /** 概率空间总整数权重。 */
    totalWeight: number;
    /** 按正式抽奖顺序排列的奖品区间。 */
    intervals: StrategyProbabilityInterval[];
}

/** 策略发布就绪检查结果。 */
export interface StrategyReadiness {
    /** 策略业务标识。 */
    strategyId: string;
    /** 是否满足发布条件。 */
    ready: boolean;
    /** 阻止发布的错误。 */
    errors: StrategyReadinessIssue[];
    /** 不阻止发布的风险提示。 */
    warnings: StrategyReadinessIssue[];
    /** 配置优化建议。 */
    suggestions: StrategyReadinessIssue[];
    /** 正式抽奖概率空间预览。 */
    probabilityPreview: StrategyProbabilityPreview;
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
    /** 关联奖品；仅次数锁规则填写。 */
    awardId?: string;
    /** 当前编辑的规则模型。 */
    ruleModel: ConfigurableRuleModel;
    /** 黑名单命中后的兜底奖品。 */
    blacklistFallbackAwardId?: string;
    /** 黑名单用户标识。 */
    blacklistUserIds?: string[];
    /** 权重奖池次数档位。 */
    weightTiers?: WeightTierEditorValues[];
    /** 次数锁要求的最低完成次数。 */
    lockRequiredCount?: number;
    /** 运营说明。 */
    ruleDesc: string;
}

/**
 * 权重奖池的单个次数档位编辑字段。
 */
export interface WeightTierEditorValues {
    /** 进入当前子奖池所需的最低累计次数。 */
    threshold: number;
    /** 当前档位包含的策略奖品标识。 */
    awardIds: string[];
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
    rules: StrategyRulePayload[];
}

/**
 * 服务端结构化策略规则写请求。
 */
export interface StrategyRulePayload {
    awardId?: string;
    ruleModel: ConfigurableRuleModel;
    blacklist?: { fallbackAwardId: string; userIds: string[] };
    weight?: { tiers: WeightTierEditorValues[] };
    lock?: { requiredCount: number };
    ruleDesc: string;
}
