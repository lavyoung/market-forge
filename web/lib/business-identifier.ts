const POSITIVE_DECIMAL_IDENTIFIER_PATTERN = /^[1-9][0-9]*$/;

/**
 * 判断未知值是否为合法的十进制正整数业务标识。
 *
 * 业务标识在浏览器和 HTTP 协议中始终保持字符串，不转换成 number，
 * 避免超过 Number.MAX_SAFE_INTEGER 后发生精度丢失。
 *
 * 该校验只验证协议格式，不验证标识对应的活动、策略、奖品或 SKU
 * 是否真实存在；存在性和归属关系必须由服务端业务接口判断。
 *
 * @param value 接口响应、请求参数或表单中取得的未知值
 * @returns 非零开头且仅包含十进制数字的字符串返回 true
 */
export function isPositiveDecimalIdentifier(value: unknown): value is string {
    return typeof value === "string" && POSITIVE_DECIMAL_IDENTIFIER_PATTERN.test(value);
}
