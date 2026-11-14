<template>
  <div class="page">
    <div class="page-title">
      <h2>菜单可视化</h2>
      <span class="hint">三栏编辑：结构树 → 实时预览 → 属性面板；整树保存，层级上限由服务端校验</span>
    </div>

    <div class="toolbar">
      <el-radio-group v-model="endType" @change="load">
        <el-radio-button v-for="item in endTypes" :key="item" :value="item">{{ item }}</el-radio-button>
      </el-radio-group>
      <el-button size="small" @click="addRoot">新增一级</el-button>
      <el-button size="small" :disabled="!selected" @click="addChild">新增子级</el-button>
      <el-button size="small" :disabled="!selected" @click="removeNode">删除</el-button>
      <el-button size="small" type="primary" :loading="saving" @click="save">保存整树</el-button>
      <span class="muted">{{ ruleText }}</span>
    </div>

    <div class="designer">
      <div class="card column">
        <h3 class="card-title">结构树</h3>
        <el-tree
          ref="treeRef"
          :data="tree"
          node-key="_uid"
          :props="{ label: 'title', children: 'children' }"
          draggable
          default-expand-all
          :expand-on-click-node="false"
          highlight-current
          @node-click="(data:any) => (selected = data)"
          @node-drop="onDrop"
        >
          <template #default="{ data }">
            <span class="node-label">
              <span>{{ data.title }}</span>
              <span class="muted">{{ data.linkType }}</span>
            </span>
          </template>
        </el-tree>
        <el-empty v-if="!tree.length" description="暂无菜单，点击「新增一级」开始" />
      </div>

      <div class="card column preview">
        <h3 class="card-title">实时预览</h3>
        <div class="phone">
          <div class="phone-body" :class="{ wechat: endType === '公众号' }">
            <div v-if="endType === '公众号'" class="mp-header">{{ previewTitle }}</div>
            <div class="phone-content">
              <div class="placeholder">页面内容区</div>
            </div>
            <div v-if="endType === '公众号'" class="mp-tabbar">
              <div v-for="node in tree.slice(0, 3)" :key="node._uid" class="mp-tab">
                <div class="mp-tab-title">{{ node.title }}</div>
                <div v-if="node.children?.length" class="mp-sub">
                  <div v-for="child in node.children.slice(0, 5)" :key="child._uid">{{ child.title }}</div>
                </div>
              </div>
            </div>
            <div v-else class="app-tabbar">
              <div v-for="node in tree.slice(0, 5)" :key="node._uid" class="app-tab">
                <span class="app-icon" />
                <span>{{ node.title }}</span>
              </div>
            </div>
          </div>
        </div>
        <p class="muted">{{ previewHint }}</p>
      </div>

      <div class="card column">
        <h3 class="card-title">属性面板</h3>
        <el-empty v-if="!selected" description="选择左侧节点后编辑属性" />
        <el-form v-else label-position="top" size="small">
          <el-form-item label="标题（中文）" required>
            <el-input v-model="selected.title" maxlength="20" show-word-limit />
          </el-form-item>
          <el-form-item label="链接类型">
            <el-select v-model="selected.linkType" style="width: 100%">
              <el-option v-for="t in linkTypes" :key="t" :label="t" :value="t" />
            </el-select>
          </el-form-item>
          <el-form-item label="路径 / 链接">
            <el-input v-model="selected.path" placeholder="/pages/index/index" />
          </el-form-item>
          <el-form-item label="图标">
            <el-input v-model="selected.icon" placeholder="图标名称（可选）" />
          </el-form-item>
          <el-form-item label="可见">
            <el-switch v-model="selected.visible" :active-value="1" :inactive-value="0" />
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getNavTree, saveNavTree } from '../../api/modules'

const endTypes = ['公众号', '小程序tabBar', 'H5底部导航', 'PC官网', 'PC后台']
const linkTypes = ['站内页', '外链', '小程序页', '关键词回复']
const endType = ref('公众号')
const tree = ref<any[]>([])
const selected = ref<any>(null)
const saving = ref(false)
const dirty = ref(false)
const treeRef = ref()

const rules: Record<string, string> = {
  公众号: '一级 ≤3，二级 ≤5，最多两级',
  小程序tabBar: '2~5 项，仅一级',
  H5底部导航: '≤5 项，仅一级',
  PC官网: '最多三级，同级 ≤12',
  PC后台: '最多三级，同级 ≤12'
}
const ruleText = computed(() => rules[endType.value] || '')
const previewTitle = computed(() => tree.value[0]?.title || '菜单预览')
const previewHint = computed(() =>
  endType.value === '公众号' ? '一级菜单点击后展开二级菜单' : '底部导航按顺序展示前 5 项'
)

