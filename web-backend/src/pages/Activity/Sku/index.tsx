import {
    adjustActivitySkuStock,
    createActivitySkuDefinition,
    getActivitySkuDefinition,
    queryActivitySkuDefinitions,
    updateActivitySkuDefinition,
} from '@/services/activity/activity-sku-definition';
import {getActivityCountDefinition} from '@/services/activity/activity-count-definition';
import {getActivityDefinition, queryDraftActivityOptions} from '@/services/activity/activity-definition';
import type {
    ActivitySkuDefinitionFormValues,
    ActivitySkuDefinitionItem,
    ActivitySkuDefinitionQuery,
    ActivitySkuStockAdjustmentFormValues,
} from '@/services/activity/types';
import {getApiErrorMessage} from '@/services/errors';
import {EditOutlined, EyeOutlined, PlusOutlined, SwapOutlined} from '@ant-design/icons';
import type {ActionType, ProColumns, ProFormInstance} from '@ant-design/pro-components';
import {
    ModalForm,
    PageContainer,
    ProForm,
    ProFormDigit,
    ProFormSelect,
    ProFormTextArea,
    ProTable,
} from '@ant-design/pro-components';
import {Alert, Button, Descriptions, Drawer, message, Progress, Tag, Typography} from 'antd';
import {useRef, useState} from 'react';

type SelectOption = { label: string; value: string };

/**
 * 活动 SKU 运营管理页面。
 *
 * @returns SKU 列表、配置编辑器、详情抽屉和库存调整弹窗
 */
