import {queryAwardDefinitions} from '@/services/award/award-definition';
import {
    changeStrategyDefinitionState,
    checkStrategyReadiness,
    copyStrategyDefinition,
    createStrategyDefinition,
    getStrategyDefinition,
    queryStrategyDefinitions,
    updateStrategyDefinition,
} from '@/services/strategy/strategy-definition';
import type {
    StrategyAwardConfig,
    StrategyDefinition,
    StrategyDefinitionEditorValues,
    StrategyDefinitionQuery,
    StrategyDefinitionState,
    StrategyDefinitionSummary,
    StrategyProbabilityInterval,
    StrategyReadiness,
    StrategyReadinessIssue,
    StrategyRuleConfig,
    StrategyRuleEditorValues,
} from '@/services/strategy/types';
import {getApiErrorMessage} from '@/services/errors';
import {
    CopyOutlined,
    EditOutlined,
    EyeOutlined,
    PlusOutlined,
    RocketOutlined,
    SafetyCertificateOutlined,
    StopOutlined
} from '@ant-design/icons';
import type {ActionType, ProColumns} from '@ant-design/pro-components';
import {
    ModalForm,
    PageContainer,
    ProForm,
    ProFormDependency,
    ProFormDigit,
    ProFormList,
    ProFormSelect,
    ProFormText,
    ProTable,
} from '@ant-design/pro-components';
import type {ColumnsType} from 'antd/es/table';
import {Alert, Button, Descriptions, Drawer, Empty, message, Modal, Space, Table, Tag, Typography} from 'antd';
import {useEffect, useRef, useState} from 'react';

type SelectOption = { label: string; value: string };

const stateLabels: Record<StrategyDefinitionState, { color: string; text: string }> = {
    draft: {color: 'default', text: '草稿'},
    active: {color: 'success', text: '已生效'},
    disabled: {color: 'error', text: '已停用'},
};

const strategyRuleModelOptions = [
    {label: '黑名单规则', value: 'rule_blacklist'},
    {label: '权重规则', value: 'rule_weight'},
];

const awardRuleModelOptions = [
    {label: '次数锁规则', value: 'rule_lock'},
    {label: '库存规则', value: 'rule_stock'},
];

const ruleConfigModelOptions = [
    ...strategyRuleModelOptions,
    {label: '次数锁规则', value: 'rule_lock'},
];

const positiveIdRules = [
    {required: true, message: '请选择奖品'},
    {pattern: /^[1-9][0-9]{0,18}$/, message: '奖品标识格式不合法'},
];

/**
 * 策略定义运营页面。
 *
 * <p>列表使用轻量摘要接口，只有查看和编辑时才加载奖品、规则完整聚合。
 * 草稿可编辑和发布，历史策略通过复制生成新草稿，避免原地修改运行中配置。</p>
 *
 * @returns 策略列表、详情、编辑器和生命周期操作界面
 */
