import {
    createActivityStage,
    getActivityStage,
    getActivityStagePublication,
    publishActivityStage,
    queryActivityStages,
    updateActivityStage,
    waitForActivityStagePublication,
} from '@/services/activity/activity-stage';
import {checkActivityReadiness, queryOpenActivityOptions} from '@/services/activity/activity-definition';
import type {
    ActivityReadinessReport,
    ActivityStageFormValues,
    ActivityStageItem,
    ActivityStagePublicationItem,
    ActivityStagePublicationPhase,
    ActivityStageQuery,
    ActivityStageState,
} from '@/services/activity/types';
import {getApiErrorMessage} from '@/services/errors';
import {EditOutlined, EyeOutlined, PlusOutlined, RocketOutlined} from '@ant-design/icons';
import type {ActionType, ProColumns} from '@ant-design/pro-components';
import {ModalForm, PageContainer, ProForm, ProFormSelect, ProFormText, ProTable} from '@ant-design/pro-components';
import {Button, Descriptions, Drawer, message, Modal, Tag} from 'antd';
import {useRef, useState} from 'react';
import {ActivityReadinessSummary} from '../ActivityReadinessSummary';

const stateLabels: Record<ActivityStageState, { color: string; text: string }> = {
    create: {color: 'default', text: '待上架'},
    active: {color: 'success', text: '生效中'},
    expire: {color: 'error', text: '已失效'},
};

const publicationPhaseLabels: Record<ActivityStagePublicationPhase, string> = {
    checking: '配置检查',
    preheating: '运行数据预热',
    switching: '生效展台切换',
    completed: '发布完成',
};

/**
 * 活动展台运营管理页面。
 *
 * @returns 活动展台列表、创建编辑、详情和发布界面
 */
