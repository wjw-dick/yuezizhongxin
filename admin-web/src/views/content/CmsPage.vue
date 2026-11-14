<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>内容管理</h1>
        <p class="sub">发布前三道闸：适用端必选 → 案例须标记已获授权 → 敏感词检测；通过后官网、小程序、H5 同步可见</p>
      </div>
      <div class="actions">
        <el-button @click="loadAll"><Icon name="refresh" :size="15" /><span style="margin-left: 6px">刷新</span></el-button>
        <el-button type="primary" @click="openEditor()">
          <Icon name="plus" :size="15" /><span style="margin-left: 6px">新增内容</span>
        </el-button>
      </div>
    </div>

    <div class="type-strip">
      <button
        v-for="t in typeTabs"
        :key="t.value"
        class="type-chip"
        :class="{ active: contentType === t.value }"
        @click="contentType = t.value; loadList()"
      >
        {{ t.label }}
      </button>
    </div>

    <div class="card flush">
      <div class="toolbar filter-bar">
        <el-input v-model="keyword" placeholder="标题关键词" clearable size="small" style="width: 200px" @keyup.enter="loadList" @clear="loadList" />
        <el-select v-model="status" placeholder="全部状态" clearable size="small" style="width: 130px" @change="loadList">
          <el-option label="草稿" value="草稿" />
          <el-option label="已发布" value="已发布" />
          <el-option label="已下线" value="已下线" />
        </el-select>
        <el-button size="small" @click="loadList">查询</el-button>
        <span class="spacer" />
        <span class="muted">共 {{ list.length }} 条</span>
      </div>

      <el-table v-loading="loading" :data="list" size="small" stripe>
        <el-table-column label="封面" width="92">
          <template #default="{ row }">
            <div v-if="row.cover" class="thumb"><img :src="row.cover" alt="" /></div>
            <div v-else class="thumb empty-thumb"><Icon name="inbox" :size="16" /></div>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="220" />
        <el-table-column label="类型" width="120">
          <template #default="{ row }">{{ typeLabel(row.contentType) }}</template>
        </el-table-column>
        <el-table-column label="适用端" width="180">
          <template #default="{ row }">
            <span v-for="end in endsOf(row.applyEnds)" :key="end" class="end-chip">{{ end }}</span>
            <span v-if="!row.applyEnds" class="muted">未设置</span>
          </template>
        </el-table-column>
        <el-table-column label="授权" width="90">
          <template #default="{ row }">
            <span v-if="row.contentType === '案例'" class="badge" :class="row.authorized ? 'ok' : 'danger'">
              {{ row.authorized ? '已授权' : '未授权' }}
            </span>
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <span class="badge" :class="row.status === '已发布' ? 'ok' : row.status === '草稿' ? 'muted' : 'warn'">{{ row.status }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="updatedTime" label="更新时间" min-width="150" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditor(row)">编辑</el-button>
            <el-button v-if="row.status !== '已发布'" link type="primary" @click="publish(row)">发布</el-button>
            <el-button v-else link type="danger" @click="offline(row)">下线</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div class="empty"><strong>还没有内容</strong>先新建 Banner 或品牌故事，官网首页会自动聚合</div>
        </template>
      </el-table>
    </div>

    <el-drawer v-model="editorDrawer" :title="form.id ? '编辑内容' : '新增内容'" size="760px">
      <el-form :model="form" label-position="top" class="editor-form">
        <el-form-item label="内容类型" required>
          <el-select v-model="form.contentType" style="width: 100%">
            <el-option v-for="t in typeTabs" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="中文标题（多语言在下方单独维护）" />
        </el-form-item>
        <el-form-item label="封面图">
          <ImageUploader v-model="form.cover" dir="cms" :limit="1" />
        </el-form-item>
        <el-form-item label="正文（富文本）">
          <RichEditor v-model="form.bodyHtml" :min-height="280" />
        </el-form-item>
        <el-form-item label="适用端" required>
          <el-checkbox-group v-model="endList">
            <el-checkbox label="web">PC 官网</el-checkbox>
            <el-checkbox label="mp">小程序</el-checkbox>
            <el-checkbox label="h5">H5 分享页</el-checkbox>
            <el-checkbox label="app">App</el-checkbox>
          </el-checkbox-group>
          <p class="hint">至少选择一个适用端，否则发布会被拦截</p>
        </el-form-item>
        <el-form-item v-if="form.contentType === '案例'" label="客户授权">
          <el-switch v-model="form.authorized" :active-value="1" :inactive-value="0" active-text="已取得客户书面授权" />
        </el-form-item>

        <div class="seo-block">
          <p class="section-label">SEO 与结构化数据</p>
          <el-form-item label="SEO 标题"><el-input v-model="form.seoTitle" :placeholder="form.title" /></el-form-item>
          <el-form-item label="关键词"><el-input v-model="form.seoKeywords" placeholder="月子中心,产后康复,月子餐" /></el-form-item>
          <el-form-item label="描述"><el-input v-model="form.seoDescription" type="textarea" :rows="2" /></el-form-item>
        </div>

        <div class="i18n-block">
          <p class="section-label">多语言内容</p>
          <el-tabs v-model="activeLang" type="card" class="lang-tabs">
            <el-tab-pane v-for="lang in languages" :key="lang.code" :label="lang.label" :name="lang.code">
              <el-input v-model="i18nForm[lang.code].title" placeholder="该语言的标题" style="margin-bottom: 8px" />
              <el-input v-model="i18nForm[lang.code].bodyHtml" type="textarea" :rows="3" placeholder="该语言的正文（纯文本或 HTML）" />
              <el-checkbox v-model="i18nForm[lang.code].translated" :true-value="1" :false-value="0" style="margin-top: 8px">
                已完成人工翻译
              </el-checkbox>
            </el-tab-pane>
          </el-tabs>
        </div>
      </el-form>

      <template #footer>
        <div class="drawer-footer">
          <el-button @click="editorDrawer = false">取消</el-button>
          <el-button :loading="saving" @click="save(false)">保存草稿</el-button>
          <el-button type="primary" :loading="saving" @click="save(true)">保存并发布</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import Icon from '../../components/Icon.vue'
import ImageUploader from '../../components/ImageUploader.vue'
import RichEditor from '../../components/RichEditor.vue'
import { tenantHttp } from '../../api/client'

const loading = ref(false)
const saving = ref(false)
const list = ref<any[]>([])
const contentType = ref('')
const status = ref('')
const keyword = ref('')

const typeTabs = [
  { label: '全部', value: '' },
  { label: 'Banner', value: 'Banner' },
  { label: '品牌故事', value: '品牌故事' },
  { label: '房型展示', value: '房型展示' },
  { label: '康复项目', value: '康复项目' },
  { label: '月子餐', value: '月子餐' },
  { label: '团队', value: '团队' },
  { label: '案例', value: '案例' },
  { label: '常见问题', value: '常见问题' },
  { label: '资讯', value: '资讯' }
]

const languages = [
  { code: 'zh-CN', label: '简体中文' },
  { code: 'en-US', label: 'English' }
]

const activeLang = ref('zh-CN')
const endList = ref<string[]>([])
const i18nForm = reactive<Record<string, any>>({
  'zh-CN': { title: '', bodyHtml: '', translated: 1 },
  'en-US': { title: '', bodyHtml: '', translated: 0 }
})

function typeLabel(value: string) {
  return typeTabs.find((t) => t.value === value)?.label || value || '-'
}

function endsOf(value: string) {
  if (!value) return []
  return value
    .split(',')
    .filter(Boolean)
    .map((v) => ({ web: '官网', mp: '小程序', h5: 'H5', app: 'App' }[v] || v))
}

async function loadList() {
  loading.value = true
  try {
    const res: any = await tenantHttp.get('/api/tenant/cms', {
      params: {
        contentType: contentType.value || undefined,
        status: status.value || undefined,
        keyword: keyword.value || undefined
      }
    })
    list.value = res.data || []
  } finally {
    loading.value = false
  }
}

function loadAll() {
  loadList()
}

/* 编辑 */
const editorDrawer = ref(false)
const form = reactive<any>({})

async function openEditor(row?: any) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, {
    contentType: contentType.value || 'Banner',
    title: '',
    cover: '',
    bodyHtml: '',
    seoTitle: '',
    seoKeywords: '',
    seoDescription: '',
    authorized: 0,
    status: '草稿'
  })
  endList.value = ['web', 'mp']
  Object.keys(i18nForm).forEach((code) => {
    i18nForm[code] = { title: '', bodyHtml: '', translated: 0 }
  })

  if (row) {
    const res: any = await tenantHttp.get(`/api/tenant/cms/${row.id}`)
    const data = res.data || {}
    const content = data.content || data
    Object.assign(form, content)
    endList.value = String(content.applyEnds || '').split(',').filter(Boolean)
    ;(data.i18n || []).forEach((item: any) => {
      if (!i18nForm[item.langCode]) {
        i18nForm[item.langCode] = { title: '', bodyHtml: '', translated: 0 }
      }
      i18nForm[item.langCode] = {
        title: item.title || '',
        bodyHtml: item.bodyHtml || '',
        translated: item.translated || 0
      }
    })
  }
  editorDrawer.value = true
}