function StrategyDefinitionPage() {
    const actionRef = useRef<ActionType>();
    const [awardOptions, setAwardOptions] = useState<SelectOption[]>([]);
    const [awardOptionsLoading, setAwardOptionsLoading] = useState(true);
    const [detailStrategy, setDetailStrategy] = useState<StrategyDefinition>();
    const [detailLoading, setDetailLoading] = useState(false);
    const [editorOpen, setEditorOpen] = useState(false);
    const [editingStrategy, setEditingStrategy] = useState<StrategyDefinition>();
    const [readinessOpen, setReadinessOpen] = useState(false);
    const [readinessLoading, setReadinessLoading] = useState(false);
    const [readinessStrategy, setReadinessStrategy] = useState<StrategyDefinitionSummary>();
    const [readinessReport, setReadinessReport] = useState<StrategyReadiness>();

    useEffect(() => {
        void loadActiveAwardOptions();
    }, []);

    const loadActiveAwardOptions = async () => {
        setAwardOptionsLoading(true);
        try {
            const page = await queryAwardDefinitions({current: 1, pageSize: 100, state: 'active'});
            setAwardOptions(page.data.map((award) => ({
                label: `${award.awardName} (${award.awardId})`,
                value: award.awardId
            })));
        } catch (error) {
            message.error(getApiErrorMessage(error, '已启用奖品加载失败'));
        } finally {
            setAwardOptionsLoading(false);
        }
    };

    const openCreateEditor = () => {
        setEditingStrategy(undefined);
        setEditorOpen(true);
    };

    const openDetail = async (strategy: StrategyDefinitionSummary) => {
        setDetailLoading(true);
        try {
            setDetailStrategy(await getStrategyDefinition(strategy.strategyId));
        } catch (error) {
            message.error(getApiErrorMessage(error, '策略详情加载失败'));
        } finally {
            setDetailLoading(false);
        }
    };

    const openEditEditor = async (strategy: StrategyDefinitionSummary) => {
        try {
            const definition = await getStrategyDefinition(strategy.strategyId);
            setEditingStrategy(definition);
            setAwardOptions((current) => mergeReferencedAwards(current, definition.awards));
            setEditorOpen(true);
        } catch (error) {
            message.error(getApiErrorMessage(error, '策略详情加载失败'));
        }
    };

    const submitDefinition = async (values: StrategyDefinitionEditorValues): Promise<boolean> => {
        try {
            if (editingStrategy) {
                await updateStrategyDefinition(editingStrategy.strategyId, editingStrategy.version, values);
                message.success('策略草稿已更新');
            } else {
                await createStrategyDefinition(values);
                message.success('策略草稿已创建');
            }
            await actionRef.current?.reload();
            return true;
        } catch (error) {
            message.error(getApiErrorMessage(error, editingStrategy ? '策略草稿更新失败' : '策略草稿创建失败'));
            return false;
        }
    };

    const confirmCopy = (strategy: StrategyDefinitionSummary) => {
        Modal.confirm({
            title: '复制策略为新草稿',
            content: `确认复制策略“${strategy.strategyDesc}”？系统会生成新的策略 ID，原策略保持不变。`,
            okText: '复制',
            cancelText: '取消',
            async onOk() {
                try {
                    const copied = await copyStrategyDefinition(strategy.strategyId);
                    message.success(`已生成新草稿 ${copied.strategyId}`);
                    await actionRef.current?.reload();
                } catch (error) {
                    message.error(getApiErrorMessage(error, '策略复制失败'));
                    throw error;
                }
            },
        });
    };

    const confirmStateChange = (strategy: StrategyDefinitionSummary, targetState: 'active' | 'disabled') => {
        const publishing = targetState === 'active';
        Modal.confirm({
            title: publishing ? '发布策略' : '停用策略',
            content: publishing
                ? `确认发布“${strategy.strategyDesc}”？系统将校验奖池、规则和概率配置并刷新运行时快照。`
                : `确认停用“${strategy.strategyDesc}”？该策略将不能再被新活动引用。`,
            okText: publishing ? '发布' : '停用',
            cancelText: '取消',
            okButtonProps: {danger: !publishing},
            async onOk() {
                try {
                    await changeStrategyDefinitionState(strategy.strategyId, targetState, strategy.version);
                    message.success(publishing ? '策略已发布' : '策略已停用');
                    await actionRef.current?.reload();
                } catch (error) {
                    message.error(getApiErrorMessage(error, publishing ? '策略发布失败' : '策略停用失败'));
                    throw error;
                }
            },
        });
    };

    const openReadiness = async (strategy: StrategyDefinitionSummary) => {
        setReadinessOpen(true);
        setReadinessLoading(true);
        setReadinessStrategy(strategy);
        setReadinessReport(undefined);
        try {
            setReadinessReport(await checkStrategyReadiness(strategy.strategyId));
        } catch (error) {
            message.error(getApiErrorMessage(error, '策略发布检查失败'));
        } finally {
            setReadinessLoading(false);
        }
    };

    const columns: ProColumns<StrategyDefinitionSummary>[] = [
        {title: '策略 ID', dataIndex: 'strategyId', copyable: true, width: 160},
        {title: '策略描述', dataIndex: 'strategyDesc', ellipsis: true, width: 220},
        {
            title: '状态',
            dataIndex: 'state',
            valueType: 'select',
            width: 110,
            valueEnum: {
                draft: {text: '草稿', status: 'Default'},
                active: {text: '已生效', status: 'Success'},
                disabled: {text: '已停用', status: 'Error'},
            },
            render: (_, record) => <Tag color={stateLabels[record.state].color}>{stateLabels[record.state].text}</Tag>,
        },
        {
            title: '策略规则',
            dataIndex: 'ruleModels',
            hideInSearch: true,
            width: 260,
            render: (_, record) => <RuleModelTags value={record.ruleModels}/>,
        },
        {title: '版本', dataIndex: 'version', hideInSearch: true, width: 80},
        {title: '修改人', dataIndex: 'updatedBy', hideInSearch: true, ellipsis: true, width: 120},
        {title: '更新时间', dataIndex: 'updateTime', valueType: 'dateTime', hideInSearch: true, width: 170},
        {
            title: '操作',
            valueType: 'option',
            fixed: 'right',
            width: 330,
            render: (_, record) => [
                <Button key="detail" type="link" icon={<EyeOutlined/>} onClick={() => openDetail(record)}>查看</Button>,
                record.state === 'draft' ? (
                    <Button key="edit" type="link" icon={<EditOutlined/>}
                            onClick={() => openEditEditor(record)}>编辑</Button>
                ) : null,
                record.state === 'draft' ? (
                    <Button key="readiness" type="link" icon={<SafetyCertificateOutlined/>}
                            onClick={() => openReadiness(record)}>检查并发布</Button>
                ) : null,
                record.state !== 'draft' ? (
                    <Button key="copy" type="link" icon={<CopyOutlined/>}
                            onClick={() => confirmCopy(record)}>复制</Button>
                ) : null,
                record.state === 'active' ? (
                    <Button key="disable" type="link" danger icon={<StopOutlined/>}
                            onClick={() => confirmStateChange(record, 'disabled')}>停用</Button>
                ) : null,
            ],
        },
    ];

    return (
        <PageContainer title="策略定义" subTitle="配置奖池、概率、规则与策略生命周期">
            <ProTable<StrategyDefinitionSummary, StrategyDefinitionQuery>
                actionRef={actionRef}
                rowKey="strategyId"
                headerTitle="策略定义列表"
                columns={columns}
                request={queryStrategyDefinitions}
                search={{labelWidth: 'auto', defaultCollapsed: false, collapseRender: false, span: 8}}
                pagination={{defaultPageSize: 20, showSizeChanger: true}}
                scroll={{x: 1420}}
                toolBarRender={() => [
                    <Button key="create" type="primary" icon={<PlusOutlined/>}
                            onClick={openCreateEditor}>新建策略</Button>,
                ]}
            />

            <StrategyDefinitionEditor
                key={editingStrategy?.strategyId ?? 'create'}
                open={editorOpen}
                strategy={editingStrategy}
                awardOptions={awardOptions}
                awardOptionsLoading={awardOptionsLoading}
                onOpenChange={setEditorOpen}
                onFinish={submitDefinition}
            />

            <Drawer title="策略定义详情" width={920} open={Boolean(detailStrategy) || detailLoading}
                    loading={detailLoading}
                    onClose={() => setDetailStrategy(undefined)}>
                {detailStrategy ? <StrategyDefinitionDetail strategy={detailStrategy}/> : null}
            </Drawer>

            <Drawer
                title="策略发布检查"
                width={920}
                open={readinessOpen}
                loading={readinessLoading}
                onClose={() => setReadinessOpen(false)}
                extra={readinessStrategy?.state === 'draft' && readinessReport?.ready ? (
                    <Button type="primary" icon={<RocketOutlined/>} onClick={() => {
                        setReadinessOpen(false);
                        confirmStateChange(readinessStrategy, 'active');
                    }}>确认发布</Button>
                ) : null}
            >
                {readinessReport ? <StrategyReadinessDetail report={readinessReport}/> : null}
            </Drawer>
        </PageContainer>
    );
}

