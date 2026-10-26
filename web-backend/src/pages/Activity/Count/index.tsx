import {
    createActivityCountDefinition,
    getActivityCountDefinition,
    queryActivityCountDefinitions,
    updateActivityCountDefinition,
} from '@/services/activity/activity-count-definition';
import type {
    ActivityCountDefinitionFormValues,
    ActivityCountDefinitionItem,
    ActivityCountDefinitionQuery,
} from '@/services/activity/types';
import {getApiErrorMessage} from '@/services/errors';
import {EditOutlined, EyeOutlined, PlusOutlined} from '@ant-design/icons';
import type {ActionType, ProColumns} from '@ant-design/pro-components';
import {ModalForm, PageContainer, ProForm, ProFormDigit, ProFormText, ProTable} from '@ant-design/pro-components';
import {Alert, Button, Descriptions, Drawer, message} from 'antd';
import {useRef, useState} from 'react';

/**
 * 活动次数配置运营管理页面。
 *
 * @returns 次数配置列表、创建编辑表单和详情抽屉
 */
function ActivityCountDefinitionPage() {
    const actionRef = useRef<ActionType>();
    const [editorOpen, setEditorOpen] = useState(false);
    const [editingDefinition, setEditingDefinition] = useState<ActivityCountDefinitionItem>();
    const [detailDefinition, setDetailDefinition] = useState<ActivityCountDefinitionItem>();

    const openCreateEditor = () => {
        setEditingDefinition(undefined);
        setEditorOpen(true);
    };

    const openEditEditor = async (definition: ActivityCountDefinitionItem) => {
        try {
            setEditingDefinition(await getActivityCountDefinition(definition.activityCountId));
            setEditorOpen(true);
        } catch (error) {
            message.error(getApiErrorMessage(error, '次数配置详情加载失败'));
        }
    };

    const openDetail = async (definition: ActivityCountDefinitionItem) => {
        try {
            setDetailDefinition(await getActivityCountDefinition(definition.activityCountId));
        } catch (error) {
            message.error(getApiErrorMessage(error, '次数配置详情加载失败'));
        }
    };

    const submitDefinition = async (values: ActivityCountDefinitionFormValues): Promise<boolean> => {
        if (values.dayCount > values.monthCount || values.monthCount > values.totalCount) {
            message.error('次数必须满足：日次数 ≤ 月次数 ≤ 总次数');
            return false;
        }
        try {
            if (editingDefinition) {
                await updateActivityCountDefinition(editingDefinition.activityCountId, editingDefinition.version, values);
                message.success('活动次数配置已更新');
            } else {
                await createActivityCountDefinition(values);
                message.success('活动次数配置已创建');
            }
            await actionRef.current?.reload();
            return true;
        } catch (error) {
            message.error(getApiErrorMessage(error, editingDefinition ? '活动次数配置更新失败' : '活动次数配置创建失败'));
            return false;
        }
    };

    const columns: ProColumns<ActivityCountDefinitionItem>[] = [
        {title: '配置 ID', dataIndex: 'activityCountId', copyable: true, width: 170},
        {title: '配置名称', dataIndex: 'activityCountName', ellipsis: true, width: 220},
        {title: '日次数', dataIndex: 'dayCount', hideInSearch: true, width: 100},
        {title: '月次数', dataIndex: 'monthCount', hideInSearch: true, width: 100},
        {title: '总次数', dataIndex: 'totalCount', hideInSearch: true, width: 100},
        {title: '版本', dataIndex: 'version', hideInSearch: true, width: 80},
        {title: '修改人', dataIndex: 'updatedBy', hideInSearch: true, ellipsis: true, width: 120},
        {title: '更新时间', dataIndex: 'updateTime', valueType: 'dateTime', hideInSearch: true, width: 170},
        {
            title: '操作',
            valueType: 'option',
            fixed: 'right',
            width: 160,
            render: (_, record) => [
                <Button key="detail" type="link" icon={<EyeOutlined/>} onClick={() => openDetail(record)}>查看</Button>,
                <Button key="edit" type="link" icon={<EditOutlined/>}
                        onClick={() => openEditEditor(record)}>编辑</Button>,
            ],
        },
    ];

    return (
        <PageContainer title="活动次数配置" subTitle="管理活动 SKU 入账时增加的总、月、日抽奖额度">
            <ProTable<ActivityCountDefinitionItem, ActivityCountDefinitionQuery>
                actionRef={actionRef}
                rowKey="activityCountId"
                headerTitle="次数配置列表"
                columns={columns}
                request={queryActivityCountDefinitions}
                search={{labelWidth: 'auto', defaultCollapsed: false, collapseRender: false, span: 8}}
                pagination={{defaultPageSize: 20, showSizeChanger: true}}
                scroll={{x: 1200}}
                toolBarRender={() => [
                    <Button key="create" type="primary" icon={<PlusOutlined/>}
                            onClick={openCreateEditor}>新建次数配置</Button>,
                ]}
            />

            {editorOpen ? (
                <ModalForm<ActivityCountDefinitionFormValues>
                    key={editingDefinition?.activityCountId ?? 'create'}
                    title={editingDefinition ? '编辑活动次数配置' : '新建活动次数配置'}
                    width={680}
                    open
                    initialValues={toFormValues(editingDefinition)}
                    modalProps={{destroyOnClose: true, maskClosable: false, onCancel: () => setEditorOpen(false)}}
                    submitter={{
                        searchConfig: {
                            submitText: editingDefinition ? '保存修改' : '创建配置',
                            resetText: '取消'
                        }
                    }}
                    onOpenChange={setEditorOpen}
                    onFinish={submitDefinition}
                >
                    <Alert type="info" showIcon
                           message="次数表示一次 SKU 入账实际增加的额度，0 不代表不限。必须满足：日次数 ≤ 月次数 ≤ 总次数。"/>
                    <ProFormText name="activityCountName" label="配置名称" width="lg"
                                 rules={[{required: true, message: '请输入配置名称'}, {
                                     max: 64,
                                     message: '配置名称不能超过 64 个字符'
                                 }]}/>
                    <ProForm.Group>
                        <ProFormDigit name="dayCount" label="日次数" width="sm" min={1} fieldProps={{precision: 0}}
                                      rules={[{required: true, message: '请输入日次数'}]}/>
                        <ProFormDigit name="monthCount" label="月次数" width="sm" min={1} fieldProps={{precision: 0}}
                                      rules={[{required: true, message: '请输入月次数'}]}/>
                        <ProFormDigit name="totalCount" label="总次数" width="sm" min={1} fieldProps={{precision: 0}}
                                      rules={[{required: true, message: '请输入总次数'}]}/>
                    </ProForm.Group>
                </ModalForm>
            ) : null}

            <Drawer title="活动次数配置详情" width={640} open={Boolean(detailDefinition)}
                    onClose={() => setDetailDefinition(undefined)}>
                {detailDefinition ? <ActivityCountDefinitionDetail definition={detailDefinition}/> : null}
            </Drawer>
        </PageContainer>
    );
}

