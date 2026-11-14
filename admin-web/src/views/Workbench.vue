<template>
  <div class="workbench">
    <header class="hero">
      <div class="hero-text">
        <p class="eyebrow">{{ todayLabel }}</p>
        <h1>{{ greeting }}，{{ userName }}</h1>
        <p class="sub">
          今日在住 <strong>{{ today.occupied ?? 0 }}</strong> 位客户，入住率
          <strong>{{ today.occupancyRate ?? 0 }}%</strong>，还有
          <strong>{{ pendingTotal }}</strong> 项待处理事项。
        </p>
      </div>
      <div class="hero-actions">
        <el-button type="primary" @click="go('/orders?new=1')">
          <Icon name="plus" :size="15" /><span style="margin-left: 6px">新建订单</span>
        </el-button>
        <el-button @click="go('/calendar')">查看房态</el-button>
      </div>
    </header>

    <section class="kpis">
      <article v-for="kpi in kpis" :key="kpi.label" class="kpi">
        <div class="kpi-top">
          <span class="kpi-label">{{ kpi.label }}</span>
          <span class="kpi-icon" :class="kpi.tone"><Icon :name="kpi.icon" :size="16" /></span>
        </div>
        <strong class="kpi-value">{{ kpi.value }}<small v-if="kpi.unit">{{ kpi.unit }}</small></strong>
        <span class="kpi-hint">{{ kpi.hint }}</span>
      </article>
    </section>

    <div class="grid-main">
      <section class="card trend-card">
        <div class="card-title">
          <span>近 14 天营收</span>
          <small>单位：元</small>
        </div>
        <div class="chart">
          <div v-for="point in trend" :key="point.date" class="bar-wrap">
            <span class="bar-value">{{ short(point.revenue) }}</span>
            <div class="bar" :style="{ height: barHeight(point.revenue) }" />
            <span class="bar-label">{{ point.date.slice(5) }}</span>
          </div>
          <div v-if="!trend.length" class="chart-empty">暂无营收数据</div>
        </div>
      </section>

      <section class="card reminders">
        <div class="card-title">
          <span>待办提醒</span>
          <small>{{ pendingTotal }} 项</small>
        </div>
        <ul>
          <li v-for="item in reminders" :key="item.title" @click="go(item.link)">
            <span class="dot" :class="{ zero: !item.count }" />
            <div class="r-text">
              <strong>{{ item.title }}</strong>
              <span>{{ item.module }}</span>
            </div>
            <span class="count" :class="{ zero: !item.count }">{{ item.count }}</span>
            <Icon name="chevron" :size="14" class="chev" />
          </li>
        </ul>
      </section>
    </div>

    <div class="grid-bottom">
      <section class="card">
        <div class="card-title"><span>快捷入口</span></div>
        <div class="quick-grid">
          <button v-for="entry in quickEntries" :key="entry.title" class="quick" @click="go(entry.path)">
            <span class="q-icon"><Icon :name="entry.icon" :size="18" /></span>
            <span>{{ entry.title }}</span>
          </button>
        </div>
      </section>

      <section class="card channel">
        <div class="card-title"><span>渠道占比</span><small>本月累计</small></div>
        <div v-if="channels.length" class="channel-list">
          <div v-for="ch in channels" :key="ch.source" class="channel-row">
            <span class="ch-name">{{ ch.source }}</span>
            <div class="ch-track"><div class="ch-fill" :style="{ width: ch.ratio + '%' }" /></div>
            <span class="ch-num">{{ ch.count }} 单</span>
          </div>
        </div>
        <el-empty v-else description="本月暂无成交渠道数据" :image-size="60" />
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import Icon from '../components/Icon.vue'
import { tenantHttp } from '../api/client'
import { USER_KEY, TENANT_KEY } from '../api/http'

const router = useRouter()
const today = ref<any>({})
const reminders = ref<any[]>([])
const trend = ref<any[]>([])
const channels = ref<any[]>([])

const todayLabel = new Date().toLocaleDateString('zh-CN', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  weekday: 'long'
})

