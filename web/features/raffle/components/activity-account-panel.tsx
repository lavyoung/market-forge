import type {ActivityAccount} from "@/features/raffle/model/activity-raffle-types";
import styles from "./activity-account-panel.module.css";

type ActivityAccountPanelProps = {
    account: ActivityAccount | null;
    error: string | null;
};

type QuotaItem = {
    label: string;
    total: number;
    surplus: number;
};

/**
 * 展示当前用户在活动中的总、月、日抽奖额度。
 *
 * account 为 null 且没有错误时表示额度仍在加载。
 * 当前可抽次数取三个额度维度剩余次数的最小值，
 * 与后端需要同时满足总、月、日额度限制的规则保持一致。
 *
 * @param props 活动账户数据与加载错误
 * @returns 活动额度状态区域
 */
export default function ActivityAccountPanel({account, error}: ActivityAccountPanelProps) {
    if (error) {
        return (
            <section className={styles.panel} aria-labelledby="activity-account-title">
                <h2 id="activity-account-title">我的抽奖额度</h2>
                <p className={styles.error} role="alert">{error}</p>
            </section>
        );
    }

    if (!account) {
        return (
            <section className={styles.panel} aria-labelledby="activity-account-title" aria-busy="true">
                <h2 id="activity-account-title">我的抽奖额度</h2>
                <p className={styles.loading} role="status">正在加载活动额度……</p>
            </section>
        );
    }

    const availableCount = Math.min(account.totalCountSurplus, account.monthCountSurplus, account.dayCountSurplus);
    const quotaItems: QuotaItem[] = [
        {label: "活动总额度", total: account.totalCount, surplus: account.totalCountSurplus},
        {label: "本月额度", total: account.monthCount, surplus: account.monthCountSurplus},
        {label: "今日额度", total: account.dayCount, surplus: account.dayCountSurplus},
    ];

    return (
        <section className={styles.panel} aria-labelledby="activity-account-title">
            <div className={styles.heading}>
                <div>
                    <h2 id="activity-account-title">我的抽奖额度</h2>
                    <p>总、月、日额度需要同时满足</p>
                </div>
                <div className={styles.available}>
                    <strong>{availableCount}</strong>
                    <span>当前可抽</span>
                </div>
            </div>

            <dl className={styles.quotaList}>
                {quotaItems.map((quota) => (
                    <div className={styles.quotaItem} key={quota.label}>
                        <dt>{quota.label}</dt>
                        <dd>
                            <strong>{quota.surplus}</strong>
                            <span> / {quota.total}</span>
                        </dd>
                        <progress max={Math.max(quota.total, 1)} value={quota.surplus}>
                            剩余 {quota.surplus} 次，共 {quota.total} 次
                        </progress>
                    </div>
                ))}
            </dl>
        </section>
    );
}