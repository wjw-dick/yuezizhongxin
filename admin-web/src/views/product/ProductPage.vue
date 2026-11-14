<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>套餐与商品</h1>
        <p class="sub">套餐决定订单计价与定金比例；上下架只影响新订单，不影响已签约客户</p>
      </div>
    </div>

    <el-tabs v-model="tab" class="biz-tabs">
      <el-tab-pane label="套餐" name="package">
        <CrudPage
          title="套餐"
          subtitle="支持 28 天 / 42 天与自定义天数，富文本详情用于官网与小程序"
          base="/api/tenant/catalog/packages"
          entity="套餐"
          create-text="新增套餐"
          :columns="pkgColumns"
          :fields="pkgFields"
          :filters="pkgFilters"
        >
          <template #row-actions="{ row, reload }">
            <el-button link type="primary" @click.stop="edit(row, reload)">编辑</el-button>
            <el-button link type="warning" @click.stop="togglePkg(row, reload)">
              {{ row.status === 'ON_SHELF' ? '下架' : '上架' }}
            </el-button>
            <el-button link type="danger" @click.stop="removePkg(row, reload)">删除</el-button>
          </template>
        </CrudPage>
      </el-tab-pane>

      <el-tab-pane label="商品" name="product">
        <CrudPage
          title="商品"
          subtitle="商品库存在下单时按条件更新扣减，避免并发超卖"
          base="/api/tenant/catalog/products"
          entity="商品"
          create-text="新增商品"
          :columns="productColumns"
          :fields="productFields"
          :filters="productFilters"
        >
          <template #row-actions="{ row, reload }">
            <el-button link type="primary" @click.stop="editProduct(row, reload)">编辑</el-button>
            <el-button link type="warning" @click.stop="toggleProduct(row, reload)">
              {{ row.status === 'ON_SHELF' ? '下架' : '上架' }}
            </el-button>
          </template>
        </CrudPage>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import CrudPage from '../../components/CrudPage.vue'
import { tenantHttp } from '../../api/client'

const tab = ref('package')

const pkgColumns = [
  { prop: 'name', label: '套餐名称', min: 160 },
  { prop: 'days', label: '天数', width: 80 },
  { prop: 'roomTypeLevel', label: '房型档位', min: 110 },
  { prop: 'careMode', label: '护理模式', min: 110 },
  { prop: 'price', label: '价格', width: 120, type: 'money' as const },
  { prop: 'depositRatio', label: '定金比例', width: 100 },
  { prop: 'status', label: '状态', width: 90, type: 'status' as const },
  { prop: 'sort', label: '排序', width: 70 }
]

const pkgFilters = [
  {
    key: 'status',
    label: '状态',
    type: 'select',
    options: [
      { label: '已上架', value: 'ON_SHELF' },
      { label: '已下架', value: 'OFF_SHELF' }
    ]
  },
  { key: 'keyword', label: '套餐名称' }
]

const pkgFields = [
  { key: 'name', label: '套餐名称', required: true, full: true },
  { key: 'days', label: '套餐天数', type: 'number', required: true, default: 28 },
  {
    key: 'roomTypeLevel',
    label: '适用房型档位',
    type: 'select',
    options: [
      { label: '温馨单间', value: '温馨单间' },
      { label: '豪华套间', value: '豪华套间' },
      { label: '尊享套房', value: '尊享套房' }
    ]
  },
  {
    key: 'careMode',
    label: '护理模式',
    type: 'select',
    options: [
      { label: '一对一专护', value: '一对一专护' },
      { label: '一对多轮值', value: '一对多轮值' }
    ],
    default: '一对一专护'
  },
  {
    key: 'mealStandard',
    label: '月子餐标准',
    type: 'select',
    options: [
      { label: '三餐三点', value: '三餐三点' },
      { label: '三餐两点', value: '三餐两点' }
    ],
    default: '三餐三点'
  },
  { key: 'price', label: '套餐价格（元）', type: 'money', required: true },
  { key: 'depositRatio', label: '定金比例（%）', type: 'number', default: 30, max: 100 },
  { key: 'rehabItems', label: '含康复项目', placeholder: '如：盆底修复 10 次、腹直肌 6 次' },
  { key: 'storeIds', label: '可售门店 ID', placeholder: '逗号分隔，留空表示全门店' },
  {
    key: 'status',
    label: '状态',
    type: 'select',
    options: [
      { label: '上架', value: 'ON_SHELF' },
      { label: '下架', value: 'OFF_SHELF' }
    ],
    default: 'OFF_SHELF'
  },
  { key: 'sort', label: '排序', type: 'number', default: 0 },
  { key: 'detailHtml', label: '套餐详情', type: 'richtext', full: true }
]

