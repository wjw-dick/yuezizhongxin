<template>
  <div class="shell">
    <aside class="sidebar">
      <div class="brand">
        <div class="logo">月</div>
        <div class="brand-meta">
          <strong>月之孕</strong>
          <span>月子中心运营后台</span>
        </div>
      </div>

      <nav class="nav">
        <div v-for="group in menuGroups" :key="group.label" class="nav-group">
          <p class="nav-label">{{ group.label }}</p>
          <router-link
            v-for="item in group.items"
            :key="item.path"
            :to="item.path"
            class="nav-item"
            :class="{ active: isActive(item.path) }"
          >
            <Icon :name="item.icon" />
            <span>{{ item.title }}</span>
            <i v-if="item.badge && item.badge() > 0" class="nav-badge">{{ item.badge() }}</i>
          </router-link>
        </div>
      </nav>

      <div class="sidebar-foot">
        <div class="avatar">{{ initial }}</div>
        <div class="who">
          <strong>{{ userName }}</strong>
          <span>{{ roleName }}</span>
        </div>
        <button class="icon-btn" title="退出登录" @click="logout">
          <Icon name="logout" :size="17" />
        </button>
      </div>
    </aside>

    <section class="main">
      <header class="topbar">
        <div class="crumb">
          <span class="crumb-root">租户后台</span>
          <span class="sep">/</span>
          <span class="crumb-current">{{ route.meta.title || '工作台' }}</span>
        </div>
        <div class="topbar-right">
          <div class="tenant-chip">
            <span class="dot" />
            <span class="code">{{ tenantCode }}</span>
            <span class="label">租户</span>
          </div>
          <span class="divider" />
          <div class="user">
            <span class="avatar sm">{{ initial }}</span>
            <span>{{ userName }}</span>
          </div>
        </div>
      </header>

      <div v-if="tabs.length > 1" class="tabs">
        <button
          v-for="tab in tabs"
          :key="tab.path"
          class="tab"
          :class="{ active: route.path === tab.path }"
          @click="router.push(tab.path)"
        >
          {{ tab.title }}
          <Icon v-if="tab.path !== '/dashboard'" name="close" :size="12" class="tab-close" />
        </button>
      </div>

      <main class="content">
        <router-view v-slot="{ Component }">
          <keep-alive>
            <component :is="Component" />
          </keep-alive>
        </router-view>
      </main>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Icon from '../components/Icon.vue'
import { TENANT_KEY, TOKEN_KEY, USER_KEY, PLATFORM_TOKEN_KEY } from '../api/http'

const route = useRoute()
const router = useRouter()

const menuGroups = [
  {
    label: '概览',
    items: [{ path: '/dashboard', title: '工作台', icon: 'dashboard' }]
  },
  {
    label: '房务与商品',
    items: [
      { path: '/rooms', title: '房型与房间', icon: 'building' },
      { path: '/calendar', title: '档期日历', icon: 'calendar' },
      { path: '/products', title: '套餐与商品', icon: 'package' }
    ]
  },
  {
    label: '客户与订单',
    items: [
      { path: '/crm', title: '线索与跟进', icon: 'users' },
      { path: '/orders', title: '订单与收款', icon: 'receipt' },
      { path: '/stay', title: '入馆与出馆', icon: 'key' }
    ]
  },
  {
    label: '护理与服务',
    items: [
      { path: '/care', title: '护理管理', icon: 'shield' },
      { path: '/rehab', title: '产后康复', icon: 'sparkle' },
      { path: '/meals', title: '月子餐', icon: 'meal' },
      { path: '/staff', title: '人力与排班', icon: 'clock' }
    ]
  },
  {
    label: '会员与内容',
    items: [
      { path: '/members', title: '会员与营销', icon: 'users' },
      { path: '/content', title: '内容管理', icon: 'book' }
    ]
  },
  {
    label: '数据与渠道',
    items: [
      { path: '/reports', title: '经营报表', icon: 'chart' },
      { path: '/mp', title: '公众号运营', icon: 'wechat' }
    ]
  },
  {
    label: '系统设置',
    items: [
      { path: '/system/roles', title: '角色与权限', icon: 'key' },
      { path: '/system/entries', title: '端入口管理', icon: 'layers' },
      { path: '/system/audit', title: '审计日志', icon: 'inbox' },
      { path: '/system/config', title: '系统参数', icon: 'settings' },
      { path: '/system/dict', title: '数据字典', icon: 'book' },
      { path: '/system/i18n', title: '多语言', icon: 'globe' },
      { path: '/system/nav', title: '菜单可视化', icon: 'grid' },
      { path: '/system/storage', title: '对象存储', icon: 'cloud' },
      { path: '/system/message', title: '消息配置', icon: 'bell' },
      { path: '/system/sso', title: 'SSO 配置', icon: 'key' }
    ]
  }
]

