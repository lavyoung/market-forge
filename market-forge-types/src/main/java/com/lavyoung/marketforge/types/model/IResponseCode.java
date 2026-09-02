package com.lavyoung.marketforge.types.model;

/**
 * 统一响应编码契约。
 * <p>
 * 每个响应编码由业务编码、国际化资源键和默认文案组成。业务编码用于程序判断和链路定位，
 * 国际化资源键用于按客户端语言解析提示信息，默认文案则在国际化资源缺失时作为兜底内容。
 * 实现类应保证三项信息稳定且业务编码全局唯一，避免因修改既有编码而影响调用方判断。
 * </p>
 * <p>
 * 建议：项目业务编码按 {@code AAA_BBB_CCC} 划分，其中 {@code AAA} 表示业务域、
 * {@code BBB} 表示业务场景、{@code CCC} 表示具体状态。下划线只用于提升源码可读性，
 * 不属于实际数值。
 * </p>
 *
 * @author lavyoung
 * @version 1.0.0
 * @email lavyoung1325@outlook.com
 * @date 2026/09/02
 * @see BusinessResponseCode
 */
public interface IResponseCode {

    /**
     * 获取全局唯一且稳定的业务编码。
     *
     * @return 业务编码
     */
    int getCode();

    /**
     * 获取国际化资源键。
     * <p>
     * 调用方可使用该键从消息资源中解析本地化文案；实现类必须返回非空值。
     * </p>
     *
     * @return 国际化资源键
     */
    String getI18nKey();

    /**
     * 获取默认响应文案。
     * <p>
     * 当国际化资源不存在或解析失败时使用该文案兜底；实现类必须返回非空值。
     * </p>
     *
     * @return 默认响应文案
     */
    String getMsg();
}
