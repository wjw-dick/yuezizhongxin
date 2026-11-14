import { createRouter, createWebHashHistory } from 'vue-router'
import { PLATFORM_TOKEN_KEY, TOKEN_KEY } from '../api/client'

const routes = [
  { path: '/login', name: 'login', component: () => import('../views/Login.vue'), meta: { public: true } },
  {
    path: '/platform',
    component: () => import('../layout/PlatformLayout.vue'),
    redirect: '/platform/dashboard',
    meta: { app: 'platform' },
    children: [
      { path: 'dashboard', name: 'platform-dashboard', component: () => import('../views/platform/Dashboard.vue'), meta: { title: '运营概览', app: 'platform' } },
      { path: 'tenants', name: 'platform-tenants', component: () => import('../views/platform/Tenants.vue'), meta: { title: '租户管理', app: 'platform' } },
      { path: 'packages', name: 'platform-packages', component: () => import('../views/platform/Packages.vue'), meta: { title: '套餐与配额', app: 'platform' } },
      { path: 'bills', name: 'platform-bills', component: () => import('../views/platform/Bills.vue'), meta: { title: '账单与计费', app: 'platform' } },
      { path: 'audits', name: 'platform-audits', component: () => import('../views/platform/Audits.vue'), meta: { title: '平台审计', app: 'platform' } }
    ]
  },
  {
    path: '/',
    component: () => import('../layout/AppLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'dashboard', component: () => import('../views/Workbench.vue'), meta: { title: '工作台' } },

      // 房务
      { path: 'rooms', name: 'rooms', component: () => import('../views/room/RoomPage.vue'), meta: { title: '房型与房间' } },
      { path: 'calendar', name: 'calendar', component: () => import('../views/room/CalendarPage.vue'), meta: { title: '档期日历' } },
      { path: 'products', name: 'products', component: () => import('../views/product/ProductPage.vue'), meta: { title: '套餐与商品' } },

      // 客户与订单
      { path: 'crm', name: 'crm', component: () => import('../views/crm/CrmPage.vue'), meta: { title: '线索与跟进' } },
      { path: 'orders', name: 'orders', component: () => import('../views/order/OrderPage.vue'), meta: { title: '订单与收款' } },
      { path: 'stay', name: 'stay', component: () => import('../views/stay/StayPage.vue'), meta: { title: '入馆与出馆' } },

      // 护理
      { path: 'care', name: 'care', component: () => import('../views/care/CarePage.vue'), meta: { title: '护理管理' } },
      { path: 'rehab', name: 'rehab', component: () => import('../views/rehab/RehabPage.vue'), meta: { title: '产后康复' } },
      { path: 'meals', name: 'meals', component: () => import('../views/meal/MealPage.vue'), meta: { title: '月子餐' } },
      { path: 'staff', name: 'staff', component: () => import('../views/staff/StaffPage.vue'), meta: { title: '人力与排班' } },

      // 会员与内容
      { path: 'members', name: 'members', component: () => import('../views/member/MemberPage.vue'), meta: { title: '会员与营销' } },
      { path: 'content', name: 'content', component: () => import('../views/content/CmsPage.vue'), meta: { title: '内容管理' } },

      // 数据与渠道
      { path: 'reports', name: 'reports', component: () => import('../views/report/ReportPage.vue'), meta: { title: '经营报表' } },
      { path: 'mp', name: 'mp', component: () => import('../views/mp/MpPage.vue'), meta: { title: '公众号运营' } },

      // 系统
      { path: 'system/roles', name: 'roles', component: () => import('../views/system/RolePage.vue'), meta: { title: '角色与权限' } },
      { path: 'system/entries', name: 'entries', component: () => import('../views/system/EntryPage.vue'), meta: { title: '端入口管理' } },
      { path: 'system/audit', name: 'audit', component: () => import('../views/system/AuditPage.vue'), meta: { title: '审计日志' } },
      { path: 'system/config', name: 'config', component: () => import('../views/system/ConfigPage.vue'), meta: { title: '系统参数' } },
      { path: 'system/dict', name: 'dict', component: () => import('../views/system/DictPage.vue'), meta: { title: '数据字典' } },
      { path: 'system/i18n', name: 'i18n', component: () => import('../views/system/I18nPage.vue'), meta: { title: '多语言' } },
      { path: 'system/nav', name: 'nav', component: () => import('../views/system/NavPage.vue'), meta: { title: '菜单可视化' } },
      { path: 'system/storage', name: 'storage', component: () => import('../views/system/StoragePage.vue'), meta: { title: '对象存储' } },
      { path: 'system/message', name: 'message', component: () => import('../views/system/MessagePage.vue'), meta: { title: '消息配置' } },
      { path: 'system/sso', name: 'sso', component: () => import('../views/system/SsoPage.vue'), meta: { title: 'SSO 配置' } }
    ]
  }
]

const router = createRouter({ history: createWebHashHistory(), routes })

router.beforeEach((to) => {
  if (to.meta.public) {
    return true
  }
  if (to.meta.app === 'platform') {
    if (!localStorage.getItem(PLATFORM_TOKEN_KEY)) {
      return { path: '/login', query: { app: 'platform' } }
    }
    return true
  }
  if (!localStorage.getItem(TOKEN_KEY)) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