const isActive = (path: string) => route.path === path

const tenantCode = computed(() => localStorage.getItem(TENANT_KEY) || '-')
const user = computed(() => {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || '{}')
  } catch {
    return {}
  }
})
const userName = computed(() => user.value.realName || user.value.username || '管理员')
const roleName = computed(() => user.value.roleName || '租户管理员')
const initial = computed(() => String(userName.value).slice(0, 1))

const tabs = reactive<{ path: string; title: string }[]>([])
watch(
  () => route.path,
  () => {
    if (route.path === '/login' || !route.meta.title) return
    if (!tabs.find((t) => t.path === route.path)) {
      tabs.push({ path: route.path, title: String(route.meta.title) })
    }
  },
  { immediate: true }
)

const logout = () => {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  localStorage.removeItem(PLATFORM_TOKEN_KEY)
  router.push('/login')
}
</script>

<style scoped>
.shell { display: flex; height: 100%; background: var(--bg); }

/* ---------------- 侧栏 ---------------- */
.sidebar {
  width: var(--sidebar-width);
  flex: 0 0 var(--sidebar-width);
  background: var(--sidebar-bg);
  color: var(--sidebar-text);
  display: flex;
  flex-direction: column;
  padding: 20px 14px 14px;
}

.brand { display: flex; align-items: center; gap: 10px; padding: 4px 8px 20px; }
.logo {
  width: 34px; height: 34px; border-radius: 10px;
  display: grid; place-items: center;
  background: linear-gradient(140deg, #2f8b70, #1a5a48);
  color: #f4faf7; font-size: 16px; font-weight: 600;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.18);
}
.brand-meta { display: flex; flex-direction: column; line-height: 1.25; }
.brand-meta strong { color: #f3f7f4; font-size: 15px; letter-spacing: 0.02em; }
.brand-meta span { font-size: 11px; color: #7d8f87; letter-spacing: 0.04em; }

.nav { flex: 1; overflow-y: auto; display: flex; flex-direction: column; gap: 16px; padding-right: 2px; }
.nav-group { display: flex; flex-direction: column; gap: 2px; }
.nav-badge {
  margin-left: auto; font-style: normal; font-size: 10px; line-height: 16px;
  min-width: 16px; text-align: center; border-radius: 8px;
  background: rgba(176, 107, 98, 0.24); color: #e8b3ad; padding: 0 4px;
}
.nav-label {
  margin: 0 0 6px 10px;
  font-size: 11px;
  letter-spacing: 0.1em;
  color: #64756d;
  text-transform: uppercase;
  display: flex; align-items: center; gap: 6px;
}
.nav-label em {
  font-style: normal; font-size: 10px; letter-spacing: 0;
  padding: 1px 5px; border-radius: 4px;
  background: rgba(255, 255, 255, 0.05); color: #6d7f76;
}

.nav-item {
  position: relative;
  display: flex; align-items: center; gap: 10px;
  height: 38px; padding: 0 10px;
  border-radius: 9px;
  color: var(--sidebar-text);
  font-size: 13.5px;
  text-decoration: none;
  transition: background 0.15s ease, color 0.15s ease;
}
.nav-item :deep(svg) { opacity: 0.72; }
.nav-item:hover { background: rgba(255, 255, 255, 0.05); color: #eef3f0; }
.nav-item.active {
  background: rgba(47, 139, 112, 0.18);
  color: #ffffff;
  font-weight: 500;
}
.nav-item.active :deep(svg) { opacity: 1; color: #6cc3a5; }
.nav-item.active::before {
  content: '';
  position: absolute; left: -14px; top: 9px; bottom: 9px;
  width: 3px; border-radius: 0 3px 3px 0;
  background: #4fae8d;
}
.nav-item.disabled { color: #5f716a; cursor: not-allowed; }
.nav-item.disabled:hover { background: transparent; }
.pending {
  margin-left: auto; font-style: normal; font-size: 10px;
  color: #5f716a; border: 1px solid #2a3a33; border-radius: 4px;
  padding: 0 4px; line-height: 15px;
}

.sidebar-foot {
  margin-top: 12px; padding: 10px;
  border-top: 1px solid rgba(255, 255, 255, 0.07);
  display: flex; align-items: center; gap: 10px;
}
.avatar {
  width: 32px; height: 32px; border-radius: 50%;
  display: grid; place-items: center;
  background: #23372f; color: #cfe6dc; font-size: 13px; font-weight: 600;
}
.avatar.sm { width: 26px; height: 26px; font-size: 12px; background: var(--brand-50); color: var(--brand-700); }
.who { display: flex; flex-direction: column; line-height: 1.3; min-width: 0; }
.who strong { color: #e7efeb; font-size: 12.5px; font-weight: 500; }
.who span { color: #6d7f76; font-size: 11px; }
.icon-btn {
  margin-left: auto; width: 28px; height: 28px; border: 0; border-radius: 7px;
  background: transparent; color: #7d8f87; cursor: pointer; display: grid; place-items: center;
}
.icon-btn:hover { background: rgba(255, 255, 255, 0.07); color: #fff; }

/* ---------------- 主区 ---------------- */
.main { flex: 1; min-width: 0; display: flex; flex-direction: column; }

.topbar {
  height: var(--header-height);
  flex: 0 0 var(--header-height);
  background: var(--surface);
  border-bottom: 1px solid var(--line);
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 24px;
}
.crumb { display: flex; align-items: center; gap: 8px; font-size: 13px; }
.crumb-root { color: var(--ink-400); }
.sep { color: var(--line-strong); }
.crumb-current { color: var(--ink-900); font-weight: 550; }

.topbar-right { display: flex; align-items: center; gap: 14px; }
.tenant-chip {
  display: flex; align-items: center; gap: 7px;
  height: 30px; padding: 0 12px;
  border: 1px solid var(--line); border-radius: 999px;
  background: var(--surface-sunken);
  font-size: 12.5px;
}
.tenant-chip .dot { width: 6px; height: 6px; border-radius: 50%; background: #3aa173; }
.tenant-chip .code { color: var(--ink-900); font-weight: 600; font-family: ui-monospace, Menlo, monospace; }
.tenant-chip .label { color: var(--ink-400); }
.divider { width: 1px; height: 20px; background: var(--line); }
.user { display: flex; align-items: center; gap: 8px; font-size: 13px; color: var(--ink-700); }

.tabs {
  display: flex; align-items: center; gap: 2px;
  padding: 0 24px;
  background: var(--surface);
  border-bottom: 1px solid var(--line);
  overflow-x: auto;
}
.tab {
  position: relative;
  border: 0; background: transparent; cursor: pointer;
  padding: 9px 12px 10px;
  font-size: 13px; color: var(--ink-500);
  display: flex; align-items: center; gap: 6px;
  white-space: nowrap;
}
.tab:hover { color: var(--ink-900); }
.tab.active { color: var(--brand-700); font-weight: 550; }
.tab.active::after {
  content: ''; position: absolute; left: 12px; right: 12px; bottom: -1px;
  height: 2px; border-radius: 2px 2px 0 0; background: var(--brand-600);
}
.tab-close { opacity: 0.4; }
.tab-close:hover { opacity: 1; }

.content { flex: 1; overflow: auto; }
</style>
