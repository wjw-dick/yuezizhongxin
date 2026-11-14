<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>角色与权限</h1>
        <p class="sub">角色 → 菜单/按钮授权；审计日志对所有角色只读，无写入口</p>
      </div>
      <div class="actions">
        <el-button type="primary" @click="openRole()">
          <Icon name="plus" :size="15" /><span style="margin-left: 6px">新增角色</span>
        </el-button>
      </div>
    </div>

    <div class="role-layout">
      <aside class="role-list card flush">
        <button
          v-for="item in roles"
          :key="item.role.id"
          class="role-item"
          :class="{ active: current?.role?.id === item.role.id }"
          @click="pick(item)"
        >
          <div>
            <strong>{{ item.role.roleName }}</strong>
            <em class="mono">{{ item.role.roleCode }}</em>
          </div>
          <span class="count">{{ item.menuIds?.length || 0 }}</span>
        </button>
        <p v-if="!roles.length" class="empty">暂无角色</p>
      </aside>

      <section class="perm-panel card">
        <div v-if="current" class="perm-head">
          <div>
            <h3>{{ current.role.roleName }}</h3>
            <p class="muted">{{ current.role.remark || '未填写角色说明' }}</p>
          </div>
          <div class="perm-actions">
            <el-button size="small" @click="checkAll(true)">全选</el-button>
            <el-button size="small" @click="checkAll(false)">清空</el-button>
            <el-button size="small" type="primary" :loading="saving" @click="saveMenus">保存授权</el-button>
          </div>
        </div>

        <div v-if="current" class="perm-groups">
          <div v-for="group in menuTree" :key="group.id" class="perm-group">
            <label class="group-head">
              <el-checkbox
                :model-value="isChecked(group.id)"
                :indeterminate="isIndeterminate(group)"
                @change="(v: boolean) => toggleGroup(group, v)"
              />
              <Icon :name="group.icon || 'grid'" :size="15" />
              <strong>{{ group.title }}</strong>
              <span class="muted">{{ (group.children || []).length }} 项</span>
            </label>
            <div class="group-children">
              <label v-for="child in group.children || []" :key="child.id" class="child-item">
                <el-checkbox :model-value="checkedIds.includes(child.id)" @change="(v: boolean) => toggleChild(child.id, v)" />
                <span>{{ child.title }}</span>
                <em v-if="child.perm" class="perm-tag mono">{{ child.perm }}</em>
              </label>
              <p v-if="!(group.children || []).length" class="muted">该组暂无子项</p>
            </div>
          </div>
          <p v-if="!menuTree.length" class="empty">还没有菜单数据</p>
        </div>
        <p v-else class="empty">请选择左侧角色</p>
      </section>
    </div>

    <el-dialog v-model="roleDialog" :title="roleForm.id ? '编辑角色' : '新增角色'" width="480px">
      <el-form :model="roleForm" label-position="top">
        <el-form-item label="角色名称" required><el-input v-model="roleForm.roleName" /></el-form-item>
        <el-form-item label="角色编码"><el-input v-model="roleForm.roleCode" placeholder="留空自动生成" /></el-form-item>
        <el-form-item label="角色说明"><el-input v-model="roleForm.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRole">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import Icon from '../../components/Icon.vue'
import { tenantHttp } from '../../api/client'

const roles = ref<any[]>([])
const menuTree = ref<any[]>([])
const current = ref<any>(null)
const checkedIds = ref<number[]>([])
const saving = ref(false)

async function loadRoles() {
  const res: any = await tenantHttp.get('/api/tenant/roles')
  roles.value = res.data || []
  if (roles.value.length && !current.value) {
    pick(roles.value[0])
  }
}

async function loadTree() {
  const res: any = await tenantHttp.get('/api/tenant/menus/tree')
  menuTree.value = res.data || []
}

function pick(item: any) {
  current.value = item
  checkedIds.value = [...(item.menuIds || [])]
}

function isChecked(id: number) {
  return checkedIds.value.includes(id)
}

function isIndeterminate(group: any) {
  const children = group.children || []
  const hit = children.filter((c: any) => checkedIds.value.includes(c.id)).length
  return hit > 0 && hit < children.length
}

