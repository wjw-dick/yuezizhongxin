<template>
  <div class="site">
    <header class="topbar" :class="{ solid: scrolled || route.path !== '/' }">
      <div class="wrap bar">
        <router-link to="/" class="brand">
          <span class="mark">月</span>
          <span class="brand-text">
            <strong>{{ site.name || '月之孕' }}</strong>
            <em>{{ site.slogan || '母婴护理中心' }}</em>
          </span>
        </router-link>

        <nav class="menu">
          <router-link v-for="item in menus" :key="item.path" :to="item.path" class="menu-item" :class="{ active: route.path === item.path }">
            {{ item.label }}
          </router-link>
        </nav>

        <div class="actions">
          <div class="lang">
            <button v-for="l in LANGS" :key="l.code" :class="{ on: lang === l.code }" @click="switchLang(l.code)">
              {{ l.short }}
            </button>
          </div>
          <a v-if="site.phone" class="phone" :href="`tel:${site.phone}`">{{ site.phone }}</a>
          <router-link to="/visit" class="btn btn-primary">{{ t('book') }}</router-link>
          <button class="burger" @click="drawer = !drawer" aria-label="menu">
            <span /><span /><span />
          </button>
        </div>
      </div>

      <div v-if="drawer" class="drawer">
        <router-link v-for="item in menus" :key="item.path" :to="item.path" @click="drawer = false">{{ item.label }}</router-link>
        <router-link to="/visit" class="btn btn-primary btn-block" @click="drawer = false">{{ t('book') }}</router-link>
      </div>
    </header>

    <main>
      <router-view />
    </main>

    <footer class="footer">
      <div class="wrap foot-grid">
        <div class="foot-brand">
          <div class="brand">
            <span class="mark">月</span>
            <strong class="serif">{{ site.name || '月之孕月子中心' }}</strong>
          </div>
          <p class="muted">{{ site.address || '以专业护理与循证康复，陪伴新生儿家庭度过第一个月。' }}</p>
          <div class="foot-actions">
            <router-link to="/visit" class="btn btn-ghost">预约参观</router-link>
            <button class="btn btn-ghost" @click="showQr">小程序 / App</button>
          </div>
        </div>

        <div class="foot-col">
          <h4>了解月之孕</h4>
          <router-link to="/rooms">房型与套餐</router-link>
          <router-link to="/rehab">产后康复</router-link>
          <router-link to="/meals">月子餐</router-link>
          <router-link to="/team">护理团队</router-link>
        </div>

        <div class="foot-col">
          <h4>服务与支持</h4>
          <router-link to="/cases">案例口碑</router-link>
          <router-link to="/stores">门店分布</router-link>
          <router-link to="/news">资讯</router-link>
          <router-link to="/visit">预约参观</router-link>
        </div>

        <div class="foot-col">
          <h4>联系我们</h4>
          <span>{{ t('consult') }}：{{ site.phone || '400-000-0000' }}</span>
          <span>{{ site.wechat ? `微信：${site.wechat}` : '微信：公众号搜索「月之孕」' }}</span>
          <span>24 小时护理值班</span>
        </div>
      </div>

      <div class="wrap foot-bottom">
        <span>© {{ year }} {{ site.name || '月之孕月子中心' }} · {{ site.icp || 'ICP 备案号待配置' }}</span>
        <span class="muted">{{ t('footer_note') }}</span>
      </div>
    </footer>

    <div v-if="qrOpen" class="qr-mask" @click.self="qrOpen = false">
      <div class="qr-card">
        <h3>小程序与 App</h3>
        <div class="qr-list">
          <div v-for="e in entries" :key="e.id" class="qr-item">
            <img v-if="e.imageKey" :src="e.imageKey" :alt="e.name" @click="scan(e)" />
            <div v-else class="qr-ph">未上传</div>
            <strong>{{ e.name }}</strong>
            <span class="muted">{{ e.entryType }}</span>
          </div>
          <p v-if="!entries.length" class="muted">运营后台尚未配置入口二维码</p>
        </div>
        <button class="btn btn-ghost btn-block" @click="qrOpen = false">关闭</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { LANGS, lang, setLang, t } from './i18n'
import { siteApi } from './api'

const route = useRoute()
const menus = [
  { path: '/', label: '首页' },
  { path: '/rooms', label: '房型与套餐' },
  { path: '/rehab', label: '产后康复' },
  { path: '/meals', label: '月子餐' },
  { path: '/team', label: '护理团队' },
  { path: '/cases', label: '案例口碑' },
  { path: '/stores', label: '门店分布' },
  { path: '/news', label: '资讯' }
]

const site = ref<any>({})
const entries = ref<any[]>([])
const drawer = ref(false)
const qrOpen = ref(false)
const scrolled = ref(false)
const year = new Date().getFullYear()

function switchLang(code: string) {
  setLang(code)
  location.reload()
}

async function loadSite() {
  try {
    const data: any = await siteApi.home(lang.value)
    site.value = data?.site || {}
    document.title = `${site.value.name || '月之孕月子中心'} · ${route.meta.title || '母婴护理'}`
  } catch {
    site.value = { name: '月之孕月子中心', phone: '400-000-0000' }
  }
}

async function showQr() {
  qrOpen.value = true
  try {
    entries.value = ((await siteApi.entries()) as any) || []
  } catch {
    entries.value = []
  }
}

async function scan(entry: any) {
  try {
    await siteApi.scan(entry.id)
  } catch {
    /* 计数失败不影响用户 */
  }
}

onMounted(() => {
  loadSite()
  window.addEventListener('scroll', () => {
    scrolled.value = window.scrollY > 40
  })
})
</script>

