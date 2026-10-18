import {defineConfig} from '@umijs/max';

export default defineConfig({
    antd: {},
    model: {},
    initialState: {},
    request: {},
    layout: {
        title: 'Market Forge 运营后台',
        locale: false,
    },
    routes: [
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

