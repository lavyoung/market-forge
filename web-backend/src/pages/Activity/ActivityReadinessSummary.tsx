import type {ActivityReadinessIssue, ActivityReadinessReport} from '@/services/activity/types';
import {InfoCircleOutlined, StopOutlined, WarningOutlined} from '@ant-design/icons';
import {Alert, List, Space, Tag, Typography} from 'antd';

/**
 * 活动就绪报告展示组件参数。
 */
interface ActivityReadinessSummaryProps {
    /** 服务端返回的最新活动就绪报告。 */
    report: ActivityReadinessReport;
    /** 当前操作名称，例如“开放”或“发布”。 */
    operationName: string;
    /** 补充说明当前操作通过检查后的影响。 */
    description: string;
}

/**
 * 统一展示活动开放和展台发布所使用的运行就绪报告。
 *
 * @param props 服务端报告、操作名称和影响说明
 * @returns 包含汇总状态以及错误、警告和建议列表的展示区域
 */
export function ActivityReadinessSummary({report, operationName, description}: ActivityReadinessSummaryProps) {
    const summaryType = report.ready ? (report.warnings.length > 0 ? 'warning' : 'success') : 'error';
    const summaryMessage = report.ready
        ? `活动已满足${operationName}条件`
        : `发现 ${report.errors.length} 个阻断问题`;

    return (
        <Space direction="vertical" size="middle" style={{width: '100%', marginTop: 12}}>
            <Alert
                showIcon
                type={summaryType}
                message={summaryMessage}
                description={`检查时间：${report.checkedAt}。${description}`}
            />
            <ReadinessIssueList title="阻断错误" color="error" issues={report.errors}/>
            <ReadinessIssueList title="风险提示" color="warning" issues={report.warnings}/>
            <ReadinessIssueList title="优化建议" color="processing" issues={report.suggestions}/>
        </Space>
    );
}

function ReadinessIssueList({title, color, issues}: {
    title: string;
    color: 'error' | 'warning' | 'processing';
    issues: ActivityReadinessIssue[];
}) {
    if (issues.length === 0) {
        return null;
    }

    const icon = color === 'error' ? <StopOutlined/> : color === 'warning' ? <WarningOutlined/> : <InfoCircleOutlined/>;
    return (
        <div>
            <Typography.Text strong>{title}</Typography.Text>
            <List
                size="small"
                dataSource={issues}
                renderItem={(issue) => (
                    <List.Item>
                        <Space align="start">
                            <Tag color={color} icon={icon}>{issue.path}</Tag>
                            <span>{issue.message}</span>
                        </Space>
                    </List.Item>
                )}
            />
        </div>
    );
}