function ActivityStagePage() {
    const actionRef = useRef<ActionType>();
    const [editorOpen, setEditorOpen] = useState(false);
    const [editingStage, setEditingStage] = useState<ActivityStageItem>();
    const [detailStage, setDetailStage] = useState<ActivityStageItem>();
    const [detailLoading, setDetailLoading] = useState(false);
    const [checkingStageId, setCheckingStageId] = useState<string>();
    const [publishingStageId, setPublishingStageId] = useState<string>();

    const reloadTable = async () => {
        await actionRef.current?.reload();
    };

    const openCreateEditor = () => {
        setEditingStage(undefined);
        setEditorOpen(true);
    };

    const openEditEditor = async (stage: ActivityStageItem) => {
        try {
            const latest = await getActivityStage(stage.stageId);
            setEditingStage(latest);
            setEditorOpen(true);
        } catch (error) {
            message.error(getApiErrorMessage(error, '活动展台详情加载失败'));
        }
    };

    const openDetail = async (stage: ActivityStageItem) => {
        setDetailStage(stage);
        setDetailLoading(true);
        try {
            setDetailStage(await getActivityStage(stage.stageId));
        } catch (error) {
            message.error(getApiErrorMessage(error, '活动展台详情加载失败'));
        } finally {
            setDetailLoading(false);
        }
    };

    const submitStage = async (values: ActivityStageFormValues): Promise<boolean> => {
        try {
            if (editingStage) {
                await updateActivityStage(editingStage.stageId, editingStage.version, values);
                message.success('待上架配置已更新');
            } else {
                await createActivityStage(values);
                message.success('待上架配置已创建');
            }
            await reloadTable();
            return true;
        } catch (error) {
            const fallbackMessage = editingStage ? '待上架配置更新失败' : '待上架配置创建失败';
            message.error(getApiErrorMessage(error, fallbackMessage));
            return false;
        }
    };

    const confirmPublish = async (stage: ActivityStageItem) => {
        setCheckingStageId(stage.stageId);
        let latestStage: ActivityStageItem;
        let readinessReport: ActivityReadinessReport;
        try {
            latestStage = await getActivityStage(stage.stageId);
            if (latestStage.state !== 'create') {
                message.warning('当前展台状态已经变化，请刷新列表后重试');
                await reloadTable();
                return;
            }
            readinessReport = await checkActivityReadiness(latestStage.activityId);
        } catch (error) {
            message.error(getApiErrorMessage(error, '活动发布检查失败'));
            return;
        } finally {
            setCheckingStageId(undefined);
        }

        Modal.confirm({
            title: '发布活动',
            content: (
                <ActivityReadinessSummary
                    report={readinessReport}
                    operationName="发布"
                    description={`检查通过后将预热活动，并替换 ${latestStage.channel}/${latestStage.source} 当前生效的活动。`}
                />
            ),
            width: 720,
            okText: readinessReport.ready ? '预热并发布' : '存在阻断项',
            cancelText: '取消',
            okButtonProps: {disabled: !readinessReport.ready},
            async onOk() {
                setPublishingStageId(latestStage.stageId);
                try {
                    let publication = await publishActivityStage(latestStage.stageId, latestStage.version);
                    if (publication.state === 'processing') {
                        publication = await waitForActivityStagePublication(latestStage.stageId, latestStage.version);
                    }
                    showPublicationResult(publication);
                    await reloadTable();
                } catch (error) {
                    await showPublicationFailure(latestStage.stageId, latestStage.version, error);
                    throw error;
                } finally {
                    setPublishingStageId(undefined);
                }
            },
        });
    };

    const columns: ProColumns<ActivityStageItem>[] = [
        {title: '展台 ID', dataIndex: 'stageId', copyable: true, hideInSearch: true, width: 170},
        {title: '渠道', dataIndex: 'channel', copyable: true, width: 130},
        {title: '来源', dataIndex: 'source', copyable: true, width: 150},
        {title: '活动 ID', dataIndex: 'activityId', copyable: true, hideInSearch: true, width: 170},
        {
            title: '状态',
            dataIndex: 'state',
            valueType: 'select',
            width: 110,
            valueEnum: {
                create: {text: '待上架', status: 'Default'},
                active: {text: '生效中', status: 'Success'},
                expire: {text: '已失效', status: 'Error'},
            },
            render: (_, record) => <Tag color={stateLabels[record.state].color}>{stateLabels[record.state].text}</Tag>,
        },
        {title: '版本', dataIndex: 'version', hideInSearch: true, width: 80},
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
                record.state === 'create' ? (
                    <Button key="edit" type="link" icon={<EditOutlined/>} onClick={() => openEditEditor(record)}>
                        编辑
                    </Button>
                ) : null,
                record.state === 'create' ? (
                    <Button key="publish" type="link" icon={<RocketOutlined/>}
                            loading={checkingStageId === record.stageId || publishingStageId === record.stageId}
                            onClick={() => confirmPublish(record)}>
                        发布
                    </Button>
                ) : null,
            ],
        },
    ];

    return (
        <PageContainer title="活动上架" subTitle="按渠道和来源维护当前对外活动">
            <ProTable<ActivityStageItem, ActivityStageQuery>
                actionRef={actionRef}
                rowKey="stageId"
                headerTitle="活动展台列表"
                columns={columns}
                request={queryActivityStages}
                search={{labelWidth: 'auto', defaultCollapsed: false, collapseRender: false, span: 8}}
                pagination={{defaultPageSize: 20, showSizeChanger: true}}
                scroll={{x: 1230}}
                toolBarRender={() => [
                    <Button key="create" type="primary" icon={<PlusOutlined/>} onClick={openCreateEditor}>
                        新建上架配置
                    </Button>,
                ]}
            />

            {editorOpen ? (
                <ModalForm<ActivityStageFormValues>
                    key={editingStage?.stageId ?? 'create'}
                    title={editingStage ? '编辑待上架配置' : '新建上架配置'}
                    width={640}
                    open
                    initialValues={toFormValues(editingStage)}
                    modalProps={{destroyOnClose: true, maskClosable: false, onCancel: () => setEditorOpen(false)}}
                    submitter={{searchConfig: {submitText: editingStage ? '保存修改' : '创建草稿', resetText: '取消'}}}
                    onOpenChange={setEditorOpen}
                    onFinish={submitStage}
                >
                    <ProForm.Group>
                        <ProFormText
                            name="channel"
                            label="投放渠道"
                            width="md"
                            placeholder="例如 web"
                            rules={[{required: true, message: '请输入投放渠道'}, {
                                pattern: /^[a-z][a-z0-9_-]{0,31}$/,
                                message: '投放渠道格式不合法'
                            }]}
                        />
                        <ProFormText
                            name="source"
                            label="流量来源"
                            width="md"
                            placeholder="例如 official"
                            rules={[{required: true, message: '请输入流量来源'}, {
                                pattern: /^[a-z][a-z0-9_-]{0,31}$/,
                                message: '流量来源格式不合法'
                            }]}
                        />
                    </ProForm.Group>
                    <ProFormSelect
                        name="activityId"
                        label="活动"
                        width="lg"
                        placeholder="输入活动 ID 或名称搜索"
                        request={loadOpenActivityOptions}
                        fieldProps={{showSearch: true, filterOption: false, allowClear: true}}
                        rules={[{required: true, message: '请选择已开放活动'}]}
                    />
                </ModalForm>
            ) : null}

            <Drawer title="活动展台详情" width={640} open={Boolean(detailStage)} loading={detailLoading}
                    onClose={() => setDetailStage(undefined)}>
                {detailStage ? <ActivityStageDetail stage={detailStage}/> : null}
            </Drawer>
        </PageContainer>
    );
}