function ActivitySkuDefinitionPage() {
    const actionRef = useRef<ActionType>();
    const editorFormRef = useRef<ProFormInstance<ActivitySkuDefinitionFormValues>>();
    const [editorOpen, setEditorOpen] = useState(false);
    const [stockEditorOpen, setStockEditorOpen] = useState(false);
    const [editingDefinition, setEditingDefinition] = useState<ActivitySkuDefinitionItem>();
    const [adjustingDefinition, setAdjustingDefinition] = useState<ActivitySkuDefinitionItem>();
    const [detailDefinition, setDetailDefinition] = useState<ActivitySkuDefinitionItem>();
    const [activityCountOptions, setActivityCountOptions] = useState<SelectOption[]>([]);

    const openCreateEditor = () => {
        setEditingDefinition(undefined);
        setActivityCountOptions([]);
        setEditorOpen(true);
    };

    const openEditEditor = async (definition: ActivitySkuDefinitionItem) => {
        try {
            const latestDefinition = await getActivitySkuDefinition(definition.sku);
            const activity = await getActivityDefinition(latestDefinition.activityId);
            if (activity.state !== 'draft') {
                message.warning('只有草稿活动的 SKU 可以修改');
                return;
            }
            const countDefinition = await getActivityCountDefinition(latestDefinition.activityCountId);
            setActivityCountOptions([toActivityCountOption(countDefinition)]);
            setEditingDefinition(latestDefinition);
            setEditorOpen(true);
        } catch (error) {
            message.error(getApiErrorMessage(error, '活动 SKU 详情加载失败'));
        }
    };

    const openDetail = async (definition: ActivitySkuDefinitionItem) => {
        try {
            setDetailDefinition(await getActivitySkuDefinition(definition.sku));
        } catch (error) {
            message.error(getApiErrorMessage(error, '活动 SKU 详情加载失败'));
        }
    };

    const openStockEditor = async (definition: ActivitySkuDefinitionItem) => {
        try {
            const latestDefinition = await getActivitySkuDefinition(definition.sku);
            const activity = await getActivityDefinition(latestDefinition.activityId);
            if (activity.state !== 'draft') {
                message.warning('只有草稿活动的 SKU 可以调整库存');
                return;
            }
            setAdjustingDefinition(latestDefinition);
            setStockEditorOpen(true);
        } catch (error) {
            message.error(getApiErrorMessage(error, '活动 SKU 详情加载失败'));
        }
    };

    const handleActivityChange = async (activityId?: string) => {
        if (!activityId) {
            setActivityCountOptions([]);
            editorFormRef.current?.setFieldsValue({activityCountId: ''});
            return;
        }
        try {
            const activity = await getActivityDefinition(activityId);
            const countDefinition = await getActivityCountDefinition(activity.activityCountId);
            const countOption = toActivityCountOption(countDefinition);
            setActivityCountOptions([countOption]);
            editorFormRef.current?.setFieldsValue({activityCountId: countOption.value});
        } catch (error) {
            setActivityCountOptions([]);
            editorFormRef.current?.setFieldsValue({activityCountId: ''});
            message.error(getApiErrorMessage(error, '活动关联的次数配置加载失败'));
        }
    };

    const submitDefinition = async (values: ActivitySkuDefinitionFormValues): Promise<boolean> => {
        try {
            if (editingDefinition) {
                await updateActivitySkuDefinition(editingDefinition.sku, editingDefinition.version, values);
                message.success('活动 SKU 配置已更新');
            } else {
                await createActivitySkuDefinition(values);
                message.success('活动 SKU 已创建');
            }
            await actionRef.current?.reload();
            return true;
        } catch (error) {
            message.error(getApiErrorMessage(error, editingDefinition ? '活动 SKU 更新失败' : '活动 SKU 创建失败'));
            return false;
        }
    };

    const submitStockAdjustment = async (values: ActivitySkuStockAdjustmentFormValues): Promise<boolean> => {
        if (!adjustingDefinition) {
            return false;
        }
        if (values.quantityDelta === 0) {
            message.error('库存调整量不能为 0');
            return false;
        }
        if (values.quantityDelta < 0 && Math.abs(values.quantityDelta) > adjustingDefinition.stockCountSurplus) {
            message.error('收回数量不能超过当前剩余库存');
            return false;
        }
        if (adjustingDefinition.stockCount + values.quantityDelta <= 0) {
            message.error('调整后的库存总量必须大于 0');
            return false;
        }
        try {
            await adjustActivitySkuStock(adjustingDefinition.sku, adjustingDefinition.version, values);
            message.success('活动 SKU 库存已调整');
            await actionRef.current?.reload();
            return true;
        } catch (error) {
            message.error(getApiErrorMessage(error, '活动 SKU 库存调整失败'));
            return false;
        }
    };

    const columns: ProColumns<ActivitySkuDefinitionItem>[] = [
        {title: 'SKU', dataIndex: 'sku', copyable: true, width: 180},
        {title: '活动 ID', dataIndex: 'activityId', copyable: true, width: 180},
        {title: '次数配置 ID', dataIndex: 'activityCountId', copyable: true, width: 180},
        {
            title: '库存',
            dataIndex: 'stockCountSurplus',
            hideInSearch: true,
            width: 210,
            render: (_, record) => <SkuStockProgress definition={record}/>,
        },
        {
            title: '积分价格',
            dataIndex: 'creditAmount',
            hideInSearch: true,
            width: 120,
            render: (_, record) => record.supportsCreditExchange
                ? <Typography.Text>{record.creditAmount?.toLocaleString()}</Typography.Text>
                : <Tag>不支持兑换</Tag>,
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
                <Button key="detail" type="link" icon={<EyeOutlined/>} onClick={() => openDetail(record)}>查看</Button>,
                <Button key="edit" type="link" icon={<EditOutlined/>}
                        onClick={() => openEditEditor(record)}>编辑</Button>,
                <Button key="stock" type="link" icon={<SwapOutlined/>}
                        onClick={() => openStockEditor(record)}>调整库存</Button>,
            ],
        },
    ];

    return (
        <PageContainer title="活动 SKU" subTitle="管理积分兑换额度商品、活动次数关联和可用库存">
            <ProTable<ActivitySkuDefinitionItem, ActivitySkuDefinitionQuery>
                actionRef={actionRef}
                rowKey="sku"
                headerTitle="活动 SKU 列表"
                columns={columns}
                request={queryActivitySkuDefinitions}
                search={{labelWidth: 'auto', defaultCollapsed: false, collapseRender: false, span: 8}}
                pagination={{defaultPageSize: 20, showSizeChanger: true}}
                scroll={{x: 1480}}
                toolBarRender={() => [
                    <Button key="create" type="primary" icon={<PlusOutlined/>}
                            onClick={openCreateEditor}>新建活动 SKU</Button>,
                ]}
            />

            {editorOpen ? (
                <ModalForm<ActivitySkuDefinitionFormValues>
                    key={editingDefinition?.sku ?? 'create'}
                    formRef={editorFormRef}
                    title={editingDefinition ? '编辑活动 SKU' : '新建活动 SKU'}
                    width={720}
                    open
                    initialValues={toDefinitionFormValues(editingDefinition)}
                    modalProps={{destroyOnClose: true, maskClosable: false, onCancel: () => setEditorOpen(false)}}
                    submitter={{
                        searchConfig: {
                            submitText: editingDefinition ? '保存修改' : '创建 SKU',
                            resetText: '取消'
                        }
                    }}
                    onOpenChange={setEditorOpen}
                    onFinish={submitDefinition}
                >
                    <Alert type="info" showIcon
                           message="SKU 只能配置在草稿活动下；次数配置由所选活动决定，不能单独改成其他配置。"/>
                    <ProForm.Group>
                        <ProFormSelect name="activityId" label="所属草稿活动" width="md"
                                       placeholder="输入活动 ID 或名称搜索"
                                       request={loadDraftActivityOptions}
                                       fieldProps={{
                                           showSearch: true,
                                           filterOption: false,
                                           allowClear: true,
                                           onChange: handleActivityChange
                                       }}
                                       rules={[{required: true, message: '请选择所属草稿活动'}]}/>
                        <ProFormSelect name="activityCountId" label="活动次数配置" width="md"
                                       placeholder="选择活动后自动匹配"
                                       options={activityCountOptions}
                                       fieldProps={{disabled: activityCountOptions.length === 0}}
                                       rules={[{required: true, message: '所选活动缺少有效次数配置'}]}/>
                    </ProForm.Group>
                    {!editingDefinition ? (
                        <ProFormDigit name="stockCount" label="初始库存总量" width="md" min={1}
                                      fieldProps={{precision: 0}}
                                      rules={[{required: true, message: '请输入初始库存'}]}/>
                    ) : (
                        <Alert type="warning" showIcon
                               message={`库存不会通过普通编辑重置。当前总库存 ${editingDefinition.stockCount.toLocaleString()}，剩余 ${editingDefinition.stockCountSurplus.toLocaleString()}。`}/>
                    )}
                    <ProFormDigit name="creditAmount" label="积分兑换价格" width="md" min={1}
                                  tooltip="留空表示该 SKU 不支持积分兑换"
                                  placeholder="留空则关闭积分兑换"
                                  fieldProps={{precision: 0}}/>
                </ModalForm>
            ) : null}

            {stockEditorOpen && adjustingDefinition ? (
                <ModalForm<ActivitySkuStockAdjustmentFormValues>
                    key={`${adjustingDefinition.sku}-${adjustingDefinition.version}`}
                    title="调整活动 SKU 库存"
                    width={640}
                    open
                    initialValues={{quantityDelta: 1, reason: ''}}
                    modalProps={{destroyOnClose: true, maskClosable: false, onCancel: () => setStockEditorOpen(false)}}
                    submitter={{searchConfig: {submitText: '确认调整', resetText: '取消'}}}
                    onOpenChange={setStockEditorOpen}
                    onFinish={submitStockAdjustment}
                >
                    <SkuStockSnapshot definition={adjustingDefinition}/>
                    <Alert type="info" showIcon
                           message="正数表示补充库存，负数表示收回尚未消费的库存；总库存和剩余库存会同步增减。"/>
                    <ProFormDigit name="quantityDelta" label="库存调整量" width="md"
                                  fieldProps={{precision: 0}}
                                  rules={[{required: true, message: '请输入库存调整量'}, {
                                      validator: async (_: unknown, value?: number) => value === 0
                                          ? Promise.reject(new Error('库存调整量不能为 0'))
                                          : Promise.resolve(),
                                  }]}/>
                    <ProFormTextArea name="reason" label="调整原因" width="lg"
                                     fieldProps={{showCount: true, maxLength: 256}}
                                     rules={[{required: true, whitespace: true, message: '请输入库存调整原因'}, {
                                         max: 256,
                                         message: '调整原因不能超过 256 个字符',
                                     }]}/>
                </ModalForm>
            ) : null}

            <Drawer title="活动 SKU 详情" width={700} open={Boolean(detailDefinition)}
                    onClose={() => setDetailDefinition(undefined)}>
                {detailDefinition ? <ActivitySkuDefinitionDetail definition={detailDefinition}/> : null}
            </Drawer>
        </PageContainer>
    );
}