function StrategyReadinessDetail({report}: { report: StrategyReadiness }) {
    const issues = [
        ...report.errors.map((issue) => ({...issue, level: '错误'})),
        ...report.warnings.map((issue) => ({...issue, level: '警告'})),
        ...report.suggestions.map((issue) => ({...issue, level: '建议'})),
    ];
    return (
        <Space direction="vertical" size={24} style={{width: '100%'}}>
            <Alert
                showIcon
                type={report.ready ? 'success' : 'error'}
                message={report.ready ? '策略满足发布条件' : `发现 ${report.errors.length} 项阻断问题`}
                description={report.ready ? '可以确认发布；发布时服务端仍会再次校验并刷新运行时概率快照。' : '请修复全部错误后重新执行发布检查。'}
            />
            <Descriptions bordered size="small" column={2}>
                <Descriptions.Item label="策略 ID">{report.strategyId}</Descriptions.Item>
                <Descriptions.Item label="检查结果">
                    <Tag color={report.ready ? 'success' : 'error'}>{report.ready ? '已就绪' : '未就绪'}</Tag>
                </Descriptions.Item>
                <Descriptions.Item label="概率预览">
                    {report.probabilityPreview.available ? '可用' : '不可用'}
                </Descriptions.Item>
                <Descriptions.Item label="总整数权重">{report.probabilityPreview.totalWeight}</Descriptions.Item>
            </Descriptions>
            <div>
                <Typography.Title level={5}>检查明细</Typography.Title>
                <Table<StrategyReadinessIssue & { level: string }>
                    rowKey={(record) => `${record.level}-${record.code}-${record.path}`}
                    size="small"
                    pagination={false}
                    dataSource={issues}
                    locale={{emptyText: <Empty description="没有发现错误、警告或建议"/>}}
                    columns={readinessIssueColumns}
                />
            </div>
            <div>
                <Typography.Title level={5}>概率命中区间</Typography.Title>
                <Table<StrategyProbabilityInterval>
                    rowKey="awardId"
                    size="small"
                    pagination={false}
                    dataSource={report.probabilityPreview.intervals}
                    locale={{emptyText: <Empty description="当前无法生成概率预览"/>}}
                    columns={probabilityIntervalColumns}
                />
            </div>
        </Space>
    );
}

