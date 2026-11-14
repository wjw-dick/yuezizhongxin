import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior: () => ({ top: 0 }),
  routes: [
    { path: '/', name: 'home', component: () => import('./views/Home.vue'), meta: { title: '月之孕月子中心 · 母婴护理与产后康复' } },
    { path: '/rooms', name: 'rooms', component: () => import('./views/Rooms.vue'), meta: { title: '房型与套餐' } },
    { path: '/rehab', name: 'rehab', component: () => import('./views/Rehab.vue'), meta: { title: '产后康复' } },
    { path: '/meals', name: 'meals', component: () => import('./views/Meals.vue'), meta: { title: '月子餐' } },
    { path: '/team', name: 'team', component: () => import('./views/Team.vue'), meta: { title: '护理团队' } },
    { path: '/cases', name: 'cases', component: () => import('./views/Cases.vue'), meta: { title: '案例口碑' } },
    { path: '/stores', name: 'stores', component: () => import('./views/Stores.vue'), meta: { title: '门店分布' } },
    { path: '/news', name: 'news', component: () => import('./views/News.vue'), meta: { title: '资讯' } },
    { path: '/news/:id', name: 'news-detail', component: () => import('./views/NewsDetail.vue'), meta: { title: '资讯详情' } },
    { path: '/visit', name: 'visit', component: () => import('./views/Visit.vue'), meta: { title: '预约参观' } },
    { path: '/:pathMatch(.*)*', redirect: '/' }
  ]
})

router.afterEach((to) => {
  const title = (to.meta.title as string) || '月之孕月子中心'
  document.title = title.includes('月之孕') ? title : `${title} · 月之孕月子中心`
})

export default router
