<template>
  <div class="shell">
    <aside class="sidebar">
      <div class="brand">
        <div class="logo">月</div>
        <div class="brand-meta">
          <strong>月之孕 · 平台</strong>
          <span>多租户运营控制台</span>
        </div>
      </div>

      <div class="scope-chip">
        <Icon name="shield" :size="14" />
        <span>平台运营端</span>
        <i>仅汇总数据</i>
      </div>

      <nav class="nav">
        <p class="nav-label">运营</p>
        <router-link
          v-for="item in menus"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: route.path === item.path }"
        >
          <Icon :name="item.icon" />
          <span>{{ item.title }}</span>
        </router-link>
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

      <router-link to="/login?app=tenant" class="switch">切换到租户后台 →</router-link>
    </aside>

    <section class="main">
      <header class="topbar">
        <div class="crumb">
          <span class="crumb-root">平台运营端</span>
          <span class="sep">/</span>
          <span class="crumb-current">{{ route.meta.title || '概览' }}</span>
        </div>
        <div class="topbar-right">
          <span class="readonly">
            <Icon name="shield" :size="13" />
            平台账号不可查看租户业务明细
          </span>
          <span class="divider" />
          <div class="user">
            <span class="avatar sm">{{ initial }}</span>
            <span>{{ userName }}</span>
          </div>
        </div>
      </header>

      <main class="content">
        <router-view />
      </main>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Icon from '../components/Icon.vue'
import { PLATFORM_TOKEN_KEY, PLATFORM_USER_KEY } from '../api/client'

const route = useRoute()
const router = useRouter()

const menus = [
  { path: '/platform/dashboard', title: '运营概览', icon: 'dashboard' },
  { path: '/platform/tenants', title: '租户管理', icon: 'building' },
  { path: '/platform/packages', title: '套餐与配额', icon: 'package' },
  { path: '/platform/bills', title: '账单与计费', icon: 'receipt' },
  { path: '/platform/audits', title: '平台审计', icon: 'shield' }
]

const user = computed(() => {
  try {
    return JSON.parse(localStorage.getItem(PLATFORM_USER_KEY) || '{}')
  } catch {
    return {}
  }
})
const userName = computed(() => user.value.realName || user.value.username || '平台账号')
const roleName = computed(() => user.value.roleName || '平台运营')
const initial = computed(() => String(userName.value).slice(0, 1))

const logout = () => {
  localStorage.removeItem(PLATFORM_TOKEN_KEY)
  localStorage.removeItem(PLATFORM_USER_KEY)
  router.push('/login?app=platform')
}
</script>

<style scoped>
.shell { display: flex; height: 100%; background: var(--bg); }

.sidebar {
  width: var(--sidebar-width);
  flex: 0 0 var(--sidebar-width);
  background: #101a1f;
  color: var(--sidebar-text);
  display: flex;
  flex-direction: column;
  padding: 20px 14px 14px;
}
.brand { display: flex; align-items: center; gap: 10px; padding: 4px 8px 16px; }
.logo {
  width: 34px; height: 34px; border-radius: 10px;
  display: grid; place-items: center;
  background: linear-gradient(140deg, #3c5f6b, #16242b);
  color: #e9f2f5; font-size: 16px; font-weight: 600;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.16);
}
.brand-meta { display: flex; flex-direction: column; line-height: 1.25; }
.brand-meta strong { color: #f1f6f8; font-size: 15px; }
.brand-meta span { font-size: 11px; color: #75878f; letter-spacing: 0.04em; }

.scope-chip {
  display: flex; align-items: center; gap: 6px;
  margin: 0 6px 18px; padding: 6px 9px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  font-size: 11.5px; color: #9fb3bb;
  background: rgba(255, 255, 255, 0.03);
}
.scope-chip i { margin-left: auto; font-style: normal; font-size: 10px; color: #64777f; }

.nav { flex: 1; display: flex; flex-direction: column; gap: 2px; }
.nav-label {
  margin: 0 0 6px 10px; font-size: 11px;
  letter-spacing: 0.1em; color: #64757d; text-transform: uppercase;
}
.nav-item {
  position: relative;
  display: flex; align-items: center; gap: 10px;
  height: 38px; padding: 0 10px;
  border-radius: 9px; color: var(--sidebar-text);
  font-size: 13.5px; text-decoration: none;
  transition: background 0.15s ease, color 0.15s ease;
}
.nav-item :deep(svg) { opacity: 0.7; }
.nav-item:hover { background: rgba(255, 255, 255, 0.05); color: #eef4f6; }
.nav-item.active { background: rgba(107, 156, 174, 0.16); color: #ffffff; font-weight: 500; }
.nav-item.active :deep(svg) { opacity: 1; color: #9fc9d8; }
.nav-item.active::before {
  content: ''; position: absolute; left: -14px; top: 9px; bottom: 9px;
  width: 3px; border-radius: 0 3px 3px 0; background: #7fb2c4;
}

.sidebar-foot {
  margin-top: 12px; padding: 10px;
  border-top: 1px solid rgba(255, 255, 255, 0.07);
  display: flex; align-items: center; gap: 10px;
}
.avatar {
  width: 32px; height: 32px; border-radius: 50%;
  display: grid; place-items: center;
  background: #1e2f37; color: #cfe2e9; font-size: 13px; font-weight: 600;
}
.avatar.sm { width: 26px; height: 26px; font-size: 12px; background: #eef4f6; color: #2c4a56; }
.who { display: flex; flex-direction: column; line-height: 1.3; }
.who strong { color: #e7eff2; font-size: 12.5px; font-weight: 500; }
.who span { color: #6d7f87; font-size: 11px; }
.icon-btn {
  margin-left: auto; width: 28px; height: 28px; border: 0; border-radius: 7px;
  background: transparent; color: #7d8f97; cursor: pointer; display: grid; place-items: center;
}
.icon-btn:hover { background: rgba(255, 255, 255, 0.07); color: #fff; }

.switch {
  margin: 6px 8px 0;
  font-size: 11.5px; color: #6d7f87; text-decoration: none;
}
.switch:hover { color: #b9cdd5; }

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
.readonly {
  display: flex; align-items: center; gap: 6px;
  font-size: 12px; color: var(--ink-500);
  background: var(--surface-sunken);
  border: 1px solid var(--line);
  border-radius: 999px; padding: 4px 11px;
}
.divider { width: 1px; height: 20px; background: var(--line); }
.user { display: flex; align-items: center; gap: 8px; font-size: 13px; color: var(--ink-700); }
.content { flex: 1; overflow: auto; }
</style>