async function save(andPublish: boolean) {
  if (!form.title) {
    ElMessage.warning('请填写标题')
    return
  }
  if (!endList.value.length) {
    ElMessage.warning('请至少选择一个适用端')
    return
  }
  if (form.contentType === '案例' && !form.authorized) {
    ElMessage.warning('案例必须先标记「已取得客户书面授权」')
    return
  }
  saving.value = true
  try {
    const payload = { ...form, applyEnds: endList.value.join(',') }
    const res: any = await tenantHttp.post('/api/tenant/cms', payload)
    const id = res.data
    const rows = Object.entries(i18nForm)
      .filter(([, v]: any) => v && (v.title || v.bodyHtml))
      .map(([langCode, v]: any) => ({ langCode, title: v.title, bodyHtml: v.bodyHtml, translated: v.translated }))
    if (rows.length) {
      await tenantHttp.put(`/api/tenant/cms/${id}/i18n`, rows)
    }
    if (andPublish) {
      await tenantHttp.post(`/api/tenant/cms/${id}/publish`, { applyEnds: endList.value.join(',') })
    }
    ElMessage.success(andPublish ? '已发布' : '草稿已保存')
    editorDrawer.value = false
    loadList()
  } finally {
    saving.value = false
  }
}

async function publish(row: any) {
  try {
    await tenantHttp.post(`/api/tenant/cms/${row.id}/publish`, { applyEnds: row.applyEnds })
    ElMessage.success('已发布')
    loadList()
  } catch (e: any) {
    ElMessage.error(e?.msg || '发布失败，请检查适用端、授权与敏感词')
  }
}