function toggleChild(id: number, value: boolean) {
  if (value) {
    if (!checkedIds.value.includes(id)) checkedIds.value.push(id)
  } else {
    checkedIds.value = checkedIds.value.filter((x) => x !== id)
  }
}

function toggleGroup(group: any, value: boolean) {
  const children = (group.children || []).map((c: any) => c.id)
  if (value) {
    if (!checkedIds.value.includes(group.id)) checkedIds.value.push(group.id)
    children.forEach((id: number) => {
      if (!checkedIds.value.includes(id)) checkedIds.value.push(id)
    })
  } else {
    checkedIds.value = checkedIds.value.filter((id) => id !== group.id && !children.includes(id))
  }
}

function checkAll(value: boolean) {
  if (!value) {
    checkedIds.value = []
    return
  }
  const all: number[] = []
  menuTree.value.forEach((group: any) => {
    all.push(group.id)
    ;(group.children || []).forEach((c: any) => all.push(c.id))
  })
  checkedIds.value = all
}

async function saveMenus() {
  if (!current.value) return
  saving.value = true
  try {
    await tenantHttp.put(`/api/tenant/roles/${current.value.role.id}/menus`, checkedIds.value)
    ElMessage.success('授权已保存，重新登录后生效')
    await loadRoles()
    const found = roles.value.find((r) => r.role.id === current.value.role.id)
    if (found) pick(found)
  } finally {
    saving.value = false
  }
}

const roleDialog = ref(false)
const roleForm = reactive<any>({})

function openRole() {
  Object.keys(roleForm).forEach((k) => delete roleForm[k])
  roleDialog.value = true
}

async function saveRole() {
  if (!roleForm.roleName) {
    ElMessage.warning('请填写角色名称')
    return
  }
  saving.value = true
  try {
    await tenantHttp.post('/api/tenant/roles', { ...roleForm })
    ElMessage.success('角色已保存')
    roleDialog.value = false
    current.value = null
    loadRoles()
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await loadTree()
  await loadRoles()
})
</script>

<style scoped>
.role-layout { display: grid; grid-template-columns: 280px 1fr; gap: var(--space-4); align-items: start; }
.role-list { padding: 8px; display: flex; flex-direction: column; gap: 4px; }
.role-item {
  display: flex; align-items: center; justify-content: space-between; gap: 10px;
  border: 0; background: transparent; border-radius: var(--radius-md);
  padding: 10px 12px; cursor: pointer; text-align: left; transition: background 0.15s ease;
}
.role-item:hover { background: var(--surface-sunken); }
.role-item.active { background: var(--brand-50); }
.role-item div { display: flex; flex-direction: column; line-height: 1.3; }
.role-item strong { font-size: 13.5px; color: var(--ink-900); }
.role-item em { font-style: normal; font-size: 11px; color: var(--ink-400); }
.role-item.active strong { color: var(--brand-700); }
.count { font-size: 11px; color: var(--ink-500); background: var(--surface); border: 1px solid var(--line); border-radius: 999px; padding: 0 7px; }
.perm-panel { min-height: 420px; }
.perm-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; padding-bottom: 14px; border-bottom: 1px solid var(--line); }
.perm-head h3 { margin: 0; font-size: 16px; }
.perm-head p { margin: 4px 0 0; }
.perm-actions { display: flex; gap: 8px; }
.perm-groups { display: flex; flex-direction: column; gap: 14px; padding-top: 16px; }
.perm-group { border: 1px solid var(--line); border-radius: var(--radius-md); overflow: hidden; }
.group-head { display: flex; align-items: center; gap: 9px; padding: 10px 14px; background: var(--surface-sunken); cursor: pointer; }
.group-head strong { font-size: 13.5px; }
.group-head .muted { margin-left: auto; font-size: 11.5px; }
.group-children { display: flex; flex-wrap: wrap; gap: 6px 18px; padding: 12px 14px; }
.child-item { display: inline-flex; align-items: center; gap: 7px; font-size: 12.5px; color: var(--ink-700); cursor: pointer; }
.perm-tag { font-style: normal; font-size: 10.5px; color: var(--ink-400); background: var(--surface-sunken); border-radius: 4px; padding: 1px 6px; }
.empty { padding: 40px 0; text-align: center; color: var(--ink-400); font-size: 12.5px; }
</style>
