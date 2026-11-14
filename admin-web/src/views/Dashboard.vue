<template>
  <div class="page">
    <header class="page-head">
      <div>
        <h1>工作台</h1>
        <p class="sub">{{ greeting }}，这里是「{{ tenantCode }}」租户的运营后台。</p>
      </div>
      <div class="actions">
        <el-button @click="load">
          <Icon name="refresh" :size="15" style="margin-right: 5px" />刷新
        </el-button>
      </div>
    </header>

    <section class="kpis">
      <article v-for="k in kpis" :key="k.label" class="kpi">
        <div class="kpi-top">
          <span class="kpi-icon"><Icon :name="k.icon" :size="16" /></span>
          <span class="kpi-label">{{ k.label }}</span>
        </div>
        <strong class="kpi-value">{{ k.value }}</strong>
        <span class="kpi-foot">{{ k.foot }}</span>
      </article>
    </section>

    <div class="cols">
      <section class="card">
        <h2 class="card-title">常用入口 <small>系统配置域已上线</small></h2>
        <div class="tiles">
          <router-link v-for="t in tiles" :key="t.path" :to="t.path" class="tile">
            <span class="tile-icon"><Icon :name="t.icon" :size="17" /></span>
            <div>
              <strong>{{ t.title }}</strong>
              <span>{{ t.desc }}</span>
            </div>
            <Icon name="arrow" :size="15" class="tile-arrow" />
          </router-link>
        </div>
      </section>

      <section class="card">
        <h2 class="card-title">待办提醒</h2>
        <div class="empty todo-empty">
          <strong>业务模块上线后在此汇总</strong>
          退订审核、护理记录超时补录、资质到期、公众号配额告警将在这里提醒。
        </div>
        <div class="scope">
          <span class="scope-label">当前租户</span>
          <div class="scope-row"><span>租户标识</span><b class="mono">{{ tenantCode }}</b></div>
          <div class="scope-row"><span>登录账号</span><b>{{ userName }}</b></div>
          <div class="scope-row"><span>角色</span><b>{{ roleName }}</b></div>
          <div class="scope-row"><span>数据边界</span><b>仅本租户，跨租户 403</b></div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import Icon from '../components/Icon.vue'
import { listConfigs, listDictCategories, listLanguages, listTemplates } from '../api/modules'
import { TENANT_KEY, USER_KEY } from '../api/client'

const tenantCode = ref(localStorage.getItem(TENANT_KEY) || '-')
const configCount = ref(0)
const dictCount = ref(0)
const langCount = ref(0)
const templateCount = ref(0)

const user = computed(() => {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || '{}')
  } catch {
    return {}
  }
})
const userName = computed(() => user.value.realName || user.value.username || '管理员')
const roleName = computed(() => user.value.roleName || '租户管理员')
const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 11) return '早上好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const load = async () => {
  const [configs, dicts, langs, templates]: any[] = await Promise.all([
    listConfigs(),
    listDictCategories(),
    listLanguages(),
    listTemplates()
  ])
  configCount.value = (configs.data || []).reduce((s: number, g: any) => s + (g.items?.length || 0), 0)
  dictCount.value = (dicts.data || []).length
  langCount.value = (langs.data || []).length
  templateCount.value = (templates.data || []).length
}

const kpis = computed(() => [
  { label: '系统参数', value: configCount.value, foot: '分组维护，保存即时生效', icon: 'settings' },
  { label: '字典分类', value: dictCount.value, foot: '页面统一取中文名称', icon: 'book' },
  { label: '语言', value: langCount.value, foot: '缺词回落默认语言', icon: 'globe' },
  { label: '消息模板', value: templateCount.value, foot: '短信 / 站内信节点映射', icon: 'bell' }
])

const tiles = [
  { path: '/system/config', title: '系统参数', desc: '站点、门店、订单与护理规则', icon: 'settings' },
  { path: '/system/dict', title: '数据字典', desc: '房态、订单状态等中文名称', icon: 'book' },
  { path: '/system/nav', title: '菜单可视化', desc: '公众号 / 小程序 / H5 菜单', icon: 'grid' },
  { path: '/system/storage', title: '对象存储', desc: '本地 / S3 / MinIO / OSS / COS', icon: 'cloud' },
  { path: '/system/message', title: '消息配置', desc: '短信渠道与消息模板', icon: 'bell' },
  { path: '/system/sso', title: 'SSO 配置', desc: 'CAS / OAuth2 / OIDC', icon: 'key' }
]

onMounted(load)
</script>

<style scoped>
.kpis { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 14px; }
.kpi {
  background: var(--surface); border: 1px solid var(--line);
  border-radius: var(--radius-lg); padding: 16px 18px;
  display: flex; flex-direction: column; gap: 8px;
}
.kpi-top { display: flex; align-items: center; gap: 8px; }
.kpi-icon {
  width: 26px; height: 26px; border-radius: 8px; display: grid; place-items: center;
  background: var(--brand-50); color: var(--brand-600);
}
.kpi-label { font-size: 12.5px; color: var(--ink-500); }
.kpi-value { font-size: 28px; font-weight: 650; letter-spacing: -0.02em; line-height: 1.1; }
.kpi-foot { font-size: 11.5px; color: var(--ink-400); }

.cols { display: grid; grid-template-columns: minmax(0, 1.6fr) minmax(0, 1fr); gap: 14px; }
@media (max-width: 1000px) { .cols { grid-template-columns: 1fr; } }

.tiles { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
@media (max-width: 1280px) { .tiles { grid-template-columns: 1fr; } }

.tile {
  display: flex; align-items: center; gap: 11px;
  padding: 13px 14px;
  border: 1px solid var(--line); border-radius: var(--radius-md);
  text-decoration: none; color: inherit;
  transition: border-color 0.15s ease, background 0.15s ease;
}
.tile:hover { border-color: var(--brand-100); background: var(--brand-50); }
.tile-icon {
  width: 30px; height: 30px; border-radius: 9px; flex: 0 0 30px;
  display: grid; place-items: center;
  background: var(--surface-sunken); color: var(--ink-500);
}
.tile:hover .tile-icon { background: #fff; color: var(--brand-600); }
.tile strong { display: block; font-size: 13px; font-weight: 550; }
.tile span { font-size: 11.5px; color: var(--ink-400); }
.tile-arrow { margin-left: auto; color: var(--ink-300); }
.tile:hover .tile-arrow { color: var(--brand-600); }

.todo-empty { padding: 34px 20px; }
.todo-empty strong { color: var(--ink-700); }

.scope { border-top: 1px solid var(--line); padding-top: 14px; display: flex; flex-direction: column; gap: 8px; }
.scope-label { font-size: 11px; letter-spacing: 0.08em; color: var(--ink-400); text-transform: uppercase; }
.scope-row { display: flex; justify-content: space-between; font-size: 12.5px; }
.scope-row span { color: var(--ink-400); }
.scope-row b { font-weight: 550; color: var(--ink-900); }
</style>
