import "server-only" // server-only 用于防止这个文件被意外导入 Client Component。

/**
 * 生成与 Java 后端结构一致的错误响应。
 */
export function errorResponse(status: number, code: number, message: string): Response {
    return Response.json(
        {
            code,
            message,
            data: null,
        },
        {
            status,
            headers: {
                "Cache-Control": "no-store",
            }
        }
    );
}

/**
 * 向 Java 服务发送请求。
 *
 * 不缓存接口响应，不自动重试请求。
 */
export async function forwardMarketForge(path: string, init: RequestInit = {}): Promise<Response> {
    const baseUrl = process.env.MARKET_FORGE_API_BASE_URL;
    if (!baseUrl) {
        throw new Error("MARKET_FORGE_API_BASE_URL is not configured");
    }

    const headers = new Headers(init?.headers);
    headers.set("Accept", "application/josn");

    if (init.body != null) {
        headers.set("Content-Type", "application/json");
    }

    return fetch(`${baseUrl.replace(/\/+$/, "")}${path}`, {
        ...init,
        cache: "no-cache",
        headers,
        signal: AbortSignal.timeout(15_000),
    })
}

/**
 * 保留 Java 返回的 HTTP 状态和 JSON 内容。
 */
export async function toProxyResponse(upstream: Response): Promise<Response> {
    const contentType = upstream.headers.get("Content-Type") ?? "";

    if (!contentType.toLowerCase().includes("application/json")) {
        throw new Error("服务未返回 JSON");
    }

    return new Response(await upstream.text(), {
        status: upstream.status,
        headers: {
            "Content-Type": contentType,
            "Cache-Control": "no-store",
        },
    });
}

/**
 * 返回服务不可用响应，不暴露内部连接信息。
 */
export function badGatewayResponse(message = "后端服务暂时不可用，请稍后再试",): Response {
    return errorResponse(502, 900_000_001, message);
}