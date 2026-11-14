<template>
  <div class="page">
    <header class="page-head">
      <div>
        <h1>运营概览</h1>
        <p class="sub">平台只统计租户汇总与状态，不展示任何租户业务明细。</p>
      </div>
      <div class="actions">
        <el-button :loading="loading" @click="loadAll">
          <Icon name="refresh" :size="15" style="margin-right: 5px" />刷新
        </el-button>
      </div>
    </header>

    <section class="kpis">
      <article v-for="k in kpis" :key="k.label" class="kpi">
        <span class="kpi-label">{{ k.label }}</span>
        <strong class="kpi-value">{{ k.value }}</strong>
        <span class="kpi-foot" :class="k.tone">{{ k.foot }}</span>
      </article>
    </section>

    <div class="cols">
      <section class="card flush">
        <div class="card-head">
          <div>
            <h2>到期与试用租户</h2>
            <p>按到期时间升序，30 天内到期需提前续费</p>
          </div>
          <router-link to="/platform/tenants" class="more">全部租户 →</router-link>
        </div>

        <el-table :data="expiring" style="width: 100%" :show-header="true" empty-text="暂无临近到期租户">
          <el-table-column label="租户" min-width="180">
            <template #default="{ row }">
              <div class="cell-main">{{ row.tenantName }}</div>
              <div class="cell-sub mono">{{ row.tenantCode }}</div>
            </template>
          </el-table-column>
          <el-table-column label="套餐" width="130">
            <template #default="{ row }">{{ row.packageName || '-' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="96">
            <template #default="{ row }">
              <span class="pill" :class="tenantStatus(row.status).tone">{{ tenantStatus(row.status).text }}</span>
            </template>
          </el-table-column>
          <el-table-column label="到期" width="150">
            <template #default="{ row }">
              <span>{{ fmtDate(row.expireTime || row.trialEndTime) }}</span>
              <span v-if="leftOf(row) !== null" class="left" :class="{ urgent: leftOf(row)! <= 7 }">
                {{ leftOf(row)! < 0 ? '已过期' : '剩 ' + leftOf(row) + ' 天' }}
              </span>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section class="card flush">
        <div class="card-head">
          <div>
            <h2>最近平台操作</h2>
            <p>开通、停用、改价等敏感操作留痕</p>
          </div>
          <router-link to="/platform/audits" class="more">审计日志 →</router-link>
        </div>

        <ul class="timeline">
          <li v-for="a in audits" :key="a.id">
            <span class="dot" :class="a.result === '成功' ? 'ok' : 'bad'" />
            <div class="tl-body">
              <p>
                <strong>{{ a.action }}</strong>
                <span class="tl-module">{{ a.module }}</span>
              </p>
              <p class="tl-meta">
                {{ a.operatorName || '系统' }} · {{ fmtDateTime(a.createdTime) }}
                <template v-if="a.tenantCode"> · {{ a.tenantCode }}</template>
              </p>
            </div>
          </li>
          <li v-if="!audits.length" class="empty">暂无平台操作记录</li>
        </ul>
      </section>
    </div>

    <section class="card bill-strip">
      <div class="bill-item">
        <span>本期应收</span>
        <strong class="money">{{ money(summary.receivable) }}</strong>
      </div>
      <span class="bill-sep" />
      <div class="bill-item">
        <span>已收</span>
        <strong class="money">{{ money(summary.paid) }}</strong>
      </div>
      <span class="bill-sep" />
      <div class="bill-item">
        <span>逾期笔数</span>
        <strong class="money">{{ summary.overdue }}</strong>
      </div>
      <router-link to="/platform/bills" class="more">去账单与计费 →</router-link>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import Icon from '../../components/Icon.vue'
import { listAudits, listBills, listTenants } from '../../api/platform'
import { daysLeft, fmtDate, fmtDateTime, money, tenantStatus } from '../../utils/format'

const loading = ref(false)
const tenants = ref<any[]>([])
const audits = ref<any[]>([])
const bills = ref<any[]>([])

const loadAll = async () => {
  loading.value = true
  try {
    const [t, a, b]: any[] = await Promise.all([
      listTenants({ pageSize: 200 }),
      listAudits({ pageSize: 6 }),
      listBills({ pageSize: 200 })
    ])
    tenants.value = t.data?.records || []
    audits.value = a.data?.records || []
    bills.value = b.data?.records || []
  } finally {
    loading.value = false
  }
}

