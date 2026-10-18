import {fileURLToPath, URL} from "node:url";
import {defineConfig} from "vitest/config";

/**
 * 前端单元测试配置。
 *
 * 测试默认运行在 Node 环境，只验证无浏览器依赖的模型转换和 API 客户端。
 * 路径别名与 Next.js 项目保持一致，避免测试使用不同的导入方式。
 */
export default defineConfig({
    resolve: {
        alias: {
            "@": fileURLToPath(new URL("./", import.meta.url)),
        },
    },
    test: {
        environment: "node",
    },
});