function ActivityCountDefinitionDetail({definition}: { definition: ActivityCountDefinitionItem }) {
    return (
        <Descriptions bordered size="small" column={2}>
            <Descriptions.Item label="配置 ID" span={2}>{definition.activityCountId}</Descriptions.Item>
            <Descriptions.Item label="配置名称" span={2}>{definition.activityCountName}</Descriptions.Item>
            <Descriptions.Item label="日次数">{definition.dayCount}</Descriptions.Item>
            <Descriptions.Item label="月次数">{definition.monthCount}</Descriptions.Item>
            <Descriptions.Item label="总次数">{definition.totalCount}</Descriptions.Item>
            <Descriptions.Item label="乐观锁版本">{definition.version}</Descriptions.Item>
            <Descriptions.Item label="创建人">{definition.createdBy}</Descriptions.Item>
            <Descriptions.Item label="修改人">{definition.updatedBy}</Descriptions.Item>
            <Descriptions.Item label="创建时间">{definition.createTime}</Descriptions.Item>
            <Descriptions.Item label="更新时间">{definition.updateTime}</Descriptions.Item>
        </Descriptions>
    );
}

function toFormValues(definition?: ActivityCountDefinitionItem): ActivityCountDefinitionFormValues {
    return definition
        ? {
            activityCountName: definition.activityCountName,
            totalCount: definition.totalCount,
            dayCount: definition.dayCount,
            monthCount: definition.monthCount,
        }
        : {activityCountName: '', totalCount: 100, dayCount: 10, monthCount: 50};
}

export default ActivityCountDefinitionPage;
