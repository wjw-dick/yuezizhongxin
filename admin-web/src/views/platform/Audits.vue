<template>
  <div class="page">
    <header class="page-head">
      <div>
        <h1>平台审计</h1>
        <p class="sub">平台侧的登录、开通、停用、改价、生成账单等操作全部留痕，只读不可删。</p>
      </div>
      <div class="actions">
        <el-button @click="load">
          <Icon name="refresh" :size="15" style="margin-right: 5px" />刷新
        </el-button>
      </div>
    </header>

    <section class="card filters">
      <el-select v-model="query.module" placeholder="全部模块" clearable style="width: 160px" @change="search">
        <el-option v-for="m in modules" :key="m" :label="m" :value="m" />
      </el-select>
      <span class="spacer" />
      <span class="muted">共 {{ total }} 条</span>
    </section>

    <section class="card flush">
      <el-table v-loading="loading" :data="rows" style="width: 100%" empty-text="暂无审计记录">
        <el-table-column label="时间" width="170">
          <template #default="{ row }"><span class="muted">{{ fmtDateTime(row.createdTime) }}</span></template>
        </el-table-column>
        <el-table-column prop="module" label="模块" width="130" />
        <el-table-column prop="action" label="动作" min-width="140" />
        <el-table-column label="对象" min-width="150">
          <template #default="{ row }">
            <span class="mono">{{ row.target || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="租户" width="120">
          <template #default="{ row }">
            <span class="mono">{{ row.tenantCode || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作人" width="130">
          <template #default="{ row }">
            {{ row.operatorName || '系统' }}
            <span class="role">{{ row.platformRole || '' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="结果" width="90">
          <template #default="{ row }">
            <span class="pill plain" :class="row.result === '成功' ? 'ok' : 'danger'">{{ row.result }}</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <span class="muted">保留期 ≥180 天</span>
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
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import Icon from '../../components/Icon.vue'
import { listAudits } from '../../api/platform'
import { fmtDateTime } from '../../utils/format'

const loading = ref(false)
const rows = ref<any[]>([])
const total = ref(0)
const modules = ['租户管理', '套餐与配额', '账单与计费', '平台认证']

const query = reactive({ module: '', pageNum: 1, pageSize: 15 })

const load = async () => {
  loading.value = true
  try {
    const params: Record<string, unknown> = { pageNum: query.pageNum, pageSize: query.pageSize }
    if (query.module) params.module = query.module
    const res: any = await listAudits(params)
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

onMounted(load)
</script>

<style scoped>
.filters { display: flex; align-items: center; gap: 12px; padding: 14px 18px; }
.filters .spacer { flex: 1; }
.pager { display: flex; align-items: center; justify-content: space-between; padding: 12px 18px; border-top: 1px solid var(--line); }
.role { color: var(--ink-400); font-size: 11.5px; margin-left: 4px; }
</style>
