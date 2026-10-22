import {
    changeAwardDefinitionState,
    createAwardDefinition,
    getAwardDefinition,
    queryAwardDefinitions,
    updateAwardDefinition,
} from '@/services/award/award-definition';
import type {
    AwardDefinitionFormValues,
    AwardDefinitionItem,
    AwardDefinitionQuery,
    AwardDefinitionState,
} from '@/services/award/types';
import {CheckCircleOutlined, EditOutlined, EyeOutlined, PlusOutlined, StopOutlined} from '@ant-design/icons';
import type {ActionType, ProColumns} from '@ant-design/pro-components';
import {
    ModalForm,
    PageContainer,
    ProForm,
    ProFormDigit,
    ProFormSelect,
    ProFormText,
    ProFormTextArea,
    ProTable,
} from '@ant-design/pro-components';
import {Button, Descriptions, Drawer, message, Modal, Tag} from 'antd';
import {useRef, useState} from 'react';
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
    const [editorOpen, setEditorOpen] = useState(false);
    const [editingAward, setEditingAward] = useState<AwardDefinitionItem>();
    const [detailAward, setDetailAward] = useState<AwardDefinitionItem>();
    const [detailLoading, setDetailLoading] = useState(false);

    const reloadTable = async () => {
        await actionRef.current?.reload();
    };

    const openCreateEditor = () => {
        setEditingAward(undefined);
        setEditorOpen(true);
    };

    const openEditEditor = async (award: AwardDefinitionItem) => {
        try {
            const latest = await getAwardDefinition(award.awardId);
            setEditingAward(latest);
            setEditorOpen(true);
        } catch {
            message.error('奖品详情加载失败');
        }
    };

    const openDetail = async (award: AwardDefinitionItem) => {
        setDetailLoading(true);
        setDetailAward(award);

        try {
            setDetailAward(await getAwardDefinition(award.awardId));
        } catch {
            message.error('奖品详情加载失败');
        } finally {
            setDetailLoading(false);
        }
    };

    const submitDefinition = async (values: AwardDefinitionFormValues): Promise<boolean> => {
        try {
            if (editingAward) {
                await updateAwardDefinition(editingAward.awardId, editingAward.version, values);
                message.success('奖品草稿已更新');
            } else {
                await createAwardDefinition(values);
                message.success('奖品草稿已创建');
            }

            await reloadTable();
            return true;
        } catch {
            message.error(editingAward ? '奖品草稿更新失败' : '奖品草稿创建失败');
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
                } catch {
                    message.error(activating ? '奖品启用失败' : '奖品停用失败');
                    throw new Error('Award state change failed');
                }
            },
        });
    };

    const columns: ProColumns<AwardDefinitionItem>[] = [
        {title: '奖品 ID', dataIndex: 'awardId', copyable: true, hideInSearch: true, width: 170},
        {title: '奖品编码', dataIndex: 'awardCode', copyable: true, ellipsis: true, width: 160},
        {title: '奖品名称', dataIndex: 'awardName', ellipsis: true, width: 180},
        {title: '发奖类型', dataIndex: 'awardType', ellipsis: true, width: 170},
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
                    <Button
                        key="active"
                        type="link"
                        icon={<CheckCircleOutlined/>}
                        onClick={() => confirmStateChange(record, 'active')}
                    >
                        启用
                    </Button>
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
                    <Button key="create" type="primary" icon={<PlusOutlined/>} onClick={openCreateEditor}>
                        新建奖品
                    </Button>,
                ]}
            />

            <ModalForm<AwardDefinitionFormValues>
                key={editingAward?.awardId ?? 'create'}
                title={editingAward ? '编辑奖品草稿' : '新建奖品'}
                width={720}
                open={editorOpen}
                initialValues={toFormValues(editingAward)}
                modalProps={{destroyOnClose: true, maskClosable: false, onCancel: () => setEditorOpen(false)}}
                submitter={{searchConfig: {submitText: editingAward ? '保存修改' : '创建草稿', resetText: '取消'}}}
                onOpenChange={setEditorOpen}
                onFinish={submitDefinition}
            >
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
                    <ProFormText
                        name="awardType"
                        label="发奖类型"
                        width="md"
                        rules={[
                            {required: true, message: '请输入发奖类型'},
                            {pattern: /^[a-z][a-z0-9_]{1,31}$/, message: '发奖类型格式不合法'},
                        ]}
                    />
                    <ProFormSelect
                        name="claimMode"
                        label="领取模式"
                        width="md"
                        options={[
                            {label: '自动发放', value: 'automatic'},
                            {label: '人工领取', value: 'manual'},
                        ]}
                        rules={[{required: true, message: '请选择领取模式'}]}
                    />
                </ProForm.Group>
                <ProForm.Group>
                    <ProFormText
                        name="providerCode"
                        label="供应商编码"
                        width="md"
                        rules={[{pattern: /^[a-z][a-z0-9_-]{1,31}$/, message: '供应商编码格式不合法'}]}
                    />
                    <ProFormDigit
                        name="configVersion"
                        label="配置版本"
                        width="md"
                        min={1}
                        fieldProps={{precision: 0}}
                        rules={[{required: true, message: '请输入配置版本'}]}
                    />
                </ProForm.Group>
                <ProFormTextArea
                    name="awardConfig"
                    label="发奖配置 JSON"
                    fieldProps={{autoSize: {minRows: 5, maxRows: 10}}}
                    rules={[
                        {required: true, message: '请输入发奖配置'},
                        {max: 4000, message: '发奖配置不能超过 4000 个字符'},
                        {validator: validateJson},
                    ]}
                />
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
                {detailAward ? <AwardDefinitionDetail award={detailAward}/> : null}
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
function AwardDefinitionDetail({award}: { award: AwardDefinitionItem }) {
    const state = stateLabels[award.state];
    return (
        <Descriptions bordered size="small" column={2}>
            <Descriptions.Item label="奖品 ID" span={2}>
                {award.awardId}
            </Descriptions.Item>
            <Descriptions.Item label="奖品编码">{award.awardCode}</Descriptions.Item>
            <Descriptions.Item label="奖品名称">{award.awardName}</Descriptions.Item>
            <Descriptions.Item label="发奖类型">{award.awardType}</Descriptions.Item>
            <Descriptions.Item label="领取模式">
                {award.claimMode === 'automatic' ? '自动发放' : '人工领取'}
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

function toFormValues(award?: AwardDefinitionItem): AwardDefinitionFormValues {
    if (!award) {
        return {
            awardCode: '',
            awardName: '',
            awardType: 'internal_credit',
            claimMode: 'automatic',
            awardConfig: '{}',
            configVersion: 1,
        };
    }
    return {
        awardCode: award.awardCode,
        awardName: award.awardName,
        awardType: award.awardType,
        claimMode: award.claimMode,
        providerCode: award.providerCode ?? undefined,
        awardConfig: formatJson(award.awardConfig),
        configVersion: award.configVersion,
        awardDesc: award.awardDesc ?? undefined,
    };
}

function formatJson(value: string): string {
    try {
        return JSON.stringify(JSON.parse(value), null, 2);
    } catch {
        return value;
    }
}

async function validateJson(_: unknown, value?: string): Promise<void> {
    if (!value) {
        return;
    }
    try {
        JSON.parse(value);
    } catch {
        throw new Error('请输入合法的 JSON');
    }
}

export default AwardDefinitionPage;
