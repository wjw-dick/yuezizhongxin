<template>
  <div class="page">
    <header class="page-head">
      <div>
        <h1>租户管理</h1>
        <p class="sub">开通、停用、续期与配额调整；平台侧只看汇总，不进入租户业务数据。</p>
      </div>
      <div class="actions">
        <el-button @click="load">
          <Icon name="refresh" :size="15" style="margin-right: 5px" />刷新
        </el-button>
        <el-button type="primary" @click="openCreate">
          <Icon name="plus" :size="15" style="margin-right: 5px" />开通租户
        </el-button>
      </div>
    </header>

    <section class="card filters">
      <el-input
        v-model="query.keyword"
        placeholder="租户名称或标识"
        clearable
        style="width: 220px"
        @keyup.enter="search"
      >
        <template #prefix><Icon name="search" :size="15" /></template>
      </el-input>
      <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 150px">
        <el-option v-for="(v, k) in TENANT_STATUS" :key="k" :label="v.text" :value="k" />
      </el-select>
      <el-checkbox v-model="query.expiringSoon" label="仅看 30 天内到期" />
      <span class="spacer" />
      <el-button @click="reset">重置</el-button>
      <el-button type="primary" plain @click="search">查询</el-button>
    </section>

    <section class="card flush">
      <el-table v-loading="loading" :data="rows" style="width: 100%" empty-text="没有符合条件的租户">
        <el-table-column label="租户" min-width="230">
          <template #default="{ row }">
            <div class="tenant-cell">
              <span class="tenant-logo">{{ (row.tenantName || '租').slice(0, 1) }}</span>
              <div>
                <div class="cell-main">{{ row.tenantName }}</div>
                <div class="cell-sub">
                  <span class="mono">{{ row.tenantCode }}</span>
                  <span v-if="row.industry" class="dot-sep">·</span>
                  <span v-if="row.industry">{{ row.industry }}</span>
                </div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="套餐" width="140">
          <template #default="{ row }">
            <span class="cell-main">{{ row.packageName || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <span class="pill" :class="tenantStatus(row.status).tone">{{ tenantStatus(row.status).text }}</span>
          </template>
        </el-table-column>

        <el-table-column label="席位" width="150">
          <template #default="{ row }">
            <div class="quota">
              <div class="quota-bar">
                <i :style="{ width: percent(row.seatUsed, row.seatLimit) + '%' }" />
              </div>
              <span class="quota-text">{{ row.seatUsed }}/{{ row.seatLimit }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="到期时间" width="176">
          <template #default="{ row }">
            <span>{{ fmtDate(row.expireTime || row.trialEndTime) }}</span>
            <span class="left" :class="{ urgent: isUrgent(row) }">{{ leftText(row) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="210" align="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button link type="primary" @click="openRenew(row)">续期</el-button>
            <el-button
              v-if="row.status !== 'DISABLED'"
              link
              type="danger"
              @click="toggle(row, false)"
            >停用</el-button>
            <el-button v-else link type="primary" @click="toggle(row, true)">恢复</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <span class="muted">共 {{ total }} 个租户</span>
        <el-pagination
          v-model:current-page="query.pageNum"
          :page-size="query.pageSize"
          :total="total"
          layout="prev, pager, next"
          background
          @current-change="load"
        />
      </div>
    </section>

    <!-- 开通租户 -->
    <el-dialog v-model="createVisible" title="开通租户" width="620" :close-on-click-modal="false">
      <p class="dialog-hint">
        提交后自动完成：创建租户 → 绑定套餐与配额 → 初始化 9 个角色 / 14 个菜单 / 12 项参数 → 生成主管理员。
      </p>
      <el-form :model="form" label-position="top" class="create-form">
        <div class="form-grid">
          <el-form-item label="租户标识" required>
            <el-input v-model="form.tenantCode" placeholder="小写字母/数字/中划线，如 yuezi-hz" />
          </el-form-item>
          <el-form-item label="租户名称" required>
            <el-input v-model="form.tenantName" placeholder="如 杭州西湖旗舰店" />
          </el-form-item>
          <el-form-item label="所属行业">
            <el-input v-model="form.industry" placeholder="母婴照护" />
          </el-form-item>
          <el-form-item label="套餐" required>
            <el-select v-model="form.packageId" placeholder="选择套餐" style="width: 100%">
              <el-option v-for="p in packages" :key="p.id" :value="p.id" :label="`${p.packageName} · ¥${p.price}/${p.billingCycle}`" />
            </el-select>
          </el-form-item>
          <el-form-item label="联系人">
            <el-input v-model="form.contactName" />
          </el-form-item>
          <el-form-item label="联系电话">
            <el-input v-model="form.contactPhone" />
          </el-form-item>
          <el-form-item label="试用天数">
            <el-input-number v-model="form.trialDays" :min="0" :max="90" style="width: 100%" />
          </el-form-item>
          <el-form-item label="主管理员账号" required>
            <el-input v-model="form.adminUsername" placeholder="admin" />
          </el-form-item>
          <el-form-item label="主管理员姓名">
            <el-input v-model="form.adminRealName" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="submitCreate">提交开通</el-button>
      </template>
    </el-dialog>

    <!-- 开通结果：一次性口令 -->
    <el-dialog v-model="resultVisible" title="开通成功" width="480">
      <p class="dialog-hint">初始口令仅此一次返回，请立即复制并转交客户。</p>
      <div class="result-box">
        <div><span>租户标识</span><strong class="mono">{{ created.tenantCode }}</strong></div>
        <div><span>管理员账号</span><strong class="mono">{{ created.adminUsername }}</strong></div>
        <div><span>初始口令</span><strong class="mono pwd">{{ created.initialPassword }}</strong></div>
      </div>
      <template #footer>
        <el-button type="primary" @click="resultVisible = false">我已记录</el-button>
      </template>
    </el-dialog>

    <!-- 续期 -->
    <el-dialog v-model="renewVisible" title="租户续期" width="420">
      <p class="dialog-hint">{{ renewTarget.tenantName }} 当前到期 {{ fmtDate(renewTarget.expireTime || renewTarget.trialEndTime) }}</p>
      <el-form label-position="top">
        <el-form-item label="续期天数">
          <el-input-number v-model="renewDays" :min="1" :max="1095" style="width: 100%" />
        </el-form-item>
      </el-form>
      <div class="quick-days">
        <button v-for="d in [30, 90, 180, 365]" :key="d" @click="renewDays = d">{{ d }} 天</button>
      </div>
      <template #footer>
        <el-button @click="renewVisible = false">取消</el-button>
        <el-button type="primary" :loading="renewing" @click="submitRenew">确认续期</el-button>
      </template>
    </el-dialog>

    <!-- 详情 -->
    <el-drawer v-model="detailVisible" title="租户详情" size="480">
      <div v-if="detail" class="detail">
        <div class="detail-head">
          <span class="tenant-logo lg">{{ (detail.tenantName || '租').slice(0, 1) }}</span>
          <div>
            <h3>{{ detail.tenantName }}</h3>
            <p class="muted mono">{{ detail.tenantCode }}</p>
          </div>
          <span class="pill" :class="tenantStatus(detail.status).tone">{{ tenantStatus(detail.status).text }}</span>
        </div>

        <dl class="kv">
          <div><dt>套餐</dt><dd>{{ detail.packageName || '-' }}</dd></div>
          <div><dt>行业</dt><dd>{{ detail.industry || '-' }}</dd></div>
          <div><dt>联系人</dt><dd>{{ detail.contactName || '-' }} {{ detail.contactPhone || '' }}</dd></div>
          <div><dt>开通时间</dt><dd>{{ fmtDate(detail.createdTime) }}</dd></div>
          <div><dt>到期时间</dt><dd>{{ fmtDate(detail.expireTime || detail.trialEndTime) }}</dd></div>
        </dl>

        <h4 class="detail-sub">配额用量</h4>
        <div class="quota-list">
          <div v-for="q in quotas" :key="q.label" class="quota-row">
            <span>{{ q.label }}</span>
            <div class="quota-bar"><i :style="{ width: percent(q.used, q.limit) + '%' }" /></div>
            <span class="quota-text">{{ q.used }}/{{ q.limit }}{{ q.unit }}</span>
          </div>
        </div>

        <el-button class="detail-action" @click="openRenew(detail)">为该租户续期</el-button>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import Icon from '../../components/Icon.vue'
import {
  createTenant,
  disableTenant,
  getTenant,
  listPackages,
  listTenants,
  renewTenant,
  resumeTenant
} from '../../api/platform'
import { TENANT_STATUS, daysLeft, fmtDate, percent, tenantStatus } from '../../utils/format'

const loading = ref(false)
const rows = ref<any[]>([])
const total = ref(0)
const packages = ref<any[]>([])

const query = reactive({
  keyword: '',
  status: '',
  expiringSoon: false,
  pageNum: 1,
  pageSize: 10
})

const load = async () => {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      pageNum: query.pageNum,
      pageSize: query.pageSize
    }
    if (query.keyword) params.tenantName = query.keyword
    if (query.status) params.status = query.status
    if (query.expiringSoon) params.expiringSoon = true
    const res: any = await listTenants(params)
    rows.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

const search = () => {
  query.pageNum = 1
  load()
}
const reset = () => {
  query.keyword = ''
  query.status = ''
  query.expiringSoon = false
  search()
}

const leftText = (row: any) => {
  const left = daysLeft(row.expireTime || row.trialEndTime)
  if (left === null) return ''
  return left < 0 ? '已过期' : '剩 ' + left + ' 天'
}
const isUrgent = (row: any) => {
  const left = daysLeft(row.expireTime || row.trialEndTime)
  return left !== null && left <= 7
}

/* ---------- 开通 ---------- */
const createVisible = ref(false)
const creating = ref(false)
const resultVisible = ref(false)
const created = reactive({ tenantCode: '', adminUsername: '', initialPassword: '' })
const form = reactive({
  tenantCode: '',
  tenantName: '',
  industry: '母婴照护',
  contactName: '',
  contactPhone: '',
  packageId: undefined as number | undefined,
  trialDays: 14,
  adminUsername: 'admin',
  adminRealName: ''
})

const openCreate = async () => {
  Object.assign(form, {
    tenantCode: '',
    tenantName: '',
    industry: '母婴照护',
    contactName: '',
    contactPhone: '',
    packageId: packages.value[0]?.id,
    trialDays: 14,
    adminUsername: 'admin',
    adminRealName: ''
  })
  createVisible.value = true
}

const submitCreate = async () => {
  if (!form.tenantCode || !form.tenantName || !form.packageId) {
    ElMessage.warning('请填写租户标识、名称并选择套餐')
    return
  }
  creating.value = true
  try {
    const res: any = await createTenant({ ...form })
    Object.assign(created, res.data)
    createVisible.value = false
    resultVisible.value = true
    load()
  } finally {
    creating.value = false
  }
}

/* ---------- 启停 ---------- */
const toggle = async (row: any, resume: boolean) => {
  const action = resume ? '恢复' : '停用'
  await ElMessageBox.confirm(
    resume
      ? `恢复后「${row.tenantName}」可重新登录，状态回到正常。`
      : `停用后「${row.tenantName}」将立即拒绝登录并清理会话与缓存，确定继续？`,
    action + '租户',
    { type: 'warning', confirmButtonText: action, cancelButtonText: '取消' }
  )
  if (resume) await resumeTenant(row.id)
  else await disableTenant(row.id)
  ElMessage.success(action + '成功')
  load()
}

/* ---------- 续期 ---------- */
const renewVisible = ref(false)
const renewing = ref(false)
const renewDays = ref(365)
const renewTarget = ref<any>({})
const openRenew = (row: any) => {
  renewTarget.value = row
  renewDays.value = 365
  renewVisible.value = true
}
const submitRenew = async () => {
  renewing.value = true
  try {
    await renewTenant(renewTarget.value.id, renewDays.value)
    ElMessage.success('续期成功')
    renewVisible.value = false
    load()
  } finally {
    renewing.value = false
  }
}

/* ---------- 详情 ---------- */
const detailVisible = ref(false)
const detail = ref<any>(null)
const openDetail = async (row: any) => {
  detailVisible.value = true
  detail.value = row
  const res: any = await getTenant(row.id)
  detail.value = res.data
}

const quotas = computed(() => {
  const d = detail.value || {}
  return [
    { label: '席位', used: d.seatUsed || 0, limit: d.seatLimit || 0, unit: '' },
    { label: '公众号', used: d.mpUsed || 0, limit: d.mpLimit || 0, unit: '' },
    { label: '存储', used: d.storageUsedGb || 0, limit: d.storageLimitGb || 0, unit: ' GB' },
    { label: '短信', used: d.smsUsed || 0, limit: d.smsLimit || 0, unit: ' 条' },
    { label: '接口', used: d.apiUsed || 0, limit: d.apiLimit || 0, unit: ' 次' }
  ]
})

onMounted(async () => {
  const p: any = await listPackages()
  packages.value = p.data || []
  load()
})
</script>

<style scoped>
.filters { display: flex; align-items: center; gap: 12px; padding: 14px 18px; }
.filters .spacer { flex: 1; }

.tenant-cell { display: flex; align-items: center; gap: 10px; }
.tenant-logo {
  width: 32px; height: 32px; border-radius: 9px; flex: 0 0 32px;
  display: grid; place-items: center;
  background: var(--brand-50); color: var(--brand-700);
  font-size: 13px; font-weight: 600;
  border: 1px solid var(--brand-100);
}
.tenant-logo.lg { width: 44px; height: 44px; flex-basis: 44px; font-size: 17px; border-radius: 12px; }
.cell-main { font-size: 13.5px; color: var(--ink-900); }
.cell-sub { font-size: 11.5px; color: var(--ink-400); }
.dot-sep { margin: 0 5px; }
.left { margin-left: 7px; font-size: 11.5px; color: var(--ink-400); white-space: nowrap; }
.left.urgent { color: #b4552f; }

.quota { display: flex; align-items: center; gap: 8px; }
.quota-bar { width: 62px; flex: 0 0 62px; height: 5px; border-radius: 999px; background: #efece5; overflow: hidden; }
.quota-bar i { display: block; height: 100%; background: var(--brand-500); border-radius: 999px; }
.quota-text { font-size: 11.5px; color: var(--ink-500); white-space: nowrap; }

.pager { display: flex; align-items: center; justify-content: space-between; padding: 12px 18px; border-top: 1px solid var(--line); }

.dialog-hint { margin: 0 0 18px; font-size: 12.5px; color: var(--ink-500); line-height: 1.7; }
.create-form .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0 16px; }

.result-box {
  border: 1px solid var(--line); border-radius: var(--radius-md);
  background: var(--surface-sunken); padding: 14px 16px;
  display: flex; flex-direction: column; gap: 10px;
}
.result-box div { display: flex; justify-content: space-between; align-items: center; font-size: 13px; }
.result-box span { color: var(--ink-500); }
.result-box strong { font-size: 14px; }
.result-box .pwd { color: var(--brand-700); letter-spacing: 0.04em; }

.quick-days { display: flex; gap: 8px; }
.quick-days button {
  border: 1px solid var(--line); background: var(--surface); cursor: pointer;
  border-radius: 999px; padding: 4px 12px; font-size: 12px; color: var(--ink-500);
}
.quick-days button:hover { border-color: var(--brand-500); color: var(--brand-700); }

.detail { display: flex; flex-direction: column; gap: 22px; }
.detail-head { display: flex; align-items: center; gap: 12px; }
.detail-head h3 { margin: 0; font-size: 17px; }
.detail-head p { margin: 2px 0 0; }
.detail-head .pill { margin-left: auto; }

.kv { margin: 0; display: grid; grid-template-columns: 1fr 1fr; gap: 14px 18px; }
.kv div { display: flex; flex-direction: column; gap: 2px; }
.kv dt { font-size: 11.5px; color: var(--ink-400); }
.kv dd { margin: 0; font-size: 13px; color: var(--ink-900); }

.detail-sub { margin: 0; font-size: 12.5px; color: var(--ink-500); font-weight: 600; letter-spacing: 0.04em; }
.quota-list { display: flex; flex-direction: column; gap: 12px; }
.quota-row { display: grid; grid-template-columns: 52px 1fr 90px; align-items: center; gap: 10px; font-size: 12.5px; color: var(--ink-700); }
.quota-row .quota-text { text-align: right; }
.detail-action { align-self: flex-start; }
</style>
