import {
    changeAwardDefinitionState,
    createAwardDefinition,
    getAwardDefinition,
    queryAwardDefinitions,
    updateAwardDefinition,
} from '@/services/award/award-definition';
import {queryAwardTypeMetadata} from '@/services/award/award-type-metadata';
import type {
    AwardConfigFieldMetadata,
    AwardDefinitionEditorValues,
    AwardDefinitionFormValues,
    AwardDefinitionItem,
    AwardDefinitionQuery,
    AwardDefinitionState,
    AwardTypeMetadata,
} from '@/services/award/types';
import {getApiErrorMessage} from '@/services/errors';
import {CheckCircleOutlined, EditOutlined, EyeOutlined, PlusOutlined, StopOutlined} from '@ant-design/icons';
import type {ActionType, ProColumns} from '@ant-design/pro-components';
import {
    ModalForm,
    PageContainer,
    ProForm,
    ProFormDigit,
    ProFormSelect,
    ProFormSwitch,
    ProFormText,
    ProFormTextArea,
    ProTable,
} from '@ant-design/pro-components';
import {Alert, Button, Descriptions, Drawer, message, Modal, Space, Tag, Tooltip} from 'antd';
import {useEffect, useMemo, useRef, useState} from 'react';
import styles from './index.less';

const stateLabels: Record<AwardDefinitionState, { color: string; text: string }> = {
    draft: {color: 'default', text: '草稿'},
    active: {color: 'success', text: '已启用'},
    disabled: {color: 'error', text: '已停用'},
};

/**
 * 奖品定义运营管理页面。
 *
 * @returns 奖品列表、详情、创建、编辑和生命周期操作界面
 */