<style scoped>
.topbar {
  position: fixed; inset: 0 0 auto 0; z-index: 40;
  background: transparent; transition: background 0.25s ease, box-shadow 0.25s ease, border-color 0.25s ease;
  border-bottom: 1px solid transparent;
}
.topbar.solid { background: rgba(251, 249, 245, 0.92); backdrop-filter: blur(10px); border-bottom-color: var(--line); box-shadow: var(--shadow-sm); }
.bar { display: flex; align-items: center; gap: 26px; height: 74px; }

.brand { display: flex; align-items: center; gap: 10px; }
.mark {
  width: 34px; height: 34px; border-radius: 10px; display: grid; place-items: center;
  background: linear-gradient(140deg, #2f8b70, #1a5a48); color: #f4faf7;
  font-family: var(--font-serif); font-size: 17px;
}
.brand-text { display: flex; flex-direction: column; line-height: 1.2; }
.brand-text strong { font-size: 15.5px; color: var(--ink-900); font-family: var(--font-serif); letter-spacing: 0.02em; }
.brand-text em { font-style: normal; font-size: 10.5px; color: var(--ink-400); letter-spacing: 0.16em; }

.menu { display: flex; align-items: center; gap: 4px; margin-left: auto; }
.menu-item {
  position: relative; padding: 8px 12px; font-size: 13.5px; color: var(--ink-700); border-radius: 999px;
  transition: color 0.15s ease, background 0.15s ease;
}
.menu-item:hover { color: var(--brand-700); background: rgba(47, 139, 112, 0.07); }
.menu-item.active { color: var(--brand-700); font-weight: 600; }
.menu-item.active::after {
  content: ''; position: absolute; left: 50%; bottom: 2px; width: 16px; height: 2px;
  transform: translateX(-50%); border-radius: 2px; background: var(--brand-600);
}

.actions { display: flex; align-items: center; gap: 12px; }
.lang { display: flex; border: 1px solid var(--line-strong); border-radius: 999px; overflow: hidden; }
.lang button {
  border: 0; background: transparent; color: var(--ink-500); font-size: 11.5px;
  padding: 4px 9px; cursor: pointer;
}
.lang button.on { background: var(--brand-700); color: #fff; }
.phone { font-size: 13px; color: var(--ink-900); font-variant-numeric: tabular-nums; }
.actions .btn { height: 38px; padding: 0 18px; font-size: 13px; }

.burger { display: none; flex-direction: column; gap: 4px; border: 0; background: transparent; cursor: pointer; padding: 6px; }
.burger span { width: 18px; height: 1.6px; background: var(--ink-900); border-radius: 2px; }

.drawer {
  display: none; flex-direction: column; gap: 4px; padding: 10px 18px 20px;
  background: var(--paper); border-bottom: 1px solid var(--line);
}
.drawer a { padding: 11px 6px; font-size: 14px; color: var(--ink-800); border-bottom: 1px dashed var(--line); }
.drawer .btn { margin-top: 12px; }

main { padding-top: 74px; }

.footer { background: #1b2a24; color: #c7d3cc; padding: 62px 0 26px; margin-top: 40px; }
.foot-grid { display: grid; grid-template-columns: 1.6fr 1fr 1fr 1.2fr; gap: 40px; }
.foot-brand .brand { margin-bottom: 12px; }
.foot-brand strong { color: #f1f6f2; font-size: 17px; }
.foot-brand p { font-size: 13px; line-height: 1.8; color: #93a49b; margin: 0 0 16px; }
.foot-actions { display: flex; gap: 10px; }
.foot-actions .btn { height: 38px; font-size: 12.5px; border-color: #33463d; color: #cfe0d7; }
.foot-actions .btn:hover { border-color: #4fae8d; color: #eaf5ef; }
.foot-col { display: flex; flex-direction: column; gap: 9px; font-size: 13px; }
.foot-col h4 { color: #eef4f0; font-size: 13.5px; margin-bottom: 4px; font-family: var(--font-sans); font-weight: 600; }
.foot-col a, .foot-col span { color: #9aaca2; }
.foot-col a:hover { color: #d8ece3; }
.foot-bottom {
  margin-top: 44px; padding-top: 18px; border-top: 1px solid #2a3a33;
  display: flex; justify-content: space-between; gap: 16px; font-size: 11.5px; color: #7c8d84; flex-wrap: wrap;
}

.qr-mask { position: fixed; inset: 0; z-index: 60; background: rgba(20, 26, 23, 0.5); display: grid; place-items: center; padding: 20px; }
.qr-card { background: var(--surface); border-radius: var(--radius-lg); padding: 26px; width: 100%; max-width: 460px; }
.qr-card h3 { font-size: 18px; margin-bottom: 16px; }
.qr-list { display: grid; grid-template-columns: repeat(auto-fit, minmax(120px, 1fr)); gap: 16px; margin-bottom: 18px; }
.qr-item { display: flex; flex-direction: column; align-items: center; gap: 4px; text-align: center; }
.qr-item img { width: 108px; height: 108px; object-fit: contain; border: 1px solid var(--line); border-radius: 10px; cursor: pointer; }
.qr-ph { width: 108px; height: 108px; display: grid; place-items: center; background: var(--paper-2); border-radius: 10px; font-size: 12px; color: var(--ink-400); }
.qr-item strong { font-size: 12.5px; }
.qr-item span { font-size: 11px; }

@media (max-width: 1080px) {
  .menu { display: none; }
  .burger { display: flex; }
  .drawer { display: flex; }
  .phone { display: none; }
  .foot-grid { grid-template-columns: 1fr 1fr; gap: 28px; }
}
@media (max-width: 640px) {
  .actions .btn { display: none; }
  .foot-grid { grid-template-columns: 1fr; }
  .bar { gap: 12px; }
}
</style>
