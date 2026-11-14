<template>
  <div class="page">
    <header class="page-head">
      <div>
        <h1>系统参数</h1>
        <p class="sub">按分组维护站点、门店与业务规则；保存即时生效，金额与比例变更需二次确认并写审计。</p>
      </div>
      <div class="actions">
        <el-button :loading="saving" @click="load">重置</el-button>
        <el-button type="primary" :loading="saving" @click="save(false)">保存本组</el-button>
      </div>
    </header>

    <div class="layout">
      <aside class="group-rail">
        <p class="rail-label">参数分组</p>
        <button
          v-for="g in groups"
          :key="g.group"
          class="group-item"
          :class="{ active: g.group === activeGroup }"
          @click="select(g.group)"
        >
          <span>{{ g.group }}</span>
          <em>{{ g.items.length }}</em>
        </button>
        <p v-if="!groups.length" class="muted rail-empty">暂无参数分组</p>
      </aside>

      <section class="card panel">
        <header class="panel-head">
          <div>
            <h2>{{ activeGroup || '参数' }}</h2>
            <p>{{ current?.items.length || 0 }} 项配置 · 保存后对所有端立即生效</p>
          </div>
          <span class="pill plain muted">修改留痕</span>
        </header>

        <div class="rows">
          <div v-for="item in current?.items || []" :key="item.key" class="row">
            <div class="row-label">
              <strong>{{ item.remark || item.key }}</strong>
              <span class="mono">{{ item.key }}</span>
            </div>
            <div class="row-control">
              <el-switch v-if="item.valueType === 'boolean'" v-model="model[item.key]" />
              <el-input-number
                v-else-if="item.valueType === 'number'"
                v-model="model[item.key]"
                :controls="false"
                class="num-input"
              />
              <el-input v-else v-model="model[item.key]" class="text-input" />
            </div>
          </div>

          <div v-if="!current?.items.length" class="empty">
            <strong>该分组暂无参数</strong>
            可在数据库预置或后续版本补充。
          </div>
        </div>

        <footer class="panel-foot">
          <span class="muted">变更会写入审计日志，包含修改前后的值。</span>
          <el-button type="primary" :loading="saving" @click="save(false)">保存本组</el-button>
        </footer>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listConfigs, saveConfigGroup } from '../../api/modules'

const groups = ref<any[]>([])
const activeGroup = ref('')
const model = reactive<Record<string, any>>({})
const saving = ref(false)

const current = computed(() => groups.value.find((g) => g.group === activeGroup.value))

const fillModel = () => {
  Object.keys(model).forEach((key) => delete model[key])
  for (const item of current.value?.items || []) {
    model[item.key] =
      item.valueType === 'number'
        ? Number(item.value || 0)
        : item.valueType === 'boolean'
          ? item.value === 'true'
          : item.value
  }
}

const load = async () => {
  const res: any = await listConfigs()
  groups.value = res.data || []
  if (!groups.value.find((g) => g.group === activeGroup.value)) {
    activeGroup.value = groups.value[0]?.group || ''
  }
  fillModel()
}

const select = (group: string) => {
  activeGroup.value = group
  fillModel()
}

const save = async (confirm: boolean) => {
  const group = current.value
  if (!group) return
  const items = group.items.map((item: any) => {
    const raw = model[item.key]
    return {
      key: item.key,
      value: typeof raw === 'boolean' ? String(raw) : raw === null || raw === undefined ? '' : String(raw),
      valueType: item.valueType,
      remark: item.remark
    }
  })
  saving.value = true
  try {
    await saveConfigGroup({ group: activeGroup.value, items, confirm })
    ElMessage.success('已保存并即时生效')
    await load()
  } catch (e: any) {
    if (e?.code === 40900) {
      await ElMessageBox.confirm(e.msg, '需要二次确认', {
        confirmButtonText: '确认变更',
        cancelButtonText: '取消',
        type: 'warning'
      })
      await save(true)
    }
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.layout { display: grid; grid-template-columns: 208px minmax(0, 1fr); gap: 14px; align-items: start; }
@media (max-width: 900px) { .layout { grid-template-columns: 1fr; } }

.group-rail {
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  padding: 14px 10px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  position: sticky;
  top: 16px;
}
.rail-label { margin: 2px 8px 8px; font-size: 11px; letter-spacing: 0.08em; color: var(--ink-400); text-transform: uppercase; }
.group-item {
  display: flex; align-items: center; justify-content: space-between;
  border: 0; background: transparent; cursor: pointer;
  padding: 8px 10px; border-radius: 8px;
  font-size: 13px; color: var(--ink-700); text-align: left;
}
.group-item:hover { background: var(--surface-sunken); }
.group-item.active { background: var(--brand-50); color: var(--brand-700); font-weight: 550; }
.group-item em { font-style: normal; font-size: 11px; color: var(--ink-400); }
.group-item.active em { color: var(--brand-600); }
.rail-empty { margin: 6px 10px; }

.panel { padding: 0; }
.panel-head {
  display: flex; align-items: flex-start; justify-content: space-between;
  padding: 18px 22px 14px; border-bottom: 1px solid var(--line);
}
.panel-head h2 { margin: 0; font-size: 16px; font-weight: 600; }
.panel-head p { margin: 4px 0 0; font-size: 12px; color: var(--ink-400); }

.rows { padding: 4px 22px; }
.row {
  display: flex; align-items: center; justify-content: space-between; gap: 24px;
  padding: 14px 0;
  border-bottom: 1px solid #f2eee6;
}
.row:last-child { border-bottom: 0; }
.row-label { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.row-label strong { font-size: 13.5px; font-weight: 500; color: var(--ink-900); }
.row-label span { font-size: 11px; color: var(--ink-400); }
.row-control { flex: 0 0 auto; }
.text-input { width: 320px; }
.num-input { width: 180px; }

.panel-foot {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 22px; border-top: 1px solid var(--line);
  background: var(--surface-sunken);
  border-radius: 0 0 var(--radius-lg) var(--radius-lg);
}
</style>