const readinessIssueColumns: ColumnsType<StrategyReadinessIssue & { level: string }> = [
    {
        title: '级别', dataIndex: 'level', width: 80,
        render: (level) => <Tag
            color={level === '错误' ? 'error' : level === '警告' ? 'warning' : 'processing'}>{level}</Tag>,
    },
    {title: '配置位置', dataIndex: 'path', width: 220},
    {title: '问题说明', dataIndex: 'message'},
    {title: '问题编码', dataIndex: 'code', width: 220, ellipsis: true},
];

const probabilityIntervalColumns: ColumnsType<StrategyProbabilityInterval> = [
    {title: '奖品 ID', dataIndex: 'awardId', width: 180},
    {title: '整数权重', dataIndex: 'weight', width: 110},
    {title: '命中区间', width: 160, render: (_, record) => `[${record.startInclusive}, ${record.endExclusive})`},
    {title: '理论概率', dataIndex: 'probability', render: (value) => `${(Number(value) * 100).toFixed(6)}%`},
];

function StrategyDefinitionEditor({open, strategy, awardOptions, awardOptionsLoading, onOpenChange, onFinish}: {
    open: boolean;
    strategy?: StrategyDefinition;
    awardOptions: SelectOption[];
    awardOptionsLoading: boolean;
    onOpenChange: (open: boolean) => void;
    onFinish: (values: StrategyDefinitionEditorValues) => Promise<boolean>;
}) {
    return (
        <ModalForm<StrategyDefinitionEditorValues>
            title={strategy ? '编辑策略草稿' : '新建策略'}
            width={1040}
            open={open}
            initialValues={toEditorValues(strategy)}
            modalProps={{destroyOnClose: true, maskClosable: false, onCancel: () => onOpenChange(false)}}
            submitter={{searchConfig: {submitText: strategy ? '保存完整配置' : '创建草稿', resetText: '取消'}}}
            onOpenChange={onOpenChange}
            onFinish={onFinish}
        >
            <Alert showIcon type="info" message="策略以完整快照保存"
                   description="保存草稿会整体替换奖品和规则配置。发布时服务端会再次校验奖品状态、库存、概率空间和规则引用。"/>
            <ProFormText name="strategyDesc" label="策略描述"
                         rules={[{required: true, message: '请输入策略描述'}, {max: 256}]}/>
            <ProFormSelect name="ruleModels" label="策略级规则" mode="multiple" options={strategyRuleModelOptions}
                           tooltip="决定抽奖前责任链执行顺序；详细参数在规则配置中维护"/>

            <Typography.Title level={5}>奖品配置</Typography.Title>
            <ProFormList name="awards" creatorButtonProps={{creatorButtonText: '添加策略奖品'}} copyIconProps={false}
                         deleteIconProps={{tooltipText: '删除奖品'}}>
                <ProForm.Group>
                    <ProFormSelect name="awardId" label="奖品" width="md" options={awardOptions}
                                   fieldProps={{
                                       showSearch: true,
                                       optionFilterProp: 'label',
                                       loading: awardOptionsLoading
                                   }}
                                   rules={positiveIdRules}/>
                    <ProFormText name="awardTitle" label="奖品标题" width="md" rules={[{required: true}, {max: 128}]}/>
                    <ProFormText name="awardSubtitle" label="奖品副标题" width="md" rules={[{max: 128}]}/>
                </ProForm.Group>
                <ProForm.Group>
                    <ProFormDigit name="awardCount" label="库存总量" width="sm" min={1} fieldProps={{precision: 0}}
                                  rules={[{required: true, message: '请输入库存总量'}]}/>
                    <ProFormDigit name="awardRate" label="概率权重" width="sm" min={0.00001} fieldProps={{precision: 5}}
                                  rules={[{required: true, message: '请输入概率权重'}]}/>
                    <ProFormDigit name="sort" label="展示顺序" width="sm" min={1} fieldProps={{precision: 0}}
                                  rules={[{required: true, message: '请输入展示顺序'}]}/>
                    <ProFormSelect name="ruleModels" label="奖品级规则" width="md" mode="multiple"
                                   options={awardRuleModelOptions}
                                   tooltip="幸运奖由系统规则树模板维护，无需在奖品上重复声明"/>
                </ProForm.Group>
            </ProFormList>

            <Typography.Title level={5}>规则配置</Typography.Title>
            <ProFormList name="rules" creatorButtonProps={{creatorButtonText: '添加规则配置'}} copyIconProps={false}
                         deleteIconProps={{tooltipText: '删除规则'}}>
                {() => (
                    <>
                        <ProForm.Group>
                            <ProFormSelect name="ruleModel" label="规则模型" width="md" options={ruleConfigModelOptions}
                                           rules={[{required: true, message: '请选择规则模型'}]}/>
                            <ProFormText name="ruleDesc" label="规则说明" width="lg"
                                         rules={[{required: true, message: '请输入规则说明'}, {max: 256}]}/>
                        </ProForm.Group>
                        <ProFormDependency name={['ruleModel']}>
                            {({ruleModel}) => <RuleConfigurationFields ruleModel={ruleModel}
                                                                       awardOptions={awardOptions}/>}
                        </ProFormDependency>
                    </>
                )}
            </ProFormList>
        </ModalForm>
    );
}