const countBy = (status: string) => tenants.value.filter((t) => t.status === status).length

const expiring = computed(() =>
  tenants.value
    .filter((t) => ['NORMAL', 'TRIAL'].includes(t.status))
    .map((t) => ({ ...t, _left: daysLeft(t.expireTime || t.trialEndTime) }))
    .filter((t) => t._left !== null && t._left <= 30)
    .sort((x, y) => (x._left as number) - (y._left as number))
    .slice(0, 6)
)

const leftOf = (row: any) => {
  const left = daysLeft(row.expireTime || row.trialEndTime)
  return left === null ? null : left
}

const kpis = computed(() => [
  { label: '租户总数', value: tenants.value.length, foot: '含试用与停用', tone: '' },
  { label: '正常运营', value: countBy('NORMAL'), foot: '已付费在服务期', tone: 'up' },
  { label: '试用中', value: countBy('TRIAL'), foot: '待转化', tone: '' },
  {
    label: '30 天内到期',
    value: expiring.value.length,
    foot: expiring.value.length ? '需跟进续费' : '暂无压力',
    tone: expiring.value.length ? 'warn' : ''
  }
])

const summary = reactive({ receivable: 0, paid: 0, overdue: 0 })
const recompute = () => {
  summary.receivable = bills.value
    .filter((b) => b.status !== 'PAID')
    .reduce((sum, b) => sum + Number(b.amount || 0), 0)
  summary.paid = bills.value.filter((b) => b.status === 'PAID').reduce((sum, b) => sum + Number(b.amount || 0), 0)
  summary.overdue = bills.value.filter((b) => b.status === 'OVERDUE').length
}

onMounted(async () => {
  await loadAll()
  recompute()
})
</script>

<style scoped>
.kpis { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 14px; }
.kpi {
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.kpi-label { font-size: 12.5px; color: var(--ink-500); }
.kpi-value { font-size: 30px; font-weight: 650; letter-spacing: -0.02em; line-height: 1.2; }
.kpi-foot { font-size: 11.5px; color: var(--ink-400); }
.kpi-foot.up { color: #2c7a4f; }
.kpi-foot.warn { color: #9a6512; }

.cols { display: grid; grid-template-columns: minmax(0, 1.55fr) minmax(0, 1fr); gap: 14px; }
@media (max-width: 1100px) { .cols { grid-template-columns: 1fr; } }

.card-head {
  display: flex; align-items: flex-start; justify-content: space-between;
  gap: 12px; padding: 18px 20px 14px;
}
.card-head h2 { margin: 0; font-size: 15px; font-weight: 600; }
.card-head p { margin: 4px 0 0; font-size: 12px; color: var(--ink-400); }
.more { font-size: 12px; color: var(--brand-600); text-decoration: none; white-space: nowrap; }
.more:hover { text-decoration: underline; }

.cell-main { font-size: 13.5px; color: var(--ink-900); }
.cell-sub { font-size: 11.5px; color: var(--ink-400); }
.left { margin-left: 8px; font-size: 11.5px; color: var(--ink-400); }
.left.urgent { color: #b4552f; }

.timeline { list-style: none; margin: 0; padding: 4px 20px 18px; display: flex; flex-direction: column; gap: 14px; }
.timeline li { display: flex; gap: 10px; }
.timeline .dot { width: 7px; height: 7px; border-radius: 50%; margin-top: 7px; flex: 0 0 7px; background: #3aa173; }
.timeline .dot.bad { background: #c04a44; }
.tl-body p { margin: 0; font-size: 12.5px; color: var(--ink-700); }
.tl-body strong { font-weight: 550; }
.tl-module {
  margin-left: 6px; font-size: 10.5px; color: var(--ink-400);
  border: 1px solid var(--line); border-radius: 4px; padding: 0 4px;
}
.tl-meta { color: var(--ink-400) !important; font-size: 11.5px !important; margin-top: 2px !important; }

.bill-strip { display: flex; align-items: center; gap: 28px; padding: 16px 22px; }
.bill-item { display: flex; flex-direction: column; gap: 2px; }
.bill-item span { font-size: 11.5px; color: var(--ink-400); }
.bill-item strong { font-size: 18px; }
.bill-sep { width: 1px; height: 26px; background: var(--line); }
.bill-strip .more { margin-left: auto; }
</style>