const user = computed(() => {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || '{}')
  } catch {
    return {}
  }
})
const userName = computed(() => user.value.realName || user.value.username || '管理员')
const tenantCode = computed(() => localStorage.getItem(TENANT_KEY) || '')

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 11) return '早上好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const pendingTotal = computed(() => reminders.value.reduce((sum, r) => sum + Number(r.count || 0), 0))

const kpis = computed(() => [
  {
    label: '今日实收',
    value: money(today.value.revenue),
    unit: '元',
    hint: `客单价 ¥${money(today.value.avgOrderAmount)}`,
    icon: 'chart',
    tone: 'green'
  },
  {
    label: '在住客户',
    value: today.value.occupied ?? 0,
    unit: '位',
    hint: `入住率 ${today.value.occupancyRate ?? 0}%`,
    icon: 'users',
    tone: 'blue'
  },
  {
    label: '今日新签',
    value: today.value.newOrders ?? 0,
    unit: '单',
    hint: `线索转化 ${today.value.conversionRate ?? 0}%`,
    icon: 'receipt',
    tone: 'bronze'
  },
  {
    label: '今日入/出馆',
    value: `${today.value.checkins ?? 0} / ${today.value.checkouts ?? 0}`,
    hint: `退订率 ${today.value.refundRate ?? 0}%`,
    icon: 'building',
    tone: 'slate'
  }
])

const quickEntries = [
  { title: '档期日历', icon: 'calendar', path: '/calendar' },
  { title: '订单收款', icon: 'receipt', path: '/orders' },
  { title: '办理入馆', icon: 'building', path: '/stay' },
  { title: '护理看板', icon: 'shield', path: '/care' },
  { title: '线索跟进', icon: 'users', path: '/crm' },
  { title: '经营报表', icon: 'chart', path: '/reports' },
  { title: '公众号', icon: 'wechat', path: '/mp/accounts' },
  { title: '系统设置', icon: 'settings', path: '/system/config' }
]

function money(value: any) {
  return Number(value || 0).toLocaleString('zh-CN', { maximumFractionDigits: 0 })
}

function short(value: any) {
  const num = Number(value || 0)
  if (num >= 10000) {
    return `${(num / 10000).toFixed(1)}w`
  }
  return num ? String(num) : ''
}

function barHeight(value: any) {
  const max = Math.max(...trend.value.map((t) => Number(t.revenue || 0)), 1)
  const percent = (Number(value || 0) / max) * 100
  return `${Math.max(percent, 2)}%`
}

function go(path: string) {
  router.push(path)
}

async function load() {
  const [wb, monthly]: any = await Promise.all([
    tenantHttp.get('/api/tenant/reports/workbench'),
    tenantHttp.get('/api/tenant/reports/monthly')
  ])
  today.value = wb?.data?.today || {}
  reminders.value = wb?.data?.reminders || []
  const daily: any[] = monthly?.data?.dailyTrend || []
  // 月报返回整月趋势，工作台只看「截至今天」的最近 14 天，避免把未来日期画成空柱
  const now = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  const todayText = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
  trend.value = daily.filter((d) => String(d.date) <= todayText).slice(-14)
  channels.value = monthly?.data?.channelMix || []
}

onMounted(load)
</script>

<style scoped>
.workbench {
  padding: var(--space-5) var(--space-6) var(--space-7);
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  max-width: var(--content-max);
  margin: 0 auto;
}
.hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  padding: 4px 2px 2px;
}
.eyebrow {
  margin: 0 0 6px;
  font-size: 12px;
  letter-spacing: 0.08em;
  color: var(--brand-600);
  text-transform: uppercase;
}
.hero h1 {
  margin: 0;
  font-size: 26px;
  font-weight: 650;
  color: var(--ink-900);
  letter-spacing: -0.01em;
}
.hero .sub {
  margin: 8px 0 0;
  color: var(--ink-500);
  font-size: 13px;
}
.hero .sub strong {
  color: var(--ink-900);
  font-weight: 600;
}
.hero-actions {
  display: flex;
  gap: 10px;
}