function RuleConfigurationFields({ruleModel, awardOptions}: {
    ruleModel?: StrategyRuleEditorValues['ruleModel'];
    awardOptions: SelectOption[];
}) {
    if (ruleModel === 'rule_blacklist') {
        return (
            <ProForm.Group>
                <ProFormSelect name="blacklistFallbackAwardId" label="黑名单兜底奖品" width="md"
                               options={awardOptions} fieldProps={{showSearch: true, optionFilterProp: 'label'}}
                               rules={positiveIdRules}/>
                <ProFormSelect name="blacklistUserIds" label="黑名单用户" width="lg" mode="tags"
                               fieldProps={{tokenSeparators: [',', '，', ' ']}}
                               rules={[{required: true, message: '请输入至少一个黑名单用户'}]}
                               tooltip="输入用户标识后按回车，可批量粘贴并使用逗号分隔"/>
            </ProForm.Group>
        );
    }
    if (ruleModel === 'rule_weight') {
        return (
            <ProFormList name="weightTiers" label="权重奖池档位"
                         creatorButtonProps={{creatorButtonText: '添加次数档位'}} copyIconProps={false}
                         deleteIconProps={{tooltipText: '删除档位'}}
                         required isValidateList emptyListMessage="至少配置一个权重档位">
                <ProForm.Group>
                    <ProFormDigit name="threshold" label="累计次数门槛" width="sm" min={0}
                                  fieldProps={{precision: 0}} rules={[{required: true, message: '请输入次数门槛'}]}/>
                    <ProFormSelect name="awardIds" label="候选奖品" width="lg" mode="multiple"
                                   options={awardOptions} fieldProps={{showSearch: true, optionFilterProp: 'label'}}
                                   rules={[{required: true, message: '请选择至少一个候选奖品'}]}/>
                </ProForm.Group>
            </ProFormList>
        );
    }
    if (ruleModel === 'rule_lock') {
        return (
            <ProForm.Group>
                <ProFormSelect name="awardId" label="解锁奖品" width="md" options={awardOptions}
                               fieldProps={{showSearch: true, optionFilterProp: 'label'}} rules={positiveIdRules}/>
                <ProFormDigit name="lockRequiredCount" label="最低完成次数" width="sm" min={0}
                              fieldProps={{precision: 0}} rules={[{required: true, message: '请输入最低完成次数'}]}/>
            </ProForm.Group>
        );
    }
    return <Typography.Text type="secondary">请选择规则模型后配置业务参数</Typography.Text>;
}

