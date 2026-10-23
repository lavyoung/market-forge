package com.lavyoung.marketforge.application.award.grant;

/**
 * 奖品履约配置校验器。
 *
 * <p>每个校验器负责一种 {@code awardType} 的一个配置版本。
 * 运营人员启用奖品前，应用服务通过校验器确认配置能够被当前系统识别。</p>
 *
 * <p>校验器不执行奖品发放，也不修改任何业务数据。配置非法时必须抛出
 * 统一业务异常，不能仅返回布尔值而丢失失败原因。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/23
 */
public interface AwardConfigValidator {

    /**
     * 返回当前校验器支持的奖品类型。
     *
     * @return 稳定的奖品类型编码
     */
    String awardType();

    /**
     * 返回当前校验器支持的配置版本。
     *
     * @return 大于零的配置版本
     */
    int configVersion();

    /**
     * 校验指定奖品配置。
     *
     * @param configContent 奖品结构化配置内容
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 配置格式、字段类型或业务取值非法时抛出
     */
    void validate(String configContent);
}
