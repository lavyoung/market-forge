import {defineConfig} from '@umijs/max';

export default defineConfig({
    antd: {},
    model: {},
    initialState: {},
    request: {},
    history: {
        type: 'hash',
    },
    layout: {
        title: 'Market Forge 运营后台',
        locale: false,
    },
    proxy: {
        '/api': {
            target: 'http://127.0.0.1:23001',
            changeOrigin: true,
            pathRewrite: {'^/api': '/api/v1'},
        },
    },
    routes: [
        {
            path: '/login',
            component: './Login',
            layout: false,
        },
        {
            path: '/',
            redirect: '/activity/order',
        },
        {
            name: '活动运营',
            path: '/activity',
            icon: 'GiftOutlined',
            routes: [
        {
            name: '抽奖订单',
            path: '/activity/order',
            component: './Activity/Order',
        },
            ],
        },
    ],
    npmClient: 'pnpm',
    utoopack: {},
});