function StrategyDefinitionDetail({strategy}: { strategy: StrategyDefinition }) {
    const state = stateLabels[strategy.state];
    const totalRate = strategy.awards.reduce((total, award) => total + Number(award.awardRate), 0);
    return (
        <Space direction="vertical" size={24} style={{width: '100%'}}>
            <Descriptions bordered size="small" column={2}>
                <Descriptions.Item label="策略 ID">{strategy.strategyId}</Descriptions.Item>
                <Descriptions.Item label="状态"><Tag color={state.color}>{state.text}</Tag></Descriptions.Item>
                <Descriptions.Item label="策略描述" span={2}>{strategy.strategyDesc}</Descriptions.Item>
                <Descriptions.Item label="策略规则" span={2}><RuleModelTags
                    value={strategy.ruleModels}/></Descriptions.Item>
                <Descriptions.Item label="奖品数量">{strategy.awards.length}</Descriptions.Item>
                <Descriptions.Item label="概率权重合计">{totalRate.toFixed(5)}</Descriptions.Item>
                <Descriptions.Item label="乐观锁版本">{strategy.version}</Descriptions.Item>
                <Descriptions.Item label="修改人">{strategy.updatedBy}</Descriptions.Item>
                <Descriptions.Item label="创建时间">{strategy.createTime}</Descriptions.Item>
                <Descriptions.Item label="更新时间">{strategy.updateTime}</Descriptions.Item>
            </Descriptions>

            <div>
                <Typography.Title level={5}>策略奖品</Typography.Title>
                <Table<StrategyAwardConfig> rowKey={(record) => `${record.awardId}-${record.sort}`} size="small"
                                            pagination={false}
                                            dataSource={strategy.awards} columns={awardColumns} scroll={{x: 1050}}
                                            locale={{emptyText: <Empty description="尚未配置奖品"/>}}/>
            </div>
            <div>
                <Typography.Title level={5}>策略规则</Typography.Title>
                <Table<StrategyRuleConfig>
                    rowKey={(record) => `${record.ruleType}-${record.awardId ?? 'strategy'}-${record.ruleModel}`}
                    size="small" pagination={false} dataSource={strategy.rules} columns={ruleColumns}
                    scroll={{x: 900}} locale={{emptyText: <Empty description="尚未配置规则"/>}}/>
            </div>
        </Space>
    );
}

const awardColumns: ColumnsType<StrategyAwardConfig> = [
    {title: '顺序', dataIndex: 'sort', width: 70},
    {title: '奖品 ID', dataIndex: 'awardId', width: 160},
    {title: '标题', dataIndex: 'awardTitle', width: 150},
    {title: '副标题', dataIndex: 'awardSubtitle', width: 160, render: (value) => value || '-'},
    {title: '库存', width: 150, render: (_, record) => `${record.awardCountSurplus} / ${record.awardCount}`},
    {title: '概率权重', dataIndex: 'awardRate', width: 110},
    {
        title: '奖品规则',
        dataIndex: 'ruleModels',
        width: 230,
        render: (value) => <RuleModelTags value={value} separator="/"/>
    },
];