function ActivityStageDetail({stage}: { stage: ActivityStageItem }) {
    const state = stateLabels[stage.state];
    return (
        <Descriptions bordered size="small" column={2}>
            <Descriptions.Item label="展台 ID" span={2}>{stage.stageId}</Descriptions.Item>
            <Descriptions.Item label="投放渠道">{stage.channel}</Descriptions.Item>
            <Descriptions.Item label="流量来源">{stage.source}</Descriptions.Item>
            <Descriptions.Item label="活动 ID" span={2}>{stage.activityId}</Descriptions.Item>
            <Descriptions.Item label="状态"><Tag color={state.color}>{state.text}</Tag></Descriptions.Item>
            <Descriptions.Item label="乐观锁版本">{stage.version}</Descriptions.Item>
            <Descriptions.Item label="创建人">{stage.createdBy}</Descriptions.Item>
            <Descriptions.Item label="修改人">{stage.updatedBy}</Descriptions.Item>
            <Descriptions.Item label="创建时间">{stage.createTime}</Descriptions.Item>
            <Descriptions.Item label="更新时间">{stage.updateTime}</Descriptions.Item>
        </Descriptions>
    );
}

function toFormValues(stage?: ActivityStageItem): ActivityStageFormValues {
    return stage
        ? {channel: stage.channel, source: stage.source, activityId: stage.activityId}
        : {channel: 'web', source: 'official', activityId: ''};
}

function loadOpenActivityOptions({keyWords}: { keyWords?: string }) {
    return queryOpenActivityOptions(keyWords);
}

function showPublicationResult(publication: ActivityStagePublicationItem) {
    if (publication.state === 'succeeded') {
        message.success(`活动已完成预热并发布，本次为第 ${publication.attemptCount} 次尝试`);
        return;
    }
    if (publication.state === 'failed') {
        message.error(formatPublicationFailure(publication));
        return;
    }
    message.warning(`发布任务仍在${publicationPhaseLabels[publication.phase]}，请稍后刷新查看结果`);
}

async function showPublicationFailure(stageId: string, stageVersion: number, error: unknown) {
    try {
        const publication = await getActivityStagePublication(stageId, stageVersion);
        if (publication.state === 'failed') {
            message.error(formatPublicationFailure(publication));
            return;
        }
    } catch {
        // 发布任务可能尚未创建，继续展示原始接口错误。
    }
    message.error(getApiErrorMessage(error, '活动发布失败，原生效活动保持不变'));
}

function formatPublicationFailure(publication: ActivityStagePublicationItem): string {
    const phase = publicationPhaseLabels[publication.phase];
    return `${phase}失败：${publication.failureReason ?? '请检查活动配置后重试'}`;
}

export default ActivityStagePage;