function loadDraftActivityOptions({keyWords}: { keyWords?: string }) {
    return queryDraftActivityOptions(keyWords);
}

function toActivityCountOption(definition: {
    activityCountId: string;
    activityCountName: string;
    dayCount: number;
    monthCount: number;
    totalCount: number
}): SelectOption {
    return {
        label: `${definition.activityCountName}（日 ${definition.dayCount} / 月 ${definition.monthCount} / 总 ${definition.totalCount}）`,
        value: definition.activityCountId,
    };
}

function toDefinitionFormValues(definition?: ActivitySkuDefinitionItem): ActivitySkuDefinitionFormValues {
    return definition
        ? {
            activityId: definition.activityId,
            activityCountId: definition.activityCountId,
            creditAmount: definition.creditAmount ?? undefined,
        }
        : {activityId: '', activityCountId: '', stockCount: 100, creditAmount: 100};
}

function SkuStockProgress({definition}: { definition: ActivitySkuDefinitionItem }) {
    const remainingPercent = definition.stockCount > 0
        ? Math.min(Math.round((Math.max(definition.stockCountSurplus, 0) / definition.stockCount) * 100), 100)
        : 0;
    return (
        <div>
            <Typography.Text>
                {definition.stockCountSurplus.toLocaleString()} / {definition.stockCount.toLocaleString()}
            </Typography.Text>
            <Progress percent={remainingPercent} size="small" showInfo={false}/>
        </div>
    );
}