.kpis {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--space-3);
}
.kpi {
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  box-shadow: var(--shadow-xs);
}
.kpi-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.kpi-label {
  font-size: 12.5px;
  color: var(--ink-500);
}
.kpi-icon {
  width: 26px;
  height: 26px;
  border-radius: 8px;
  display: grid;
  place-items: center;
}
.kpi-icon.green { background: var(--brand-50); color: var(--brand-600); }
.kpi-icon.blue { background: #eef2f7; color: #4a5f7a; }
.kpi-icon.bronze { background: var(--bronze-100); color: var(--bronze-600); }
.kpi-icon.slate { background: #f0f0ec; color: var(--ink-500); }
.kpi-value {
  font-size: 26px;
  font-weight: 650;
  letter-spacing: -0.02em;
  color: var(--ink-900);
  font-variant-numeric: tabular-nums;
}
.kpi-value small {
  font-size: 12px;
  font-weight: 500;
  color: var(--ink-400);
  margin-left: 4px;
}
.kpi-hint {
  font-size: 12px;
  color: var(--ink-400);
}

.grid-main {
  display: grid;
  grid-template-columns: 1.55fr 1fr;
  gap: var(--space-3);
}
.grid-bottom {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: var(--space-3);
}

.chart {
  height: 210px;
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 8px 4px 0;
  position: relative;
}
.bar-wrap {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  height: 100%;
  gap: 6px;
}
.bar {
  width: 100%;
  max-width: 26px;
  border-radius: 5px 5px 2px 2px;
  background: linear-gradient(180deg, #4fae8d, #2a8068);
  min-height: 3px;
  transition: height 0.4s ease;
}
.bar-wrap:nth-child(7n) .bar {
  background: linear-gradient(180deg, #c49a6c, #a97a4c);
}
.bar-value {
  font-size: 10px;
  color: var(--ink-400);
  font-variant-numeric: tabular-nums;
}
.bar-label {
  font-size: 10px;
  color: var(--ink-300);
}
.chart-empty {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  color: var(--ink-400);
  font-size: 13px;
}

.reminders ul {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
}
.reminders li {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 4px;
  border-bottom: 1px dashed var(--line);
  cursor: pointer;
  transition: background 0.15s ease;
}
.reminders li:last-child { border-bottom: 0; }
.reminders li:hover { background: var(--surface-sunken); }
.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--bronze-600);
  flex: 0 0 6px;
}
.dot.zero { background: var(--line-strong); }
.r-text {
  display: flex;
  flex-direction: column;
  line-height: 1.35;
  flex: 1;
  min-width: 0;
}
.r-text strong {
  font-size: 13px;
  font-weight: 550;
  color: var(--ink-900);
}
.r-text span {
  font-size: 11px;
  color: var(--ink-400);
}
.count {
  font-size: 15px;
  font-weight: 650;
  color: var(--bronze-600);
  font-variant-numeric: tabular-nums;
}
.count.zero { color: var(--ink-300); }
.chev { color: var(--ink-300); }

.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}
.quick {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
  padding: 12px;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  background: var(--surface-sunken);
  cursor: pointer;
  font-size: 12.5px;
  color: var(--ink-700);
  transition: all 0.15s ease;
}
.quick:hover {
  background: var(--brand-50);
  border-color: var(--brand-100);
  color: var(--brand-700);
  transform: translateY(-1px);
}
.q-icon {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  background: var(--surface);
  border: 1px solid var(--line);
  color: var(--brand-600);
}

.channel-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.channel-row {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12.5px;
}
.ch-name {
  width: 84px;
  color: var(--ink-700);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.ch-track {
  flex: 1;
  height: 6px;
  border-radius: 3px;
  background: var(--surface-sunken);
  overflow: hidden;
}
.ch-fill {
  height: 100%;
  border-radius: 3px;
  background: linear-gradient(90deg, #4fae8d, #2a8068);
}
.ch-num {
  width: 48px;
  text-align: right;
  color: var(--ink-500);
  font-variant-numeric: tabular-nums;
}
</style>
