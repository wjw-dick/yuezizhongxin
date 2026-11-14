import { createRouter, createWebHashHistory } from 'vue-router'
import Home from './views/Home.vue'

/**
 * H5 走 hash 路由：分享链接在微信内打开时无需服务端 rewrite，
 * 支付回跳也能带上 #/pay/result 的参数，减少 404 与回跳丢失。
 */
export const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', name: 'home', component: Home },
    { path: '/packages', name: 'packages', component: () => import('./views/Packages.vue') },
    { path: '/activity', name: 'activity', component: () => import('./views/Activity.vue') },
    { path: '/cases', name: 'cases', component: () => import('./views/Cases.vue') },
    { path: '/book', name: 'book', component: () => import('./views/Book.vue') },
    { path: '/orders', name: 'orders', component: () => import('./views/Orders.vue') },
    { path: '/pay/result', name: 'payResult', component: () => import('./views/PayResult.vue') },
    { path: '/mine', name: 'mine', component: () => import('./views/Mine.vue') },
    { path: '/:pathMatch(.*)*', redirect: '/' }
  ],
  scrollBehavior: () => ({ top: 0 })
})

router.afterEach((to) => {
  const titles: Record<string, string> = {
    home: '月之孕月子中心 · 预约参观',
    packages: '月子套餐与房型 · 月之孕',
    activity: '到店活动 · 月之孕',
    cases: '客户案例 · 月之孕',
    book: '预约参观 · 月之孕',
    orders: '订单查询 · 月之孕',
    payResult: '支付结果 · 月之孕',
    mine: '我的 · 月之孕'
  }
  document.title = titles[String(to.name)] || '月之孕月子中心'
})
