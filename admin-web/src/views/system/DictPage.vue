<template>
  <div class="page">
    <div class="page-title">
      <h2>数据字典</h2>
      <span class="hint">中文名称是页面唯一展示值，编码仅供接口传参</span>
    </div>

    <div class="split">
      <div class="card left">
        <div class="toolbar" style="justify-content: space-between">
          <h3 class="card-title" style="margin: 0">字典分类</h3>
          <el-button size="small" type="primary" @click="openCategory()">新增分类</el-button>
        </div>
        <el-input v-model="keyword" placeholder="搜索分类" size="small" style="margin: 12px 0" clearable />
        <ul class="cat-list">
          <li
            v-for="cat in filteredCategories"
            :key="cat.id"
            :class="{ active: cat.category === currentCategory }"
            @click="selectCategory(cat)"
          >
            <span>{{ cat.name }}</span>
            <span class="muted mono">{{ cat.category }}</span>
          </li>
        </ul>
      </div>

      <div class="card right">
        <div class="toolbar" style="justify-content: space-between">
          <h3 class="card-title" style="margin: 0">
            {{ currentCategoryName }} · 字典项
          </h3>
          <div class="toolbar">
            <el-button size="small" @click="openCategory(currentCategoryObj)">编辑分类</el-button>
            <el-button size="small" type="danger" plain @click="removeCategory">删除分类</el-button>
            <el-button size="small" type="primary" :disabled="!currentCategory" @click="openItem()">新增字典项</el-button>
          </div>
        </div>

        <el-table :data="items" size="small" style="margin-top: 12px" stripe>
          <el-table-column prop="itemName" label="中文名称" min-width="140" />
          <el-table-column prop="itemCode" label="编码（内部）" min-width="140">
            <template #default="{ row }"><span class="mono muted">{{ row.itemCode }}</span></template>
          </el-table-column>
          <el-table-column label="颜色" width="100">
            <template #default="{ row }">
              <span class="swatch" :style="{ background: row.color || '#d0d5dd' }" />
              <span class="muted">{{ row.color || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="sort" label="排序" width="80" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'" size="small">
                {{ row.status === 'ENABLED' ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140">
            <template #default="{ row }">
              <el-button link type="primary" @click="openItem(row)">编辑</el-button>
              <el-button link type="danger" @click="removeItem(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!items.length" description="该分类下暂无字典项" />
      </div>
    </div>

    <el-dialog v-model="categoryDialog" :title="categoryForm.id ? '编辑分类' : '新增分类'" width="420px">
      <el-form label-width="100px">
        <el-form-item label="分类编码" required>
          <el-input v-model="categoryForm.category" :disabled="!!categoryForm.id" placeholder="内部编码，如 room_state" />
        </el-form-item>
        <el-form-item label="中文名称" required>
          <el-input v-model="categoryForm.name" placeholder="页面展示名称" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="categoryForm.sort" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="categoryDialog = false">取消</el-button>
        <el-button type="primary" @click="submitCategory">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="itemDialog" :title="itemForm.id ? '编辑字典项' : '新增字典项'" width="460px">
      <el-form label-width="100px">
        <el-form-item label="中文名称" required>
          <el-input v-model="itemForm.name" placeholder="如：待清洁" />
        </el-form-item>
        <el-form-item label="编码" required>
          <el-input v-model="itemForm.code" :disabled="!!itemForm.id" placeholder="内部编码，如 dirty" />
        </el-form-item>
        <el-form-item label="颜色标记">
          <el-color-picker v-model="itemForm.color" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="itemForm.sort" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="itemForm.status">
            <el-radio value="ENABLED">启用</el-radio>
            <el-radio value="DISABLED">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="itemDialog = false">取消</el-button>
        <el-button type="primary" @click="submitItem">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createDictCategory,
  createDictItem,
  deleteDictCategory,
  deleteDictItem,
  listDictCategories,
  listDictItems,
  updateDictCategory,
  updateDictItem
} from '../../api/modules'

const categories = ref<any[]>([])
const items = ref<any[]>([])
const currentCategory = ref('')
const keyword = ref('')
const categoryDialog = ref(false)
const itemDialog = ref(false)
const categoryForm = reactive<any>({ id: null, category: '', name: '', sort: 0 })
const itemForm = reactive<any>({ id: null, code: '', name: '', color: '', sort: 0, status: 'ENABLED' })

const filteredCategories = computed(() =>
  categories.value.filter(
    (c) => !keyword.value || c.name.includes(keyword.value) || c.category.includes(keyword.value)
  )
)
const currentCategoryObj = computed(() => categories.value.find((c) => c.category === currentCategory.value))
const currentCategoryName = computed(() => currentCategoryObj.value?.name || '未选择分类')

const loadCategories = async () => {
  const res: any = await listDictCategories()
  categories.value = res.data
  if (!currentCategory.value && categories.value.length) {
    currentCategory.value = categories.value[0].category
  }
  await loadItems()
}

const loadItems = async () => {
  if (!currentCategory.value) return
  const res: any = await listDictItems(currentCategory.value)
  items.value = res.data
}

const selectCategory = async (cat: any) => {
  currentCategory.value = cat.category
  await loadItems()
}

const openCategory = (cat?: any) => {
  Object.assign(categoryForm, cat ? { ...cat } : { id: null, category: '', name: '', sort: 0 })
  categoryDialog.value = true
}

const submitCategory = async () => {
  if (!categoryForm.category || !categoryForm.name) {
    ElMessage.warning('分类编码与中文名称必填')
    return
  }
  const payload = {
    category: categoryForm.category,
    name: categoryForm.name,
    sort: categoryForm.sort,
    status: categoryForm.status || 'ENABLED'
  }
  if (categoryForm.id) {
    await updateDictCategory(categoryForm.id, payload)
  } else {
    await createDictCategory(payload)
  }
  categoryDialog.value = false
  await loadCategories()
  ElMessage.success('已保存')
}

const removeCategory = async () => {
  if (!categoryFormSafe()) return
  await ElMessageBox.confirm('删除后该分类不可恢复，确认删除？', '二次确认', { type: 'warning' })
  await deleteDictCategory(currentCategoryObj.value.id)
  currentCategory.value = ''
  await loadCategories()
}

const categoryFormSafe = () => !!currentCategoryObj.value

const openItem = (row?: any) => {
  Object.assign(itemForm, row
    ? { id: row.id, code: row.itemCode, name: row.itemName, color: row.color, sort: row.sort, status: row.status }
    : { id: null, code: '', name: '', color: '', sort: items.value.length, status: 'ENABLED' })
  itemDialog.value = true
}

const submitItem = async () => {
  if (!itemForm.code || !itemForm.name) {
    ElMessage.warning('中文名称与编码必填')
    return
  }
  const payload = {
    code: itemForm.code,
    name: itemForm.name,
    color: itemForm.color,
    sort: itemForm.sort,
    status: itemForm.status
  }
  if (itemForm.id) {
    await updateDictItem(itemForm.id, payload)
  } else {
    await createDictItem(currentCategory.value, payload)
  }
  itemDialog.value = false
  await loadItems()
  ElMessage.success('已保存')
}

const removeItem = async (row: any) => {
  await ElMessageBox.confirm(`确认删除字典项「${row.itemName}」？`, '二次确认', { type: 'warning' })
  await deleteDictItem(row.id)
  await loadItems()
}

onMounted(loadCategories)
</script>

<style scoped>
.split { display: grid; grid-template-columns: 280px 1fr; gap: var(--space-4); align-items: start; }
.cat-list { list-style: none; margin: 0; padding: 0; max-height: 520px; overflow: auto; }
.cat-list li {
  display: flex; justify-content: space-between; gap: var(--space-2);
  padding: 10px 12px; border-radius: var(--radius-md); cursor: pointer; font-size: var(--font-sm);
}
.cat-list li:hover { background: var(--color-bg); }
.cat-list li.active { background: var(--color-primary-soft); color: var(--color-primary); font-weight: 600; }
.swatch { display: inline-block; width: 10px; height: 10px; border-radius: 50%; margin-right: 6px; }
@media (max-width: 900px) { .split { grid-template-columns: 1fr; } }
</style>
