<template>
  <div class="page">
    <header class="page-head">
      <div>
        <h1>账单与计费</h1>
        <p class="sub">按席位用量生成账期账单，状态流转为 待支付 → 已支付 / 已逾期。</p>
      </div>
      <div class="actions">
        <el-button :loading="scanning" @click="scanOverdue">逾期扫描</el-button>
        <el-button type="primary" :loading="generating" @click="generate">生成账单</el-button>
      </div>
    </header>

    <section class="card filters">
      <el-date-picker
        v-model="query.period"
        type="month"
        placeholder="账期"
        value-format="YYYY-MM"
        style="width: 150px"
        @change="search"
      />
      <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px" @change="search">
        <el-option v-for="(v, k) in BILL_STATUS" :key="k" :label="v.text" :value="k" />
      </el-select>
      <span class="spacer" />
      <span class="muted">合计 {{ total }} 笔</span>
    </section>

    <section class="card flush">
      <el-table v-loading="loading" :data="rows" style="width: 100%" empty-text="该账期还没有账单">
        <el-table-column label="账期" width="110">
          <template #default="{ row }"><span class="mono">{{ row.billPeriod }}</span></template>
        </el-table-column>
        <el-table-column label="租户" min-width="140">
          <template #default="{ row }">
            <span class="mono">{{ tenantCode(row.tenantId) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="itemName" label="计费项" min-width="160" />
        <el-table-column label="数量" width="90" align="right">
          <template #default="{ row }"><span class="num">{{ row.quantity }}</span></template>
        </el-table-column>
        <el-table-column label="金额" width="130" align="right">
          <template #default="{ row }"><span class="money">{{ money(row.amount) }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <span class="pill" :class="billStatus(row.status).tone">{{ billStatus(row.status).text }}</span>
          </template>
        </el-table-column>
        <el-table-column label="支付时间" width="150">
          <template #default="{ row }"><span class="muted">{{ fmtDate(row.payTime) }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="110" align="right">
          <template #default="{ row }">
            <el-button v-if="row.status !== 'PAID'" link type="primary" @click="pay(row)">标记已支付</el-button>
            <span v-else class="muted">已完成</span>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="genVisible" title="生成账期账单" width="420">
      <p class="dialog-hint">按账期内每个租户的席位数生成账单；同一账期重复生成不会产生重复账单。</p>
      <el-date-picker v-model="genPeriod" type="month" value-format="YYYY-MM" style="width: 100%" />
      <template #footer>
        <el-button @click="genVisible = false">取消</el-button>
        <el-button type="primary" :loading="generating" @click="submitGenerate">生成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { generateBills, listBills, listTenants, markOverdue, payBill } from '../../api/platform'
import { BILL_STATUS, billStatus, fmtDate, money } from '../../utils/format'

const loading = ref(false)
const generating = ref(false)
const scanning = ref(false)
const rows = ref<any[]>([])
const total = ref(0)
const tenants = ref<any[]>([])

const query = reactive({ period: '', status: '' })

const load = async () => {
  loading.value = true
  try {
    const params: Record<string, unknown> = { pageSize: 100 }
    if (query.period) params.billPeriod = query.period
    if (query.status) params.status = query.status
    const res: any = await listBills(params)
    rows.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}
const search = () => load()

const tenantCode = (id: number) => tenants.value.find((t) => t.id === id)?.tenantCode || `#${id}`

const genVisible = ref(false)
const genPeriod = ref(new Date().toISOString().slice(0, 7))
const generate = () => {
  genPeriod.value = query.period || new Date().toISOString().slice(0, 7)
  genVisible.value = true
}
const submitGenerate = async () => {
  generating.value = true
  try {
    const res: any = await generateBills(genPeriod.value)
    ElMessage.success(`已生成 ${res.data ?? 0} 笔账单`)
    genVisible.value = false
    load()
  } finally {
    generating.value = false
  }
}

const scanOverdue = async () => {
  scanning.value = true
  try {
    const res: any = await markOverdue()
    ElMessage.success(`已标记 ${res.data ?? 0} 笔逾期`)
    load()
  } finally {
    scanning.value = false
  }
}

const pay = async (row: any) => {
  await payBill(row.id)
  ElMessage.success('已标记为已支付')
  load()
}

onMounted(async () => {
  const t: any = await listTenants({ pageSize: 200 })
  tenants.value = t.data?.records || []
  load()
})
</script>

<style scoped>
.filters { display: flex; align-items: center; gap: 12px; padding: 14px 18px; }
.filters .spacer { flex: 1; }
.dialog-hint { margin: 0 0 16px; font-size: 12.5px; color: var(--ink-500); line-height: 1.7; }
</style>