const ruleColumns: ColumnsType<StrategyRuleConfig> = [
    {title: '类型', dataIndex: 'ruleType', width: 100, render: (value) => value === 1 ? '策略规则' : '奖品规则'},
    {title: '关联奖品', dataIndex: 'awardId', width: 160, render: (value) => value || '-'},
    {title: '规则模型', dataIndex: 'ruleModel', width: 160},
    {title: '规则值', dataIndex: 'ruleValue', width: 220, ellipsis: true},
    {title: '规则说明', dataIndex: 'ruleDesc', width: 260, ellipsis: true},
];

function RuleModelTags({value, separator = ','}: { value: string | null; separator?: ',' | '/' }) {
    const models = value?.split(separator).map((item) => item.trim()).filter(Boolean) ?? [];
    return models.length ?
        <Space size={[4, 4]} wrap>{models.map((model) => <Tag key={model} color="blue">{model}</Tag>)}</Space> :
        <Typography.Text type="secondary">无</Typography.Text>;
}

function toEditorValues(strategy?: StrategyDefinition): StrategyDefinitionEditorValues {
    if (!strategy) {
        return {strategyDesc: '', ruleModels: [], awards: [], rules: []};
    }
    return {
        strategyDesc: strategy.strategyDesc,
        ruleModels: splitModels(strategy.ruleModels, ','),
        awards: strategy.awards.map((award) => ({
            awardId: award.awardId,
            awardTitle: award.awardTitle,
            awardSubtitle: award.awardSubtitle ?? undefined,
            awardCount: award.awardCount,
            awardRate: Number(award.awardRate),
            ruleModels: splitModels(award.ruleModels, '/'),
            sort: award.sort,
        })),
        rules: strategy.rules.map(toRuleEditorValues),
    };
}

function toRuleEditorValues(rule: StrategyRuleConfig): StrategyRuleEditorValues {
    if (rule.ruleModel === 'rule_blacklist') {
        const separatorIndex = rule.ruleValue.indexOf(':');
        return {
            ruleModel: rule.ruleModel,
            blacklistFallbackAwardId: separatorIndex >= 0 ? rule.ruleValue.slice(0, separatorIndex).trim() : undefined,
            blacklistUserIds: separatorIndex >= 0 ? splitValues(rule.ruleValue.slice(separatorIndex + 1), '/') : [],
            ruleDesc: rule.ruleDesc,
        };
    }
    if (rule.ruleModel === 'rule_weight') {
        return {
            ruleModel: rule.ruleModel,
            weightTiers: splitValues(rule.ruleValue, ';').map((tier) => {
                const separatorIndex = tier.indexOf(':');
                const threshold = Number(separatorIndex >= 0 ? tier.slice(0, separatorIndex).trim() : '');
                return {
                    threshold: Number.isSafeInteger(threshold) && threshold >= 0 ? threshold : 0,
                    awardIds: separatorIndex >= 0 ? splitValues(tier.slice(separatorIndex + 1), '/') : [],
                };
            }),
            ruleDesc: rule.ruleDesc,
        };
    }
    return {
        awardId: rule.awardId ?? undefined,
        ruleModel: 'rule_lock',
        lockRequiredCount: Number(rule.ruleValue),
        ruleDesc: rule.ruleDesc,
    };
}

function splitValues(value: string, separator: '/' | ';'): string[] {
    return value.split(separator).map((item) => item.trim()).filter(Boolean);
}

function splitModels(value: string | null, separator: ',' | '/'): string[] {
    return value?.split(separator).map((item) => item.trim()).filter(Boolean) ?? [];
}

function mergeReferencedAwards(options: SelectOption[], awards: StrategyAwardConfig[]): SelectOption[] {
    const merged = new Map(options.map((option) => [option.value, option]));
    awards.forEach((award) => merged.set(award.awardId, {
        label: `${award.awardTitle} (${award.awardId})`,
        value: award.awardId
    }));
    return Array.from(merged.values());
}

export default StrategyDefinitionPage;