async function offline(row: any) {
  await ElMessageBox.confirm('下线后官网与小程序将立即不可见，确认？', '下线内容', { type: 'warning' })
  await tenantHttp.post(`/api/tenant/cms/${row.id}/offline`)
  ElMessage.success('已下线')
  loadList()
}

onMounted(loadList)
</script>

<style scoped>
.type-strip { display: flex; flex-wrap: wrap; gap: 7px; }
.type-chip {
  border: 1px solid var(--line);
  background: var(--surface);
  color: var(--ink-600);
  border-radius: 999px;
  padding: 5px 13px;
  font-size: 12.5px;
  cursor: pointer;
  transition: all 0.15s ease;
}
.type-chip:hover { border-color: var(--line-strong); color: var(--ink-900); }
.type-chip.active { background: var(--brand-50); border-color: var(--brand-200); color: var(--brand-700); font-weight: 600; }

.filter-bar { padding: 14px var(--space-5); border-bottom: 1px solid var(--line); background: var(--surface-sunken); }
.thumb { width: 62px; height: 42px; border-radius: 6px; overflow: hidden; background: var(--surface-sunken); }
.thumb img { width: 100%; height: 100%; object-fit: cover; display: block; }
.empty-thumb { display: grid; place-items: center; color: var(--ink-300); }
.end-chip { display: inline-block; font-size: 10.5px; padding: 1px 6px; margin-right: 4px; border-radius: 4px; background: var(--surface-sunken); color: var(--ink-600); }
.badge { display: inline-flex; align-items: center; height: 22px; padding: 0 8px; border-radius: 999px; font-size: 12px; background: #eef2f7; color: #4a5f7a; }
.badge.ok { background: #eef7f1; color: #2f7d55; }
.badge.warn { background: #fdf5e8; color: #9a6a18; }
.badge.danger { background: #fbeeed; color: #a83c37; }
.badge.muted { background: #f4f3ef; color: var(--ink-500); }
.empty { padding: 34px 0; text-align: center; color: var(--ink-400); font-size: 12px; }
.empty strong { display: block; color: var(--ink-700); font-size: 13px; margin-bottom: 4px; }

.editor-form { display: flex; flex-direction: column; gap: 2px; }
.hint { margin: 4px 0 0; font-size: 11.5px; color: var(--ink-400); }
.seo-block, .i18n-block { border: 1px solid var(--line); border-radius: var(--radius-md); padding: 12px 14px; margin-top: 6px; }
.section-label { margin: 0 0 10px; font-size: 11.5px; letter-spacing: 0.08em; color: var(--ink-400); font-weight: 600; }
.lang-tabs :deep(.el-tabs__header) { margin-bottom: 10px; }
.drawer-footer { display: flex; justify-content: flex-end; gap: 10px; }
</style>