function AwardDefinitionPage() {
    const actionRef = useRef<ActionType>();
    const [editorForm] = ProForm.useForm<AwardDefinitionEditorValues>();
    const [editorOpen, setEditorOpen] = useState(false);
    const [editingAward, setEditingAward] = useState<AwardDefinitionItem>();
    const [detailAward, setDetailAward] = useState<AwardDefinitionItem>();
    const [detailLoading, setDetailLoading] = useState(false);
    const [awardTypes, setAwardTypes] = useState<AwardTypeMetadata[]>([]);
    const [awardTypesLoading, setAwardTypesLoading] = useState(true);
    const [selectedAwardType, setSelectedAwardType] = useState<string>();

    useEffect(() => {
        queryAwardTypeMetadata()
            .then((metadata) => {
                setAwardTypes(metadata);
                setSelectedAwardType((current) => current ?? metadata[0]?.awardType);
            })
            .catch((error) => message.error(getApiErrorMessage(error, '奖品类型元数据加载失败')))
            .finally(() => setAwardTypesLoading(false));
    }, []);

    const selectedMetadata = useMemo(
        () => awardTypes.find((metadata) => metadata.awardType === selectedAwardType),
        [awardTypes, selectedAwardType],
    );

    const awardTypeOptions = useMemo(() => {
        const options = awardTypes.map((metadata) => ({
            label: metadata.displayName,
            value: metadata.awardType,
        }));
        if (editingAward && !awardTypes.some((metadata) => metadata.awardType === editingAward.awardType)) {
            options.push({label: `${editingAward.awardType}（暂不支持）`, value: editingAward.awardType});
        }
        return options;
    }, [awardTypes, editingAward]);

    const awardTypeValueEnum = useMemo(
        () => Object.fromEntries(awardTypes.map((metadata) => [metadata.awardType, {text: metadata.displayName}])),
        [awardTypes],
    );

    const supportedCapabilities = useMemo(
        () => new Set(awardTypes.flatMap((metadata) => metadata.supportedClaimModes.map((mode) => `${metadata.awardType}:${mode.code}`))),
        [awardTypes],
    );

    const editingCapabilitySupported = !editingAward
        || supportedCapabilities.has(`${editingAward.awardType}:${editingAward.claimMode}`);

    const reloadTable = async () => {
        await actionRef.current?.reload();
    };

    const openCreateEditor = () => {
        const metadata = awardTypes[0];
        if (!metadata) {
            message.warning('当前没有可配置的奖品类型');
            return;
        }
        setEditingAward(undefined);
        setSelectedAwardType(metadata.awardType);
        editorForm.setFieldsValue(toEditorValues(undefined, metadata));
        setEditorOpen(true);
    };

    const openEditEditor = async (award: AwardDefinitionItem) => {
        try {
            const latest = await getAwardDefinition(award.awardId);
            setEditingAward(latest);
            setSelectedAwardType(latest.awardType);
            editorForm.setFieldsValue(
                toEditorValues(latest, awardTypes.find((metadata) => metadata.awardType === latest.awardType)),
            );
            setEditorOpen(true);
        } catch (error) {
            message.error(getApiErrorMessage(error, '奖品详情加载失败'));
        }
    };

    const openDetail = async (award: AwardDefinitionItem) => {
        setDetailLoading(true);
        setDetailAward(award);

        try {
            setDetailAward(await getAwardDefinition(award.awardId));
        } catch (error) {
            message.error(getApiErrorMessage(error, '奖品详情加载失败'));
        } finally {
            setDetailLoading(false);
        }
    };

    const submitDefinition = async (values: AwardDefinitionEditorValues): Promise<boolean> => {
        const metadata = awardTypes.find((item) => item.awardType === values.awardType);
        if (!metadata) {
            message.error('该奖品类型当前不具备可用的履约能力');
            return false;
        }

        try {
            const request = toRequestValues(values, metadata);
            if (editingAward) {
                await updateAwardDefinition(editingAward.awardId, editingAward.version, request);
                message.success('奖品草稿已更新');
            } else {
                await createAwardDefinition(request);
                message.success('奖品草稿已创建');
            }

            await reloadTable();
            return true;
        } catch (error) {
            message.error(getApiErrorMessage(error, editingAward ? '奖品草稿更新失败' : '奖品草稿创建失败'));
            return false;
        }
    };

    const confirmStateChange = (award: AwardDefinitionItem, targetState: 'active' | 'disabled') => {
        const activating = targetState === 'active';
        Modal.confirm({
            title: activating ? '启用奖品' : '停用奖品',
            content: `${activating ? '确认启用' : '确认停用'}“${award.awardName}”？`,
            okText: activating ? '启用' : '停用',
            cancelText: '取消',
            okButtonProps: {danger: !activating},
            async onOk() {
                try {
                    await changeAwardDefinitionState(award.awardId, targetState, award.version);
                    message.success(activating ? '奖品已启用' : '奖品已停用');
                    await reloadTable();
                } catch (error) {
                    message.error(getApiErrorMessage(error, activating ? '奖品启用失败' : '奖品停用失败'));
                    throw error;
                }
            },
        });
    };

    const columns: ProColumns<AwardDefinitionItem>[] = [
        {title: '奖品 ID', dataIndex: 'awardId', copyable: true, hideInSearch: true, width: 170},
        {title: '奖品编码', dataIndex: 'awardCode', copyable: true, ellipsis: true, width: 160},
        {title: '奖品名称', dataIndex: 'awardName', ellipsis: true, width: 180},
        {
            title: '发奖类型',
            dataIndex: 'awardType',
            valueType: 'select',
            valueEnum: awardTypeValueEnum,
            ellipsis: true,
            width: 170,
            render: (_, record) => {
                const displayName = awardTypes.find((metadata) => metadata.awardType === record.awardType)?.displayName ?? record.awardType;
                const supported = supportedCapabilities.has(`${record.awardType}:${record.claimMode}`);
                return (
                    <Space size={4}>
                        <span>{displayName}</span>
                        {!supported ? <Tag color="warning">暂未支持</Tag> : null}
                    </Space>
                );
            },
        },
        {
            title: '领取模式',
            dataIndex: 'claimMode',
            hideInSearch: true,
            width: 110,
            valueEnum: {automatic: {text: '自动发放'}, manual: {text: '人工领取'}},
        },
        {
            title: '状态',
            dataIndex: 'state',
            valueType: 'select',
            width: 100,
            valueEnum: {
                draft: {text: '草稿', status: 'Default'},
                active: {text: '已启用', status: 'Success'},
                disabled: {text: '已停用', status: 'Error'},
            },
            render: (_, record) => <Tag color={stateLabels[record.state].color}>{stateLabels[record.state].text}</Tag>,
        },
        {title: '配置版本', dataIndex: 'configVersion', hideInSearch: true, width: 100},
        {title: '修改人', dataIndex: 'updatedBy', hideInSearch: true, ellipsis: true, width: 120},
        {title: '更新时间', dataIndex: 'updateTime', valueType: 'dateTime', hideInSearch: true, width: 170},
        {
            title: '操作',
            valueType: 'option',
            fixed: 'right',
            width: 230,
            render: (_, record) => [
                <Button key="detail" type="link" icon={<EyeOutlined/>} onClick={() => openDetail(record)}>
                    查看
                </Button>,
                record.state === 'draft' ? (
                    <Button key="edit" type="link" icon={<EditOutlined/>} onClick={() => openEditEditor(record)}>
                        编辑
                    </Button>
                ) : null,
                record.state === 'draft' ? (
                    <Tooltip
                        key="active"
                        title={supportedCapabilities.has(`${record.awardType}:${record.claimMode}`)
                            ? undefined
                            : '当前发奖类型或领取模式尚未实现，不能启用'}
                    >
                        <span>
                            <Button
                                type="link"
                                icon={<CheckCircleOutlined/>}
                                disabled={!supportedCapabilities.has(`${record.awardType}:${record.claimMode}`)}
                                onClick={() => confirmStateChange(record, 'active')}
                            >
                                启用
                            </Button>
                        </span>
                    </Tooltip>
                ) : null,
                record.state === 'active' ? (
                    <Button
                        key="disable"
                        type="link"
                        danger
                        icon={<StopOutlined/>}
                        onClick={() => confirmStateChange(record, 'disabled')}
                    >
                        停用
                    </Button>
                ) : null,
            ],
        },
    ];

    return (
        <PageContainer title="奖品定义" subTitle="维护奖品履约配置与生命周期">
            <ProTable<AwardDefinitionItem, AwardDefinitionQuery>
                actionRef={actionRef}
                rowKey="awardId"
                headerTitle="奖品定义列表"
                columns={columns}
                request={queryAwardDefinitions}
                search={{labelWidth: 'auto', defaultCollapsed: false, collapseRender: false, span: 12}}
                pagination={{defaultPageSize: 20, showSizeChanger: true}}
                scroll={{x: 1510}}
                toolBarRender={() => [
                    <Button
                        key="create"
                        type="primary"
                        icon={<PlusOutlined/>}
                        loading={awardTypesLoading}
                        disabled={!awardTypesLoading && awardTypes.length === 0}
                        onClick={openCreateEditor}
                    >
                        新建奖品
                    </Button>,
                ]}
            />

            <ModalForm<AwardDefinitionEditorValues>
                key={editingAward?.awardId ?? 'create'}
                form={editorForm}
                title={editingAward ? '编辑奖品草稿' : '新建奖品'}
                width={720}
                open={editorOpen}
                modalProps={{destroyOnClose: true, maskClosable: false, onCancel: () => setEditorOpen(false)}}
                submitter={{searchConfig: {submitText: editingAward ? '保存修改' : '创建草稿', resetText: '取消'}}}
                onOpenChange={setEditorOpen}
                onFinish={submitDefinition}
            >
                {!editingCapabilitySupported ? (
                    <Alert
                        showIcon
                        type="warning"
                        message="当前履约方案暂未支持"
                        description="该奖品的发奖类型或领取模式尚无可执行处理器，保存时需改为系统已支持的方案，且在能力实现前不能启用。"
                    />
                ) : null}
                <ProForm.Group>
                    <ProFormText
                        name="awardCode"
                        label="奖品编码"
                        width="md"
                        rules={[
                            {required: true, message: '请输入奖品编码'},
                            {pattern: /^[a-z][a-z0-9_-]{1,63}$/, message: '奖品编码格式不合法'},
                        ]}
                    />
                    <ProFormText
                        name="awardName"
                        label="奖品名称"
                        width="md"
                        rules={[
                            {required: true, message: '请输入奖品名称'},
                            {max: 128, message: '奖品名称不能超过 128 个字符'},
                        ]}
                    />
                </ProForm.Group>
                <ProForm.Group>
                    <ProFormSelect
                        name="awardType"
                        label="发奖类型"
                        width="md"
                        options={awardTypeOptions}
                        disabled={Boolean(editingAward)}
                        fieldProps={{
                            onChange: (value: string) => {
                                const metadata = awardTypes.find((item) => item.awardType === value);
                                setSelectedAwardType(value);
                                editorForm.setFieldsValue({
                                    claimMode: metadata?.supportedClaimModes[0]?.code,
                                    configValues: {},
                                });
                            },
                        }}
                        rules={[{required: true, message: '请选择发奖类型'}]}
                    />
                    <ProFormSelect
                        name="claimMode"
                        label="领取模式"
                        width="md"
                        options={(selectedMetadata?.supportedClaimModes ?? []).map((mode) => ({
                            label: mode.displayName,
                            value: mode.code,
                        }))}
                        rules={[{required: true, message: '请选择领取模式'}]}
                    />
                </ProForm.Group>
                <ProFormText
                    name="providerCode"
                    label="供应商编码"
                    width="md"
                    rules={[{pattern: /^[a-z][a-z0-9_-]{1,31}$/, message: '供应商编码格式不合法'}]}
                />
                {selectedMetadata?.configFields.map(renderConfigField)}
                <ProFormTextArea
                    name="awardDesc"
                    label="奖品说明"
                    fieldProps={{autoSize: {minRows: 2, maxRows: 4}}}
                    rules={[{max: 256, message: '奖品说明不能超过 256 个字符'}]}
                />
            </ModalForm>

            <Drawer
                title="奖品详情"
                width={720}
                open={Boolean(detailAward)}
                loading={detailLoading}
                onClose={() => setDetailAward(undefined)}
            >
                {detailAward ? <AwardDefinitionDetail award={detailAward} awardTypes={awardTypes}/> : null}
            </Drawer>
        </PageContainer>
    );
}

