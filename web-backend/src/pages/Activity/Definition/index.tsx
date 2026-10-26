import {
    changeActivityDefinitionState,
    createActivityDefinition,
    getActivityDefinition,
    queryActivityDefinitions,
    updateActivityDefinition,
} from '@/services/activity/activity-definition';
import {queryActivityCountOptions} from '@/services/activity/activity-count-definition';
import type {
    ActivityDefinitionFormValues,
    ActivityDefinitionItem,
    ActivityDefinitionQuery,
    ActivityDefinitionState
} from '@/services/activity/types';
import {getApiErrorMessage} from '@/services/errors';
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
import {Button, Descriptions, Drawer, message, Modal, Progress, Tag, Typography} from 'antd';
import {useRef, useState} from 'react';

const stateLabels: Record<ActivityDefinitionState, { color: string; text: string }> = {
    draft: {color: 'default', text: '草稿'},
    open: {color: 'success', text: '进行中'},
    close: {color: 'error', text: '已关闭'},
};

const unlockScopeLabels = {
    day: '每日次数',
    total: '累计次数',
};

/**
 * 活动定义运营查询页面。
 *
 * @returns 活动定义列表及详情界面
 */
function ActivityDefinitionPage() {
    const actionRef = useRef<ActionType>();
    const [detailActivity, setDetailActivity] = useState<ActivityDefinitionItem>();
    const [editorOpen, setEditorOpen] = useState(false);
    const [editingActivity, setEditingActivity] = useState<ActivityDefinitionItem>();

    const openCreateEditor = () => {
        setEditingActivity(undefined);
        setEditorOpen(true);
    };

    const openEditEditor = async (activity: ActivityDefinitionItem) => {
        try {
            setEditingActivity(await getActivityDefinition(activity.activityId));
            setEditorOpen(true);
        } catch (error) {
            message.error(getApiErrorMessage(error, '活动详情加载失败'));
        }
    };

    const submitDefinition = async (values: ActivityDefinitionFormValues): Promise<boolean> => {
        try {
            if (editingActivity) {
                await updateActivityDefinition(editingActivity.activityId, editingActivity.version, values);
                message.success('活动草稿已更新');
            } else {
                await createActivityDefinition(values);
                message.success('活动草稿已创建');
            }
            await actionRef.current?.reload();
            return true;
        } catch (error) {
            const fallbackMessage = editingActivity ? '活动草稿更新失败' : '活动草稿创建失败';
            message.error(getApiErrorMessage(error, fallbackMessage));
            return false;
        }
    };

    const confirmStateChange = (activity: ActivityDefinitionItem, targetState: 'open' | 'close') => {
        const opening = targetState === 'open';
        Modal.confirm({
            title: opening ? '开放活动' : '关闭活动',
            content: opening
                ? `确认开放“${activity.activityName}”？开放后核心配置不可再编辑。`
                : `确认关闭“${activity.activityName}”？关闭后用户不能继续参与。`,
            okText: opening ? '开放' : '关闭',
            cancelText: '取消',
            okButtonProps: {danger: !opening},
            async onOk() {
                try {
                    await changeActivityDefinitionState(activity.activityId, targetState, activity.version);
                    message.success(opening ? '活动已开放' : '活动已关闭');
                    await actionRef.current?.reload();
                } catch (error) {
                    message.error(getApiErrorMessage(error, opening ? '活动开放失败' : '活动关闭失败'));
                    throw error;
                }
            },
        });
    };

    const columns: ProColumns<ActivityDefinitionItem>[] = [
        {title: '活动 ID', dataIndex: 'activityId', copyable: true, width: 150},
        {title: '活动名称', dataIndex: 'activityName', ellipsis: true, width: 190},
        {
            title: '状态',
            dataIndex: 'state',
            valueType: 'select',
            width: 100,
            valueEnum: {
                draft: {text: '草稿', status: 'Default'},
                open: {text: '进行中', status: 'Success'},
                close: {text: '已关闭', status: 'Error'},
            },
            render: (_, record) => <Tag color={stateLabels[record.state].color}>{stateLabels[record.state].text}</Tag>,
        },
        {
            title: '库存',
            dataIndex: 'stockCountSurplus',
            hideInSearch: true,
            width: 190,
            render: (_, record) => <StockProgress activity={record}/>,
        },
        {title: '活动次数配置', dataIndex: 'activityCountId', copyable: true, hideInSearch: true, width: 150},
        {title: '抽奖策略', dataIndex: 'strategyId', copyable: true, hideInSearch: true, width: 150},
        {
            title: '解锁口径',
            dataIndex: 'awardUnlockScope',
            hideInSearch: true,
            width: 110,
            render: (_, record) => unlockScopeLabels[record.awardUnlockScope],
        },
        {title: '开始时间', dataIndex: 'beginDateTime', valueType: 'dateTime', hideInSearch: true, width: 170},
        {title: '结束时间', dataIndex: 'endDateTime', valueType: 'dateTime', hideInSearch: true, width: 170},
        {title: '修改人', dataIndex: 'updatedBy', hideInSearch: true, ellipsis: true, width: 120},
        {title: '更新时间', dataIndex: 'updateTime', valueType: 'dateTime', hideInSearch: true, width: 170},
        {
            title: '操作',
            valueType: 'option',
            fixed: 'right',
            width: 90,
            render: (_, record) => [
                <Button key="detail" type="link" icon={<EyeOutlined/>} onClick={() => setDetailActivity(record)}>
                    查看
                </Button>,
                record.state === 'draft' ? (
                    <Button key="edit" type="link" icon={<EditOutlined/>} onClick={() => openEditEditor(record)}>
                        编辑
                    </Button>
                ) : null,
                record.state === 'draft' ? (
                    <Button key="open" type="link" icon={<CheckCircleOutlined/>}
                            onClick={() => confirmStateChange(record, 'open')}>
                        开放
                    </Button>
                ) : null,
                record.state === 'open' ? (
                    <Button key="close" type="link" danger icon={<StopOutlined/>}
                            onClick={() => confirmStateChange(record, 'close')}>
                        关闭
                    </Button>
                ) : null,
            ],
        },
    ];

    return (
        <PageContainer title="活动定义" subTitle="管理活动基础配置、库存与生命周期">
            <ProTable<ActivityDefinitionItem, ActivityDefinitionQuery>
                actionRef={actionRef}
                rowKey="activityId"
                headerTitle="活动定义列表"
                columns={columns}
                request={queryActivityDefinitions}
                search={{labelWidth: 'auto', defaultCollapsed: false, collapseRender: false, span: 8}}
                pagination={{defaultPageSize: 20, showSizeChanger: true}}
                scroll={{x: 1590}}
                toolBarRender={() => [
                    <Button key="create" type="primary" icon={<PlusOutlined/>} onClick={openCreateEditor}>
                        新建活动
                    </Button>,
                ]}
            />

            <ModalForm<ActivityDefinitionFormValues>
                key={editingActivity?.activityId ?? 'create'}
                title={editingActivity ? '编辑活动草稿' : '新建活动'}
                width={760}
                open={editorOpen}
                initialValues={toFormValues(editingActivity)}
                modalProps={{destroyOnClose: true, maskClosable: false, onCancel: () => setEditorOpen(false)}}
                submitter={{searchConfig: {submitText: editingActivity ? '保存修改' : '创建草稿', resetText: '取消'}}}
                onOpenChange={setEditorOpen}
                onFinish={submitDefinition}
            >
                <ProFormText name="activityName" label="活动名称" rules={[{required: true}, {max: 64}]}/>
                <ProFormTextArea name="activityDesc" label="活动说明" rules={[{required: true}, {max: 256}]}/>
                <ProForm.Group>
                    <ProFormText name="beginDateTime" label="开始时间" width="md" fieldProps={{type: 'datetime-local'}}
                                 rules={[{required: true}]}/>
                    <ProFormText name="endDateTime" label="结束时间" width="md" fieldProps={{type: 'datetime-local'}}
                                 rules={[{required: true}]}/>
                </ProForm.Group>
                <ProForm.Group>
                    <ProFormDigit name="stockCount" label="库存总量" width="md" min={1} fieldProps={{precision: 0}}
                                  rules={[{required: true}]}/>
                    <ProFormSelect name="awardUnlockScope" label="奖品解锁口径" width="md" options={[
                        {label: '每日次数', value: 'day'},
                        {label: '累计次数', value: 'total'},
                    ]} rules={[{required: true}]}/>
                </ProForm.Group>
                <ProForm.Group>
                    <ProFormSelect name="activityCountId" label="活动次数配置" width="md"
                                   placeholder="输入配置 ID 或名称搜索"
                                   request={loadActivityCountOptions}
                                   fieldProps={{showSearch: true, filterOption: false, allowClear: true}}
                                   rules={[{required: true, message: '请选择活动次数配置'}]}/>
                    <ProFormText name="strategyId" label="抽奖策略 ID" width="md" rules={positiveIdRules}/>
                </ProForm.Group>
            </ModalForm>

            <Drawer
                title="活动定义详情"
                width={720}
                open={Boolean(detailActivity)}
                onClose={() => setDetailActivity(undefined)}
            >
                {detailActivity ? <ActivityDefinitionDetail activity={detailActivity}/> : null}
            </Drawer>
        </PageContainer>
    );
}

