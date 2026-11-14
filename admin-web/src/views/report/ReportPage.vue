<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>经营报表</h1>
        <p class="sub">日报/月报口径以「已支付收款单」计营收，退订以「已退款」计；导出带水印与留痕，单次上限 5000 行</p>
      </div>
      <div class="actions">
        <el-button @click="loadCurrent"><Icon name="refresh" :size="15" /><span style="margin-left: 6px">刷新</span></el-button>
        <el-button type="primary" @click="exportDialog = true">
          <Icon name="upload" :size="15" /><span style="margin-left: 6px">导出 Excel</span>
        </el-button>
      </div>
    </div>

    <el-tabs v-model="tab" class="biz-tabs" @tab-change="loadCurrent">
      <el-tab-pane label="经营日报" name="daily">
        <div class="toolbar">
          <el-date-picker v-model="dailyDate" type="date" value-format="YYYY-MM-DD" size="small" style="width: 150px" @change="loadDaily" />
          <el-button size="small" text @click="dailyDate = today(); loadDaily()">今天</el-button>
          <span class="spacer" />
          <span class="muted">{{ daily.date || '' }}</span>
        </div>
        <div class="metric-grid">
          <div v-for="m in dailyMetrics" :key="m.label" class="metric">
            <span class="metric-label">{{ m.label }}</span>
            <strong class="metric-value">{{ m.value }}</strong>
            <em v-if="m.hint" class="metric-hint">{{ m.hint }}</em>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="经营月报" name="monthly">
        <div class="toolbar">
          <el-date-picker v-model="month" type="month" value-format="YYYY-MM" size="small" style="width: 140px" @change="loadMonthly" />
          <span class="spacer" />
          <span class="muted">{{ monthly.month || '' }}</span>
        </div>

        <div class="metric-grid">
          <div v-for="m in monthlyMetrics" :key="m.label" class="metric">
            <span class="metric-label">{{ m.label }}</span>
            <strong class="metric-value">{{ m.value }}</strong>
            <em v-if="m.hint" class="metric-hint">{{ m.hint }}</em>
          </div>
        </div>

        <div class="chart-card">
          <p class="card-title">每日营收趋势<small>单位：元</small></p>
          <div class="bar-chart">
            <div v-for="point in monthly.dailyTrend || []" :key="point.date" class="bar-col">
              <div class="bar" :style="{ height: barHeight(point.revenue) }" :title="`${point.date} ¥${point.revenue}`" />
              <span class="bar-label">{{ String(point.date).slice(8) }}</span>
            </div>
            <p v-if="!(monthly.dailyTrend || []).length" class="muted">本月暂无数据</p>
          </div>
        </div>

        <div class="chart-card">
          <p class="card-title">渠道占比<small>按下单来源统计</small></p>
          <div class="channel-list">
            <div v-for="c in monthly.channelMix || []" :key="c.source" class="channel-row">
              <span class="channel-name">{{ c.source }}</span>
              <div class="channel-bar"><span :style="{ width: `${c.ratio}%` }" /></div>
              <span class="channel-num">{{ c.count }} 单 · {{ c.ratio }}%</span>
            </div>
            <p v-if="!(monthly.channelMix || []).length" class="muted">暂无渠道数据</p>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="护理报表" name="care">
        <div class="toolbar">
          <el-date-picker v-model="careRange" type="daterange" size="small" value-format="YYYY-MM-DD" start-placeholder="起" end-placeholder="止" style="width: 240px" @change="loadCare" />
          <span class="spacer" />
        </div>
        <div class="metric-grid">
          <div v-for="m in careMetrics" :key="m.label" class="metric">
            <span class="metric-label">{{ m.label }}</span>
            <strong class="metric-value">{{ m.value }}</strong>
            <em v-if="m.hint" class="metric-hint">{{ m.hint }}</em>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="餐食报表" name="meal">
        <div class="toolbar">
          <el-date-picker v-model="mealRange" type="daterange" size="small" value-format="YYYY-MM-DD" start-placeholder="起" end-placeholder="止" style="width: 240px" @change="loadMeal" />
        </div>
        <div class="metric-grid">
          <div v-for="m in mealMetrics" :key="m.label" class="metric">
            <span class="metric-label">{{ m.label }}</span>
            <strong class="metric-value">{{ m.value }}</strong>
            <em v-if="m.hint" class="metric-hint">{{ m.hint }}</em>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="人力报表" name="staff">
        <div class="toolbar">
          <el-date-picker v-model="staffRange" type="daterange" size="small" value-format="YYYY-MM-DD" start-placeholder="起" end-placeholder="止" style="width: 240px" @change="loadStaff" />
        </div>
        <div class="card flush">
          <el-table v-loading="loading" :data="staffRows" size="small" stripe>
            <el-table-column prop="staffName" label="姓名" width="130">
              <template #default="{ row }">{{ row.staffName || row.name || `#${row.staffId}` }}</template>
            </el-table-column>
            <el-table-column prop="jobType" label="岗位" width="140" />
            <el-table-column prop="recordCount" label="护理记录" width="120" align="right" />
            <el-table-column prop="shiftCount" label="排班班次" width="120" align="right" />
            <el-table-column prop="attendanceDays" label="出勤天数" width="120" align="right" />
            <template #empty><div class="empty"><strong>该时间段无人力数据</strong></div></template>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 导出 -->
    <el-dialog v-model="exportDialog" title="导出 Excel" width="480px">
      <el-alert type="info" :closable="false" show-icon title="导出文件带租户水印与操作人留痕，单次最多 5000 行" style="margin-bottom: 14px" />
      <el-form :model="exportForm" label-position="top">
        <el-form-item label="导出模块" required>
          <el-select v-model="exportForm.module" style="width: 100%">
            <el-option label="订单明细" value="orders" />
            <el-option label="护理报表" value="care" />
            <el-option label="人力报表" value="staff" />
            <el-option label="餐食报表" value="meal" />
          </el-select>
        </el-form-item>
        <el-form-item label="导出用途" required>
          <el-input v-model="exportForm.purpose" placeholder="如：月度经营分析 / 财务对账" />
        </el-form-item>
        <el-form-item label="已提交审批（若开启导出审批）">
          <el-switch v-model="exportForm.approved" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="exportDialog = false">取消</el-button>
        <el-button type="primary" :loading="exporting" @click="doExport">开始导出</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import Icon from '../../components/Icon.vue'