/**
 * 展示奖品定义全部配置与审计信息。
 *
 * @param props 奖品详情
 * @returns 详情描述列表
 */
function AwardDefinitionDetail({award, awardTypes}: { award: AwardDefinitionItem; awardTypes: AwardTypeMetadata[] }) {
    const state = stateLabels[award.state];
    const metadata = awardTypes.find((item) => item.awardType === award.awardType);
    const claimMode = metadata?.supportedClaimModes.find((mode) => mode.code === award.claimMode);
    const fulfillmentSupported = Boolean(claimMode);
    return (
        <Descriptions bordered size="small" column={2}>
            <Descriptions.Item label="奖品 ID" span={2}>
                {award.awardId}
            </Descriptions.Item>
            <Descriptions.Item label="奖品编码">{award.awardCode}</Descriptions.Item>
            <Descriptions.Item label="奖品名称">{award.awardName}</Descriptions.Item>
            <Descriptions.Item label="发奖类型">{metadata?.displayName ?? award.awardType}</Descriptions.Item>
            <Descriptions.Item label="领取模式">{claimMode?.displayName ?? award.claimMode}</Descriptions.Item>
            <Descriptions.Item label="履约能力">
                <Tag
                    color={fulfillmentSupported ? 'success' : 'warning'}>{fulfillmentSupported ? '已支持' : '暂未支持'}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="供应商编码">{award.providerCode || '-'}</Descriptions.Item>
            <Descriptions.Item label="状态">
                <Tag color={state.color}>{state.text}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="配置版本">{award.configVersion}</Descriptions.Item>
            <Descriptions.Item label="乐观锁版本">{award.version}</Descriptions.Item>
            <Descriptions.Item label="发奖配置" span={2}>
                <pre className={styles.jsonPreview}>{formatJson(award.awardConfig)}</pre>
            </Descriptions.Item>
            <Descriptions.Item label="奖品说明" span={2}>
                {award.awardDesc || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="创建人">{award.createdBy}</Descriptions.Item>
            <Descriptions.Item label="修改人">{award.updatedBy}</Descriptions.Item>
            <Descriptions.Item label="创建时间">{award.createTime}</Descriptions.Item>
            <Descriptions.Item label="更新时间">{award.updateTime}</Descriptions.Item>
        </Descriptions>
    );
}

function toEditorValues(award: AwardDefinitionItem | undefined, metadata: AwardTypeMetadata | undefined): AwardDefinitionEditorValues {
    if (!award) {
        return {
            awardCode: '',
            awardName: '',
            awardType: metadata?.awardType ?? '',
            claimMode: metadata?.supportedClaimModes[0]?.code ?? '',
            configValues: {},
        };
    }
    return {
        awardCode: award.awardCode,
        awardName: award.awardName,
        awardType: award.awardType,
        claimMode: award.claimMode,
        providerCode: award.providerCode ?? undefined,
        configValues: parseConfigValues(award.awardConfig),
        awardDesc: award.awardDesc ?? undefined,
    };
}

/**
 * 将结构化编辑器数据转换为后端奖品定义契约。
 *
 * @param values 编辑器字段
 * @param metadata 当前奖品类型元数据
 * @returns 包含版本化 JSON 配置的后端请求
 */
function toRequestValues(values: AwardDefinitionEditorValues, metadata: AwardTypeMetadata): AwardDefinitionFormValues {
    const config = Object.fromEntries(
        metadata.configFields
            .map((field) => [field.name, values.configValues?.[field.name]] as const)
            .filter((entry) => entry[1] !== undefined),
    );
    return {
        awardCode: values.awardCode,
        awardName: values.awardName,
        awardType: metadata.awardType,
        claimMode: values.claimMode,
        providerCode: values.providerCode,
        awardConfig: JSON.stringify(config),
        configVersion: metadata.configVersion,
        awardDesc: values.awardDesc,
    };
}

/**
 * 按字段元数据渲染受控表单组件。
 *
 * @param field 奖品配置字段元数据
 * @returns 与字段类型匹配的 ProForm 控件
 */
function renderConfigField(field: AwardConfigFieldMetadata) {
    const name = ['configValues', field.name];
    const rules = [{required: field.required, message: `请输入${field.label}`}];
    const commonProps = {
        key: field.name,
        name,
        label: field.label,
        width: 'md' as const,
        tooltip: field.description,
        rules
    };

    switch (field.fieldType) {
        case 'integer':
            return (
                <ProFormDigit
                    {...commonProps}
                    min={toNumber(field.constraints.minimum)}
                    max={toNumber(field.constraints.maximum)}
                    fieldProps={{precision: 0, step: toNumber(field.constraints.step)}}
                />
            );
        case 'boolean':
            return <ProFormSwitch {...commonProps}/>;
        case 'select':
            return <ProFormSelect {...commonProps} options={parseSelectOptions(field.constraints.options)}/>;
        default:
            return <ProFormText {...commonProps}/>;
    }
}

function parseConfigValues(value: string): Record<string, string | number | boolean> {
    try {
        const parsed: unknown = JSON.parse(value);
        if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') {
            return {};
        }
        return Object.fromEntries(
            Object.entries(parsed).filter((entry): entry is [string, string | number | boolean] =>
                ['string', 'number', 'boolean'].includes(typeof entry[1]),
            ),
        );
    } catch {
        return {};
    }
}

function parseSelectOptions(value?: string): Array<{ label: string; value: string }> {
    if (!value) {
        return [];
    }
    return value.split(',').map((option) => ({
        label: option.trim(),
        value: option.trim()
    })).filter((option) => option.value);
}

function toNumber(value?: string): number | undefined {
    if (!value) {
        return undefined;
    }
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : undefined;
}

function formatJson(value: string): string {
    try {
        return JSON.stringify(JSON.parse(value), null, 2);
    } catch {
        return value;
    }
}

export default AwardDefinitionPage;
