<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>审计日志</h1>
        <p class="sub">只读留痕，租户内自动隔离；保留期 180 天以上，任何角色都没有修改或删除入口</p>
      </div>
      <div class="actions">
        <el-tag type="info" effect="plain" size="large">只读</el-tag>
        <el-button @click="load"><Icon name="refresh" :size="15" /><span style="margin-left: 6px">刷新</span></el-button>
      </div>
    </div>

    <div class="card flush">
      <div class="toolbar filter-bar">
        <el-select v-model="query.module" placeholder="全部模块" clearable size="small" style="width: 150px" @change="reload">
          <el-option v-for="m in modules" :key="m" :label="m" :value="m" />
        </el-select>
        <el-select v-model="query.action" placeholder="全部动作" clearable size="small" style="width: 130px" @change="reload">
          <el-option v-for="a in actions" :key="a" :label="a" :value="a" />
        </el-select>
        <el-input v-model="query.operatorName" placeholder="操作人" clearable size="small" style="width: 140px" @keyup.enter="reload" @clear="reload" />
        <el-date-picker v-model="range" type="daterange" size="small" value-format="YYYY-MM-DD HH:mm:ss" start-placeholder="起" end-placeholder="止" style="width: 260px" @change="reload" />
        <el-button size="small" @click="reload">查询</el-button>
        <span class="spacer" />
        <span class="muted">共 {{ total }} 条</span>
      </div>

      <el-table v-loading="loading" :data="rows" size="small" stripe>
        <el-table-column prop="createdTime" label="时间" width="160" />
        <el-table-column prop="module" label="模块" width="110" />
        <el-table-column prop="action" label="动作" width="100">
          <template #default="{ row }">
            <span class="badge" :class="actionClass(row.action)">{{ row.action }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="target" label="对象" min-width="160" />
        <el-table-column prop="operatorName" label="操作人" width="110" />
        <el-table-column prop="roleName" label="角色" width="120" />
        <el-table-column prop="ip" label="IP" width="130">
          <template #default="{ row }"><span class="mono">{{ row.ip || '-' }}</span></template>
        </el-table-column>
        <el-table-column label="结果" width="90">
          <template #default="{ row }">
            <span class="badge" :class="row.result === '成功' ? 'ok' : 'danger'">{{ row.result || '成功' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="详情" width="80" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row)">查看</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div class="empty"><strong>没有匹配的审计记录</strong>调整筛选条件后重试</div>
        </template>
      </el-table>

      <div class="pager">
        <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="prev, pager, next, total" small background @current-change="load" />
      </div>
    </div>

    <el-dialog v-model="detailDialog" title="审计详情" width="600px">
      <div class="audit-detail">
        <div class="detail-item"><span>时间</span><strong>{{ current.createdTime }}</strong></div>
        <div class="detail-item"><span>模块 / 动作</span><strong>{{ current.module }} / {{ current.action }}</strong></div>
        <div class="detail-item"><span>对象</span><strong>{{ current.target || '-' }}</strong></div>
        <div class="detail-item"><span>操作人</span><strong>{{ current.operatorName }}（{{ current.roleName }}）</strong></div>
        <div class="detail-item"><span>IP</span><strong class="mono">{{ current.ip || '-' }}</strong></div>
        <div class="detail-item full"><span>详情</span><pre>{{ pretty(current.detail) }}</pre></div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import Icon from '../../components/Icon.vue'
import { tenantHttp } from '../../api/client'

const loading = ref(false)
const rows = ref<any[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)
const range = ref<string[] | null>(null)
const query = reactive<{ module: string; action: string; operatorName: string }>({
  module: '',
  action: '',
  operatorName: ''
})

const modules = ['租户', '订单', '房型', '护理', '入馆', '会员', '内容', '系统', '公众号', '报表']
const actions = ['新增', '修改', '删除', '导出', '登录', '发布', '审核', '停用']

const detailDialog = ref(false)
const current = ref<any>({})

function actionClass(action: string) {
  return { 删除: 'danger', 导出: 'warn', 审核: 'info', 登录: 'info' }[action] || 'ok'
}

function pretty(detail: string) {
  if (!detail) return '无详情'
  try {
    return JSON.stringify(JSON.parse(detail), null, 2)
  } catch {
    return detail
  }
}

async function load() {
  loading.value = true
  try {
    const res: any = await tenantHttp.get('/api/tenant/audits', {
      params: {
        module: query.module || undefined,
        action: query.action || undefined,
        operatorName: query.operatorName || undefined,
        startTime: range.value?.[0],
        endTime: range.value?.[1],
        pageNum: pageNum.value,
        pageSize: pageSize.value
      }
    })
    rows.value = res.data?.records || []
    total.value = Number(res.data?.total || 0)
  } finally {
    loading.value = false
  }
}

function reload() {
  pageNum.value = 1
  load()
}

function showDetail(row: any) {
  current.value = row
  detailDialog.value = true
}

onMounted(load)
</script>

<style scoped>
.filter-bar { padding: 14px var(--space-5); border-bottom: 1px solid var(--line); background: var(--surface-sunken); }
.pager { display: flex; justify-content: flex-end; padding: 12px var(--space-5); border-top: 1px solid var(--line); }
.badge { display: inline-flex; align-items: center; height: 22px; padding: 0 8px; border-radius: 999px; font-size: 12px; background: #eef2f7; color: #4a5f7a; }
.badge.ok { background: #eef7f1; color: #2f7d55; }
.badge.warn { background: #fdf5e8; color: #9a6a18; }
.badge.danger { background: #fbeeed; color: #a83c37; }
.badge.info { background: #eef2f7; color: #4a5f7a; }
.empty { padding: 34px 0; text-align: center; color: var(--ink-400); font-size: 12px; }
.empty strong { display: block; color: var(--ink-700); font-size: 13px; margin-bottom: 4px; }
.audit-detail { display: grid; grid-template-columns: 1fr 1fr; gap: 12px 18px; }
.detail-item { display: flex; flex-direction: column; gap: 2px; }
.detail-item.full { grid-column: 1 / -1; }
.detail-item span { font-size: 11.5px; color: var(--ink-400); }
.detail-item strong { font-size: 13px; font-weight: 550; }
.detail-item pre { margin: 0; padding: 12px; background: var(--surface-sunken); border-radius: var(--radius-md); font-size: 11.5px; line-height: 1.6; max-height: 320px; overflow: auto; white-space: pre-wrap; word-break: break-all; }
</style>
