<template>
  <div class="page">
    <header class="page-head">
      <div>
        <h1>套餐与配额</h1>
        <p class="sub">套餐定义五类额度：席位、公众号、存储、短信、接口；单租户调整在租户详情内留痕。</p>
      </div>
      <div class="actions">
        <el-button type="primary" @click="openEdit()">
          <Icon name="plus" :size="15" style="margin-right: 5px" />新建套餐
        </el-button>
      </div>
    </header>

    <section class="pkg-grid">
      <article v-for="p in packages" :key="p.id" class="pkg">
        <header class="pkg-head">
          <div>
            <h2>{{ p.packageName }}</h2>
            <span v-if="p.trialSupported === 1" class="pill info plain">支持试用</span>
            <span v-else class="pill muted plain">不支持试用</span>
          </div>
          <div class="pkg-price">
            <strong>{{ money(p.price) }}</strong>
            <span>每{{ p.billingCycle }}</span>
          </div>
        </header>

        <ul class="pkg-quotas">
          <li><span>席位</span><b>{{ p.seatQuota }} 个</b></li>
          <li><span>公众号</span><b>{{ p.mpQuota ?? '-' }} 个</b></li>
          <li><span>存储</span><b>{{ p.storageQuotaGb ?? '-' }} GB</b></li>
          <li><span>短信</span><b>{{ (p.smsQuota ?? 0).toLocaleString() }} 条</b></li>
          <li><span>接口</span><b>{{ (p.apiQuota ?? 0).toLocaleString() }} 次/月</b></li>
          <li><span>含端数</span><b>{{ p.endCount ?? '-' }} 端</b></li>
        </ul>

        <p v-if="p.remark" class="pkg-remark">{{ p.remark }}</p>

        <footer class="pkg-foot">
          <el-button link type="primary" @click="openEdit(p)">编辑</el-button>
          <el-button link type="danger" @click="remove(p)">删除</el-button>
        </footer>
      </article>

      <div v-if="!packages.length" class="card empty">
        <strong>还没有套餐</strong>
        先创建一个套餐，再开通租户时选择它。
      </div>
    </section>

    <el-dialog v-model="visible" :title="form.id ? '编辑套餐' : '新建套餐'" width="560">
      <el-form :model="form" label-position="top">
        <div class="grid">
          <el-form-item label="套餐名称" required><el-input v-model="form.packageName" /></el-form-item>
          <el-form-item label="计费周期" required>
            <el-select v-model="form.billingCycle" style="width: 100%">
              <el-option label="月" value="月" />
              <el-option label="年" value="年" />
            </el-select>
          </el-form-item>
          <el-form-item label="价格（元）" required>
            <el-input-number v-model="form.price" :min="0" :precision="2" style="width: 100%" />
          </el-form-item>
          <el-form-item label="含端数量">
            <el-input-number v-model="form.endCount" :min="1" style="width: 100%" />
          </el-form-item>
          <el-form-item label="席位额度" required>
            <el-input-number v-model="form.seatQuota" :min="1" style="width: 100%" />
          </el-form-item>
          <el-form-item label="公众号额度">
            <el-input-number v-model="form.mpQuota" :min="0" style="width: 100%" />
          </el-form-item>
          <el-form-item label="存储额度（GB）">
            <el-input-number v-model="form.storageQuotaGb" :min="0" style="width: 100%" />
          </el-form-item>
          <el-form-item label="短信额度（条）">
            <el-input-number v-model="form.smsQuota" :min="0" :step="1000" style="width: 100%" />
          </el-form-item>
          <el-form-item label="接口额度（次/月）">
            <el-input-number v-model="form.apiQuota" :min="0" :step="10000" style="width: 100%" />
          </el-form-item>
          <el-form-item label="支持试用">
            <el-switch v-model="form.trial" />
          </el-form-item>
        </div>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import Icon from '../../components/Icon.vue'
import { deletePackage, listPackages, savePackage } from '../../api/platform'
import { money } from '../../utils/format'

const packages = ref<any[]>([])
const visible = ref(false)
const saving = ref(false)

const blank = () => ({
  id: null as number | null,
  packageName: '',
  price: 0,
  billingCycle: '年',
  endCount: 4,
  seatQuota: 10,
  mpQuota: 1,
  storageQuotaGb: 20,
  smsQuota: 5000,
  apiQuota: 100000,
  trial: true,
  remark: ''
})
const form = reactive(blank())

const load = async () => {
  const res: any = await listPackages()
  packages.value = res.data || []
}

const openEdit = (p?: any) => {
  Object.assign(form, blank())
  if (p) {
    Object.assign(form, {
      id: p.id,
      packageName: p.packageName,
      price: Number(p.price),
      billingCycle: p.billingCycle,
      endCount: p.endCount,
      seatQuota: p.seatQuota,
      mpQuota: p.mpQuota,
      storageQuotaGb: p.storageQuotaGb,
      smsQuota: p.smsQuota,
      apiQuota: p.apiQuota,
      trial: p.trialSupported === 1,
      remark: p.remark || ''
    })
  }
  visible.value = true
}

const submit = async () => {
  if (!form.packageName) {
    ElMessage.warning('请填写套餐名称')
    return
  }
  saving.value = true
  try {
    const { trial, ...rest } = form
    await savePackage({ ...rest, trialSupported: trial ? 1 : 0 })
    ElMessage.success('已保存')
    visible.value = false
    load()
  } finally {
    saving.value = false
  }
}

const remove = async (p: any) => {
  await ElMessageBox.confirm(
    `删除套餐「${p.packageName}」后，已开通的租户不受影响，但新开通时无法再选择。`,
    '删除套餐',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
  )
  await deletePackage(p.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.pkg-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(290px, 1fr)); gap: 14px; }

.pkg {
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}
.pkg:hover { border-color: var(--line-strong); box-shadow: var(--shadow-sm); }

.pkg-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; }
.pkg-head h2 { margin: 0 0 6px; font-size: 15.5px; font-weight: 600; }
.pkg-price { text-align: right; white-space: nowrap; }
.pkg-price { text-align: right; }
.pkg-price strong { display: block; font-size: 21px; font-weight: 650; letter-spacing: -0.02em; line-height: 1.2; }
.pkg-price span { font-size: 11px; color: var(--ink-400); }

.pkg-quotas {
  list-style: none; margin: 0; padding: 14px 0 0;
  border-top: 1px dashed var(--line);
  display: flex; flex-direction: column; gap: 9px;
}
.pkg-quotas li { display: flex; justify-content: space-between; align-items: baseline; font-size: 12.5px; }
.pkg-quotas span { color: var(--ink-400); }
.pkg-quotas b { font-weight: 550; color: var(--ink-900); font-variant-numeric: tabular-nums; }

.pkg-remark { margin: 0; font-size: 12px; color: var(--ink-400); line-height: 1.6; }

.pkg-foot { margin-top: auto; padding-top: 12px; border-top: 1px solid var(--line); display: flex; gap: 4px; }

.grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0 16px; }
</style>