const positiveIdRules = [
    {required: true, message: '请输入业务标识'},
    {pattern: /^[1-9][0-9]{0,18}$/, message: '业务标识格式不合法'},
];

function toFormValues(activity?: ActivityDefinitionItem): ActivityDefinitionFormValues | undefined {
    if (!activity) {
        return {
            activityName: '', activityDesc: '', beginDateTime: '', endDateTime: '', stockCount: 1,
            activityCountId: '', strategyId: '', awardUnlockScope: 'total',
        };
    }
    return {
        ...activity,
        activityDesc: activity.activityDesc ?? '',
        beginDateTime: activity.beginDateTime.slice(0, 16),
        endDateTime: activity.endDateTime.slice(0, 16),
    };
}

function loadActivityCountOptions({keyWords}: { keyWords?: string }) {
    return queryActivityCountOptions(keyWords);
}

function StockProgress({activity}: { activity: ActivityDefinitionItem }) {
    const remainingPercent = activity.stockCount > 0
        ? Math.min(Math.round((Math.max(activity.stockCountSurplus, 0) / activity.stockCount) * 100), 100)
        : 0;
    return (
        <div>
            <Typography.Text>
                {activity.stockCountSurplus.toLocaleString()} / {activity.stockCount.toLocaleString()}
            </Typography.Text>
            <Progress percent={remainingPercent} size="small" showInfo={false}/>
        </div>
    );
}