import { tenantHttp } from '../../api/client'
import { downloadExport } from '../../api/resource'

const tab = ref('daily')
const loading = ref(false)
const exporting = ref(false)
const dailyDate = ref(today())
const month = ref(today().slice(0, 7))
const careRange = ref<string[] | null>(null)
const mealRange = ref<string[] | null>(null)
const staffRange = ref<string[] | null>(null)

const daily = ref<any>({})
const monthly = ref<any>({})
const care = ref<any>({})
const meal = ref<any>({})
const staffRows = ref<any[]>([])

const exportDialog = ref(false)
const exportForm = reactive<any>({ module: 'orders', purpose: '', approved: false })

function today() {
  return new Date().toISOString().slice(0, 10)
}

function money(v: any) {
  if (v === null || v === undefined || v === '') return '0.00'
  return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const dailyMetrics = computed(() => [
  { label: '入住率', value: `${daily.value.occupancyRate ?? 0}%`, hint: `在住 ${daily.value.occupied ?? 0} / 房间 ${daily.value.roomCount ?? 0}` },
  { label: '营收', value: `¥${money(daily.value.revenue)}`, hint: '已支付收款单' },
  { label: '客单价', value: `¥${money(daily.value.avgOrderAmount)}`, hint: `新单 ${daily.value.newOrders ?? 0} 笔` },
  { label: '入馆 / 出馆', value: `${daily.value.checkins ?? 0} / ${daily.value.checkouts ?? 0}`, hint: '当日口径' },
  { label: '新增线索', value: daily.value.leads ?? 0, hint: `转化 ${daily.value.converted ?? 0}` },
  { label: '退订率', value: `${daily.value.refundRate ?? 0}%`, hint: `退款 ¥${money(daily.value.refundAmount)}` },
  { label: '增值占比', value: `${daily.value.extraShare ?? 0}%`, hint: '增值 + 商品 / 总额' },
  { label: '周转天数', value: daily.value.turnoverDays ?? '-', hint: '平均在住天数' }
])

const monthlyMetrics = computed(() => [
  { label: '出租间夜', value: monthly.value.roomNights ?? 0, hint: `可售 ${monthly.value.capacityRoomNights ?? 0}` },
  { label: '入住率', value: `${monthly.value.occupancyRate ?? 0}%`, hint: '间夜口径' },
  { label: '周转', value: monthly.value.turnover ?? '-', hint: '次/月' },
  { label: '营收', value: `¥${money(monthly.value.revenue)}`, hint: `新单 ${monthly.value.newOrders ?? 0} 笔` },
  { label: '客单价', value: `¥${money(monthly.value.avgOrderAmount)}`, hint: '按月新单' },
  { label: '转化率', value: `${monthly.value.conversionRate ?? 0}%`, hint: '线索转订单' },
  { label: '退订率', value: `${monthly.value.refundRate ?? 0}%`, hint: '按月' },
  { label: '增值占比', value: `${monthly.value.extraRevenueShare ?? 0}%`, hint: '增值收入占比' }
])

const careMetrics = computed(() => [
  { label: '计划项', value: care.value.planItems ?? 0, hint: `已完成 ${care.value.finishedItems ?? 0}` },
  { label: '完成率', value: `${care.value.finishRate ?? 0}%`, hint: '含审批通过补录' },
  { label: '护理记录', value: care.value.records ?? 0, hint: '条' },
  { label: '异常单', value: care.value.abnormals ?? 0, hint: `闭环 ${care.value.abnormalClosed ?? 0}` },
  { label: '异常闭环率', value: `${care.value.abnormalCloseRate ?? 0}%` },
  { label: '紧急异常', value: care.value.urgentAbnormals ?? 0, hint: '需重点关注' },
  { label: '交接及时率', value: `${care.value.handoverRate ?? 0}%`, hint: '按班次结束时间' }
])

const mealMetrics = computed(() => [
  { label: '出餐份数', value: meal.value.meals ?? 0, hint: `${meal.value.from || ''} ~ ${meal.value.to || ''}` },
  { label: '已核对', value: meal.value.checked ?? 0, hint: `核对率 ${meal.value.checkRate ?? 0}%` },
  { label: '忌口冲突', value: meal.value.conflicts ?? 0, hint: '生成时被拦截' },
  { label: '加餐收入', value: `¥${money(meal.value.addOnCharge)}`, hint: '计入增值消费' }
])

function barHeight(value: any) {
  const max = Math.max(1, ...(monthly.value.dailyTrend || []).map((p: any) => Number(p.revenue || 0)))
  const ratio = Number(value || 0) / max
  return `${Math.max(3, ratio * 100)}%`
}

async function loadDaily() {
  const res: any = await tenantHttp.get('/api/tenant/reports/daily', { params: { date: dailyDate.value } })
  daily.value = res.data || {}
}

async function loadMonthly() {
  const res: any = await tenantHttp.get('/api/tenant/reports/monthly', { params: { month: month.value } })
  monthly.value = res.data || {}
}

async function loadCare() {
  const res: any = await tenantHttp.get('/api/tenant/reports/care', {
    params: { from: careRange.value?.[0], to: careRange.value?.[1] }
  })
  care.value = res.data || {}
}

async function loadMeal() {
  const res: any = await tenantHttp.get('/api/tenant/reports/meal', {
    params: { from: mealRange.value?.[0], to: mealRange.value?.[1] }
  })
  meal.value = res.data || {}
}

async function loadStaff() {
  loading.value = true
  try {
    const res: any = await tenantHttp.get('/api/tenant/reports/staff', {
      params: { from: staffRange.value?.[0], to: staffRange.value?.[1] }
    })
    staffRows.value = res.data || []
  } finally {
    loading.value = false
  }
}

function loadCurrent() {
  if (tab.value === 'daily') loadDaily()
  if (tab.value === 'monthly') loadMonthly()
  if (tab.value === 'care') loadCare()
  if (tab.value === 'meal') loadMeal()
  if (tab.value === 'staff') loadStaff()
}

async function doExport() {
  if (!exportForm.purpose.trim()) {
    ElMessage.warning('请填写导出用途（留痕必填）')
    return
  }
  exporting.value = true
  try {
    await downloadExport({
      module: exportForm.module,
      purpose: exportForm.purpose,
      approved: exportForm.approved
    })
    ElMessage.success('导出已开始，文件带水印与留痕')
    exportDialog.value = false
  } finally {
    exporting.value = false
  }
}

onMounted(async () => {
  await loadDaily()
  await loadMonthly()
})
</script>

<style scoped>
.biz-tabs :deep(.el-tabs__header) { margin-bottom: 0; padding: 0 4px; border-bottom: 1px solid var(--line); }
.biz-tabs :deep(.el-tabs__nav-wrap::after) { display: none; }
.biz-tabs :deep(.el-tabs__item) { height: 42px; font-size: 13.5px; color: var(--ink-500); }
.biz-tabs :deep(.el-tabs__item.is-active) { color: var(--brand-700); font-weight: 600; }
.biz-tabs :deep(.el-tabs__active-bar) { background: var(--brand-600); }
.biz-tabs :deep(.el-tabs__content) { display: flex; flex-direction: column; gap: var(--space-4); padding-top: var(--space-3); }

.metric-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: var(--space-3); }
.metric { background: var(--surface); border: 1px solid var(--line); border-radius: var(--radius-lg); padding: 16px 18px; display: flex; flex-direction: column; gap: 3px; box-shadow: var(--shadow-xs); }
.metric-label { font-size: 11.5px; color: var(--ink-400); letter-spacing: 0.03em; }
.metric-value { font-size: 23px; font-weight: 650; color: var(--ink-900); font-variant-numeric: tabular-nums; }
.metric-hint { font-style: normal; font-size: 11.5px; color: var(--ink-400); }

