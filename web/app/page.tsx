"use client";

import {useState} from "react";
import styles from "./page.module.css";

type Prize = { name: string; description: string; icon: string; tone: string };

const prizes: Prize[] = [
    {name: "限定徽章", description: "稀有藏品", icon: "✦", tone: "violet"},
    {name: "8 元红包", description: "即时到账", icon: "¥", tone: "coral"},
    {name: "幸运奖", description: "隐藏惊喜", icon: "☘", tone: "green"},
    {name: "谢谢参与", description: "好运蓄力中", icon: "☺", tone: "slate"},
    {name: "100 积分", description: "积分商城可用", icon: "P", tone: "blue"},
    {name: "随机矿石", description: "价值随机", icon: "◆", tone: "cyan"},
    {name: "再抽一次", description: "次数 +1", icon: "↻", tone: "amber"},
    {name: "咖啡兑换券", description: "全国门店可用", icon: "☕", tone: "pink"},
];

const gridOrder = [0, 1, 2, 7, -1, 3, 6, 5, 4];
const winnerFeed = [
    {avatar: "林", name: "林*夏", prize: "抽中 8 元红包", time: "刚刚"},
    {avatar: "周", name: "周*宇", prize: "抽中随机矿石", time: "2 分钟前"},
    {avatar: "沈", name: "沈*之", prize: "抽中限定徽章", time: "5 分钟前"},
];
const delay = (milliseconds: number) => new Promise((resolve) => window.setTimeout(resolve, milliseconds));

