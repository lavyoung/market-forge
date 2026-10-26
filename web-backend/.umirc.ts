import {defineConfig} from '@umijs/max';

export default defineConfig({
    title: 'Market Forge',
    antd: {},
    model: {},
    initialState: {},
    request: {},
    history: {
        type: 'hash',
    },
    layout: {
        title: 'Market Forge',
        locale: false,
        navTheme: 'light',
        fixedHeader: true,
        fixSiderbar: true,
        siderWidth: 224,
        colorPrimary: '#1677ff',
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
                    name: '活动定义',
                    path: '/activity/definition',
                    component: './Activity/Definition',
                },
                {
                    name: '次数配置',
                    path: '/activity/count',
                    component: './Activity/Count',
                },
                {
                    name: '抽奖订单',
                    path: '/activity/order',
                    component: './Activity/Order',
                },
                {
                    name: '活动上架',
                    path: '/activity/stage',
                    component: './Activity/Stage',
                },
            ],
        },
        {
            name: '发奖运营',
            path: '/award',
            icon: 'TrophyOutlined',
            routes: [
                {
                    name: '奖品定义',
                    path: '/award/definition',
                    component: './Award/Definition',
                },
                {
                    name: '中奖记录',
                    path: '/award/record',
                    component: './Award/Record',
                },
            ],
        },
        {
            name: '策略运营',
            path: '/strategy',
            icon: 'ControlOutlined',
            routes: [
                {
                    name: '策略定义',
                    path: '/strategy/definition',
                    component: './Strategy/Definition',
                },
            ],
        },
    ],
    npmClient: 'pnpm',
    utoopack: {},
});
