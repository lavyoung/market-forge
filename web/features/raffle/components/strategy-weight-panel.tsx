import type {ActivityAward} from "@/features/raffle/model/activity-raffle-types";
import {buildStrategyWeightTiers} from "@/features/raffle/model/strategy-weight-view";
import styles from "./strategy-weight-panel.module.css";

type StrategyWeightPanelProps = {
    awards: ActivityAward[];
};

/**
 * 展示当前活动的累计抽奖权重门槛。
 *
 * 组件只展示服务端已经计算的资格结果，不使用账户余额自行判断权重资格。
 * 每个档位展示达到该门槛时首次进入权重奖池的奖品。
 *
 * @param props 当前用户视角下的活动奖品
 * @returns 权重奖池进度区域；活动未配置权重规则时返回 null
 */
export default function StrategyWeightPanel({awards}: StrategyWeightPanelProps) {
    const tiers = buildStrategyWeightTiers(awards);

    if (tiers.length === 0) {
        return null;
    }

    return (
        <section className={styles.panel} aria-labelledby="strategy-weight-title">
            <div className={styles.heading}>
                <h2 id="strategy-weight-title">累计抽奖权重奖池</h2>
                <p>累计抽奖达到门槛后，对应奖品开始进入权重奖池。</p>
            </div>

            <ol className={styles.tierList}>
                {tiers.map((tier) => {
                    const progress = tier.unlocked ? tier.threshold : Math.max(0, tier.threshold - tier.remainingCount);

                    return (
                        <li className={styles.tier} key={tier.threshold}>
                            <div className={styles.tierHeading}>
                                <strong>累计 {tier.threshold.toLocaleString()} 次</strong>
                                <span className={tier.unlocked ? styles.unlocked : styles.locked}>
                                    {tier.unlocked ? "已解锁" : `还差 ${tier.remainingCount.toLocaleString()} 次`}
                                </span>
                            </div>

                            <progress max={tier.threshold} value={progress}>
                                当前进度 {progress}，门槛 {tier.threshold}
                            </progress>

                            <p>
                                <span>新增奖品：</span>
                                {tier.awards.map((award) => award.awardTitle).join("、")}
                            </p>
                        </li>
                    );
                })}
            </ol>
        </section>
    );
}