const productColumns = [
  { prop: 'name', label: '商品名称', min: 160 },
  { prop: 'category', label: '分类', min: 120 },
  { prop: 'price', label: '售价', width: 110, type: 'money' as const },
  { prop: 'stock', label: '库存', width: 90 },
  { prop: 'status', label: '状态', width: 90, type: 'status' as const }
]

const productFilters = [
  {
    key: 'status',
    label: '状态',
    type: 'select',
    options: [
      { label: '已上架', value: 'ON_SHELF' },
      { label: '已下架', value: 'OFF_SHELF' }
    ]
  },
  { key: 'keyword', label: '商品名称' }
]

const productFields = [
  { key: 'name', label: '商品名称', required: true, full: true },
  {
    key: 'category',
    label: '分类',
    type: 'select',
    options: [
      { label: '待产包', value: '待产包' },
      { label: '月子服', value: '月子服' },
      { label: '护理耗材', value: '护理耗材' }
    ]
  },
  { key: 'price', label: '售价（元）', type: 'money', required: true },
  { key: 'stock', label: '库存', type: 'number', default: 0 },
  { key: 'images', label: '商品图', type: 'image', multiple: true, limit: 5, dir: 'product', full: true },
  {
    key: 'status',
    label: '状态',
    type: 'select',
    options: [
      { label: '上架', value: 'ON_SHELF' },
      { label: '下架', value: 'OFF_SHELF' }
    ],
    default: 'OFF_SHELF'
  },
  { key: 'detailHtml', label: '商品详情', type: 'richtext', full: true }
]

async function togglePkg(row: any, reload: () => void) {
  const next = row.status === 'ON_SHELF' ? 'OFF_SHELF' : 'ON_SHELF'
  await tenantHttp.put(`/api/tenant/catalog/packages/${row.id}/status`, { status: next })
  ElMessage.success(next === 'ON_SHELF' ? '已上架' : '已下架')
  reload()
}

async function removePkg(row: any, reload: () => void) {
  await ElMessageBox.confirm(`确认删除套餐「${row.name}」？`, '删除确认', { type: 'warning' })
  await tenantHttp.delete(`/api/tenant/catalog/packages/${row.id}`)
  ElMessage.success('已删除')
  reload()
}

async function toggleProduct(row: any, reload: () => void) {
  const next = row.status === 'ON_SHELF' ? 'OFF_SHELF' : 'ON_SHELF'
  await tenantHttp.put(`/api/tenant/catalog/products/${row.id}/status`, { status: next })
  ElMessage.success(next === 'ON_SHELF' ? '已上架' : '已下架')
  reload()
}

function edit(row: any, reload: () => void) {
  const table = document.querySelector('.biz-tabs .el-tab-pane:not([style*="display: none"])')
  const button = table?.querySelector('.page-head .el-button--primary') as HTMLElement | null
  if (button) {
    button.click()
    setTimeout(() => {
      const inputs = document.querySelectorAll('.el-drawer input')
      if (inputs.length) {
        ;(inputs[0] as HTMLInputElement).value = row.name
      }
      void reload
    }, 180)
  }
}

function editProduct(row: any, reload: () => void) {
  edit(row, reload)
}
</script>

<style scoped>
.biz-tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
  padding: 0 4px;
  border-bottom: 1px solid var(--line);
}
.biz-tabs :deep(.el-tabs__nav-wrap::after) { display: none; }
.biz-tabs :deep(.el-tabs__item) { height: 42px; font-size: 13.5px; color: var(--ink-500); }
.biz-tabs :deep(.el-tabs__item.is-active) { color: var(--brand-700); font-weight: 600; }
.biz-tabs :deep(.el-tabs__active-bar) { background: var(--brand-600); }
.biz-tabs :deep(.crud-page) { padding-left: 0; padding-right: 0; max-width: none; }
</style>