function SkuStockSnapshot({definition}: { definition: ActivitySkuDefinitionItem }) {
    return (
        <Descriptions bordered size="small" column={3} style={{marginBottom: 16}}>
            <Descriptions.Item label="库存总量">{definition.stockCount.toLocaleString()}</Descriptions.Item>
            <Descriptions.Item label="剩余库存">{definition.stockCountSurplus.toLocaleString()}</Descriptions.Item>
            <Descriptions.Item label="已消费">{definition.consumedCount.toLocaleString()}</Descriptions.Item>
        </Descriptions>
    );
}

function ActivitySkuDefinitionDetail({definition}: { definition: ActivitySkuDefinitionItem }) {
    return (
        <Descriptions bordered size="small" column={2}>
            <Descriptions.Item label="SKU" span={2}>{definition.sku}</Descriptions.Item>
            <Descriptions.Item label="所属活动">{definition.activityId}</Descriptions.Item>
            <Descriptions.Item label="次数配置">{definition.activityCountId}</Descriptions.Item>
            <Descriptions.Item label="库存总量">{definition.stockCount.toLocaleString()}</Descriptions.Item>
            <Descriptions.Item label="剩余库存">{definition.stockCountSurplus.toLocaleString()}</Descriptions.Item>
            <Descriptions.Item label="已消费库存">{definition.consumedCount.toLocaleString()}</Descriptions.Item>
            <Descriptions.Item label="积分价格">
                {definition.supportsCreditExchange ? definition.creditAmount?.toLocaleString() : '不支持积分兑换'}
            </Descriptions.Item>
            <Descriptions.Item label="乐观锁版本">{definition.version}</Descriptions.Item>
            <Descriptions.Item label="创建人">{definition.createdBy}</Descriptions.Item>
            <Descriptions.Item label="修改人">{definition.updatedBy}</Descriptions.Item>
            <Descriptions.Item label="创建时间">{definition.createTime}</Descriptions.Item>
            <Descriptions.Item label="更新时间">{definition.updateTime}</Descriptions.Item>
        </Descriptions>
    );
}

export default ActivitySkuDefinitionPage;