.chart-card { background: var(--surface); border: 1px solid var(--line); border-radius: var(--radius-lg); padding: 16px 18px; box-shadow: var(--shadow-xs); }
.card-title { margin: 0 0 14px; font-size: 14px; font-weight: 600; display: flex; align-items: baseline; justify-content: space-between; }
.card-title small { font-size: 11.5px; color: var(--ink-400); font-weight: 400; }
.bar-chart { display: flex; align-items: flex-end; gap: 4px; height: 170px; padding-top: 8px; overflow-x: auto; }
.bar-col { flex: 1 0 16px; display: flex; flex-direction: column; align-items: center; height: 100%; justify-content: flex-end; gap: 5px; }
.bar { width: 100%; max-width: 26px; border-radius: 4px 4px 0 0; background: linear-gradient(180deg, #6cc3a5, #2f8b70); min-height: 3px; transition: height 0.3s ease; }
.bar-label { font-size: 10px; color: var(--ink-400); }
.channel-list { display: flex; flex-direction: column; gap: 10px; }
.channel-row { display: flex; align-items: center; gap: 12px; font-size: 12.5px; }
.channel-name { flex: 0 0 90px; color: var(--ink-700); }
.channel-bar { flex: 1; height: 8px; border-radius: 999px; background: var(--line); overflow: hidden; }
.channel-bar span { display: block; height: 100%; border-radius: 999px; background: linear-gradient(90deg, #c99a4f, #a87f3a); }
.channel-num { flex: 0 0 130px; text-align: right; color: var(--ink-500); font-variant-numeric: tabular-nums; }
.empty { padding: 30px 0; text-align: center; color: var(--ink-400); font-size: 12px; }
.empty strong { display: block; color: var(--ink-700); font-size: 13px; margin-bottom: 4px; }
</style>