let seq = 0
// 新节点使用负数临时主键：服务端按层级顺序落库后再回填真实主键
const nextTempId = () => -(++seq)
const toInternal = (nodes: any[]): any[] =>
  (nodes || []).map((node) => ({
    ...node,
    visible: node.visible ?? 1,
    linkType: node.linkType || '站内页',
    _uid: `n${node.id}`,
    children: toInternal(node.children)
  }))

const load = async () => {
  const res: any = await getNavTree(endType.value)
  tree.value = toInternal(res.data)
  selected.value = null
  dirty.value = false
}

const addRoot = () => {
  const id = nextTempId()
  tree.value.push({
    id, _uid: `n${id}`, title: '新菜单', linkType: '站内页', path: '', icon: '', visible: 1, children: []
  })
  dirty.value = true
}

const addChild = () => {
  if (!selected.value) return
  if (endType.value === '小程序tabBar' || endType.value === 'H5底部导航') {
    ElMessage.warning('该端只支持一级菜单')
    return
  }
  const id = nextTempId()
  selected.value.children = [
    ...(selected.value.children || []),
    { id, _uid: `n${id}`, title: '新子菜单', linkType: '站内页', path: '', icon: '', visible: 1, children: [] }
  ]
  dirty.value = true
}

const removeNode = () => {
  if (!selected.value) return
  const walk = (nodes: any[]): boolean => {
    const index = nodes.findIndex((n) => n._uid === selected.value._uid)
    if (index > -1) {
      nodes.splice(index, 1)
      return true
    }
    return nodes.some((n) => n.children && walk(n.children))
  }
  walk(tree.value)
  selected.value = null
  dirty.value = true
}

const onDrop = () => {
  dirty.value = true
}

const flatten = (nodes: any[], parentId: number, out: any[] = []) => {
  nodes.forEach((node, index) => {
    const id = node.id
    out.push({
      id,
      parentId,
      title: node.title,
      linkType: node.linkType,
      path: node.path,
      icon: node.icon,
      sort: index,
      visible: node.visible
    })
    if (node.children?.length) {
      flatten(node.children, id, out)
    }
  })
  return out
}

const save = async () => {
  saving.value = true
  try {
    const res: any = await saveNavTree({ endType: endType.value, nodes: flatten(tree.value, 0) })
    tree.value = toInternal(res.data)
    selected.value = null
    dirty.value = false
    ElMessage.success('菜单已保存并生效')
  } finally {
    saving.value = false
  }
}

onBeforeRouteLeave(async () => {
  if (!dirty.value) return true
  try {
    await ElMessageBox.confirm('菜单有未保存的改动，离开将丢失，确认离开？', '未保存提醒', {
      type: 'warning', confirmButtonText: '离开', cancelButtonText: '继续编辑'
    })
    return true
  } catch {
    return false
  }
})

onMounted(load)
</script>

<style scoped>
.designer { display: grid; grid-template-columns: 1fr 360px 320px; gap: var(--space-4); align-items: start; }
.column { min-height: 520px; }
.node-label { display: flex; gap: var(--space-2); align-items: center; width: 100%; justify-content: space-between; }
.preview { display: flex; flex-direction: column; align-items: center; }
.phone { width: 300px; height: 460px; border: 10px solid #1f2937; border-radius: 28px; background: #fff; overflow: hidden; }
.phone-body { display: flex; flex-direction: column; height: 100%; }
.mp-header { padding: 8px; text-align: center; font-size: 12px; background: #f2f2f2; color: #333; }
.phone-content { flex: 1; display: grid; place-items: center; background: #fafafa; }
.placeholder { color: #9aa4b2; font-size: 12px; }
.mp-tabbar { display: flex; border-top: 1px solid #e5e5e5; background: #fff; }
.mp-tab { flex: 1; text-align: center; font-size: 12px; padding: 6px 2px; position: relative; color: #333; }
.mp-sub { position: absolute; bottom: 100%; left: 0; right: 0; background: #fff; border: 1px solid #eee; display: none; }
.mp-tab:hover .mp-sub { display: block; }
.mp-sub div { padding: 6px; border-bottom: 1px solid #f2f2f2; }
.app-tabbar { display: flex; border-top: 1px solid #e5e5e5; background: #fff; }
.app-tab { flex: 1; text-align: center; font-size: 11px; padding: 6px 0; color: #444; display: flex; flex-direction: column; gap: 2px; }
.app-icon { width: 16px; height: 16px; border-radius: 4px; background: #dfe4ec; margin: 0 auto; }
@media (max-width: 1200px) { .designer { grid-template-columns: 1fr; } }
</style>