function ActivityDefinitionDetail({activity}: { activity: ActivityDefinitionItem }) {
    const state = stateLabels[activity.state];
    return (
        <Descriptions bordered size="small" column={2}>
            <Descriptions.Item label="活动 ID" span={2}>
                {activity.activityId}
            </Descriptions.Item>
            <Descriptions.Item label="活动名称" span={2}>
                {activity.activityName}
            </Descriptions.Item>
            <Descriptions.Item label="活动说明" span={2}>
                {activity.activityDesc || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="状态">
                <Tag color={state.color}>{state.text}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="乐观锁版本">{activity.version}</Descriptions.Item>
            <Descriptions.Item label="开始时间">{activity.beginDateTime}</Descriptions.Item>
            <Descriptions.Item label="结束时间">{activity.endDateTime}</Descriptions.Item>
            <Descriptions.Item label="库存总量">{activity.stockCount.toLocaleString()}</Descriptions.Item>
            <Descriptions.Item label="剩余库存">{activity.stockCountSurplus.toLocaleString()}</Descriptions.Item>
            <Descriptions.Item label="活动次数配置">{activity.activityCountId}</Descriptions.Item>
            <Descriptions.Item label="抽奖策略">{activity.strategyId}</Descriptions.Item>
            <Descriptions.Item label="解锁口径">{unlockScopeLabels[activity.awardUnlockScope]}</Descriptions.Item>
            <Descriptions.Item label="创建人">{activity.createdBy}</Descriptions.Item>
            <Descriptions.Item label="修改人">{activity.updatedBy}</Descriptions.Item>
            <Descriptions.Item label="创建时间">{activity.createTime}</Descriptions.Item>
            <Descriptions.Item label="更新时间">{activity.updateTime}</Descriptions.Item>
        </Descriptions>
    );
}

export default ActivityDefinitionPage;