export default function Home() {
    const [activePrize, setActivePrize] = useState<number | null>(null);
    const [result, setResult] = useState<Prize | null>(null);
    const [remaining, setRemaining] = useState(3);
    const [isDrawing, setIsDrawing] = useState(false);
    const [showRules, setShowRules] = useState(false);

    const draw = async () => {
        if (isDrawing || remaining === 0) return;
        setResult(null);
        setIsDrawing(true);
        setRemaining((value) => value - 1);
        const winnerIndex = Math.floor(Math.random() * prizes.length);
        const totalSteps = prizes.length * 3 + winnerIndex + 1;

        for (let step = 0; step < totalSteps; step += 1) {
            setActivePrize(step % prizes.length);
            await delay(step > totalSteps - 6 ? 150 + (step - totalSteps + 6) * 35 : 72);
        }
        setActivePrize(winnerIndex);
        if (prizes[winnerIndex].name === "再抽一次") {
            setRemaining((value) => value + 1);
        }
        setResult(prizes[winnerIndex]);
        setIsDrawing(false);
    };

    return (
        <main className={styles.pageShell}>
            <div className={styles.auroraOne}/>
            <div className={styles.auroraTwo}/>
            <header className={styles.header}>
                <a className={styles.brand} href="#top" aria-label="Market Forge 首页"><span
                    className={styles.brandMark}>M</span><span>MARKET FORGE</span></a>
                <nav className={styles.nav} aria-label="页面导航"><a href="#prizes">奖品一览</a><a
                    href="#winners">中奖动态</a>
                    <button type="button" onClick={() => setShowRules(true)}>活动规则</button>
                </nav>
                <div className={styles.account}><span
                    className={styles.onlineDot}/><span>我的积分</span><strong>2,480</strong></div>
            </header>

            <section className={styles.hero} id="top">
                <div className={styles.heroCopy}>
                    <div className={styles.eyebrow}><span>LUCKY DROP</span><i/><span>限时开放</span></div>
                    <h1>今天的好运，<span>正在派送。</span></h1>
                    <p className={styles.lead}>每一次点击，都是一场小小的未知实验。<br/>解锁你的今日幸运值，把惊喜收入囊中。
                    </p>
                    <div className={styles.heroStats}>
                        <div><strong>12,860</strong><span>今日参与</span></div>
                        <div><strong>2,106</strong><span>奖品已送出</span></div>
                        <div><strong>99.9%</strong><span>惊喜发生率</span></div>
                    </div>
                    <p className={styles.finePrint}>活动时间：09.01 — 09.30 · 每日 00:00 重置次数</p>
                </div>

                <div className={styles.raffleCard} id="prizes">
                    <div className={styles.cardHeader}>
                        <div><span className={styles.cardKicker}>LUCKY MATRIX / 09</span><h2>幸运九宫格</h2></div>
                        <div className={styles.chanceBadge}>
                            <span>今日剩余</span><strong>{remaining}</strong><span>次</span></div>
                    </div>
                    <div className={styles.prizeGrid} aria-label="抽奖奖品列表">
                        {gridOrder.map((prizeIndex, cellIndex) => {
                            if (prizeIndex === -1) return (
                                <button className={styles.drawButton} disabled={isDrawing || remaining === 0} key="draw"
                                        onClick={draw} type="button">
                                    <span
                                        className={styles.drawSpark}>✦</span><strong>{isDrawing ? "好运加载中" : remaining === 0 ? "明日再来" : "立即抽奖"}</strong><small>{isDrawing ? "请稍候…" : remaining === 0 ? "今日次数已用完" : "消耗 1 次机会"}</small>
                                </button>
                            );
                            const prize = prizes[prizeIndex];
                            return <div
                                className={`${styles.prizeCell} ${styles[prize.tone]} ${activePrize === prizeIndex ? styles.active : ""}`}
                                key={`${prize.name}-${cellIndex}`}><span
                                className={styles.prizeIcon}>{prize.icon}</span><strong>{prize.name}</strong><small>{prize.description}</small>
                            </div>;
                        })}
                    </div>
                    <div className={styles.cardFooter}><span><i className={styles.liveDot}/> 奖池实时更新中</span><span>公平概率 · 结果可追溯</span>
                    </div>
                </div>
            </section>

            <section className={styles.bottomGrid} id="winners">
                <article className={styles.winnerPanel}>
                    <div className={styles.sectionHeading}>
                        <div><span>LIVE FEED</span><h2>好运正在发生</h2></div>
                        <span className={styles.livePill}><i/>实时</span></div>
                    <div className={styles.winnerList}>{winnerFeed.map((item) => <div className={styles.winnerItem}
                                                                                      key={item.name}><span
                        className={styles.avatar}>{item.avatar}</span>
                        <div><strong>{item.name}</strong><span>{item.prize}</span></div>
                        <time>{item.time}</time>
                    </div>)}</div>
                </article>
                <article className={styles.promisePanel}><span className={styles.promiseIcon}>✓</span>
                    <div><span className={styles.panelLabel}>MARKET FORGE PROMISE</span><h2>每一份好运，都有迹可循</h2>
                        <p>规则透明、结果留痕。中奖后奖品将在 24 小时内自动发放至账户。</p></div>
                    <button type="button" onClick={() => setShowRules(true)} aria-label="查看活动规则">↗</button>
                </article>
            </section>
            <footer className={styles.footer}><span>© 2026 MARKET FORGE</span><span>让每一次选择，都更接近惊喜。</span>
            </footer>

            {result && <div className={styles.modalBackdrop} role="presentation" onMouseDown={() => setResult(null)}>
                <section aria-labelledby="result-title" aria-modal="true" className={styles.resultModal}
                         onMouseDown={(event) => event.stopPropagation()} role="dialog">
                    <button className={styles.closeButton} onClick={() => setResult(null)} type="button"
                            aria-label="关闭">×
                    </button>
                    <div className={styles.resultBurst}><span>{result.icon}</span></div>
                    <span className={styles.resultKicker}>LUCK IS YOURS</span><h2 id="result-title">恭喜抽中</h2><strong
                    className={styles.resultName}>{result.name}</strong>
                    <p>{result.description}，奖品稍后将发放至你的账户。</p>
                    <button className={styles.resultAction} onClick={() => setResult(null)} type="button">开心收下
                    </button>
                </section>
            </div>}

            {showRules &&
                <div className={styles.modalBackdrop} role="presentation" onMouseDown={() => setShowRules(false)}>
                    <section aria-labelledby="rules-title" aria-modal="true"
                             className={`${styles.resultModal} ${styles.rulesModal}`}
                             onMouseDown={(event) => event.stopPropagation()} role="dialog">
                        <button className={styles.closeButton} onClick={() => setShowRules(false)} type="button"
                                aria-label="关闭">×
                        </button>
                        <span className={styles.resultKicker}>ACTIVITY RULES</span><h2 id="rules-title">活动规则</h2>
                        <ol>
                            <li><span>01</span>每位用户每天拥有 3 次免费抽奖机会。</li>
                            <li><span>02</span>中奖结果以页面展示及账户记录为准。</li>
                            <li><span>03</span>虚拟奖品将在 24 小时内自动发放。</li>
                            <li><span>04</span>如遇异常，未完成的抽奖次数将自动返还。</li>
                        </ol>
                        <button className={styles.resultAction} onClick={() => setShowRules(false)}
                                type="button">我知道了
                        </button>
                    </section>
                </div>}
        </main>
    );
}
