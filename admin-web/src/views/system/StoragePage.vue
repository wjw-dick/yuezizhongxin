<template>
  <div class="page">
    <header class="page-head">
      <div>
        <h1>对象存储</h1>
        <p class="sub">连接测试通过才可保存；切换驱动需二次确认；密钥只显示掩码。</p>
      </div>
    </header>

    <div class="drivers">
      <div
        v-for="driver in drivers"
        :key="driver.code"
        class="driver-card"
        :class="{ active: form.driver === driver.code }"
        @click="pickDriver(driver.code)"
      >
        <strong>{{ driver.name }}</strong>
        <span class="muted">{{ driver.desc }}</span>
        <el-tag v-if="current.driver === driver.code && current.tested" size="small" type="success">使用中</el-tag>
      </div>
    </div>

    <div class="card">
      <h3 class="card-title">{{ activeDriverName }} 参数</h3>
      <el-form label-width="140px" label-position="left" style="max-width: 760px">
        <template v-if="form.driver === 'local'">
          <el-alert
            type="info"
            :closable="false"
            title="本地磁盘驱动无需额外参数，文件写入服务端 data/storage 目录，路径强制 tenants/{租户标识}/ 前缀。"
          />
        </template>
        <template v-else>
          <el-form-item label="服务地址" required>
            <el-input v-model="form.endpoint" placeholder="https://oss-cn-hangzhou.aliyuncs.com" />
          </el-form-item>
          <el-form-item label="Region">
            <el-input v-model="form.region" placeholder="cn-hangzhou / us-east-1" />
          </el-form-item>
          <el-form-item label="Bucket" required>
            <el-input v-model="form.bucket" placeholder="yzz-files" />
          </el-form-item>
          <el-form-item label="AccessKey">
            <el-input v-model="form.accessKey" :placeholder="current.accessKeyMasked || 'AK'" />
          </el-form-item>
          <el-form-item label="SecretKey">
            <el-input
              v-model="form.secretKey"
              type="password"
              show-password
              :placeholder="current.secretKeyMasked ? '已保存，留空表示不修改' : 'SK'"
            />
          </el-form-item>
          <el-form-item label="Path Style">
            <el-switch v-model="form.pathStyle" />
            <span class="muted" style="margin-left: 8px">MinIO 与自建网关需要开启</span>
          </el-form-item>
          <el-form-item label="访问前缀">
            <el-input v-model="form.publicBaseUrl" placeholder="可选，留空按服务地址拼接" />
          </el-form-item>
        </template>

        <div class="toolbar">
          <el-button :loading="testing" @click="test">连接测试</el-button>
          <el-button type="primary" :disabled="form.driver !== 'local' && !tested" :loading="saving" @click="save">
            保存配置
          </el-button>
          <span class="muted" v-if="form.driver !== 'local' && !tested">请先完成连接测试</span>
        </div>
      </el-form>
    </div>

    <div class="card">
      <h3 class="card-title">当前生效配置 <small>只读</small></h3>
      <div class="kv-grid">
        <div><span>驱动</span><b>{{ current.driver || '-' }}</b></div>
        <div><span>Bucket</span><b>{{ current.bucket || '-' }}</b></div>
        <div><span>区域</span><b>{{ current.region || '-' }}</b></div>
        <div><span>AccessKey</span><b class="mono">{{ current.accessKeyMasked || '-' }}</b></div>
        <div><span>SecretKey</span><b class="mono">{{ current.secretKeyMasked || '-' }}</b></div>
        <div>
          <span>连通状态</span>
          <b><span class="pill plain" :class="current.tested ? 'ok' : 'muted'">{{ current.tested ? '已通过' : '未测试' }}</span></b>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getStorageConfig, saveStorage, testStorage } from '../../api/modules'

const drivers = [
  { code: 'local', name: '本地磁盘', desc: '默认，零依赖，适合单机与演示' },
  { code: 's3', name: 'AWS S3', desc: '海外部署首选' },
  { code: 'minio', name: 'MinIO', desc: '私有化部署，S3 兼容' },
  { code: 'oss', name: '阿里云 OSS', desc: '国内备案场景常用' },
  { code: 'cos', name: '腾讯云 COS', desc: '与微信生态同厂' }
]

const current = ref<any>({})
const tested = ref(false)
const testing = ref(false)
const saving = ref(false)
const form = reactive<any>({
  driver: 'local', endpoint: '', region: '', bucket: '', accessKey: '', secretKey: '', pathStyle: true, publicBaseUrl: ''
})

const activeDriverName = computed(() => drivers.find((d) => d.code === form.driver)?.name || '')
const switching = computed(() => current.value.driver && current.value.driver !== form.driver)

const load = async () => {
  const res: any = await getStorageConfig()
  current.value = res.data
  Object.assign(form, {
    driver: res.data.driver || 'local',
    endpoint: res.data.endpoint || '',
    region: res.data.region || '',
    bucket: res.data.bucket || '',
    accessKey: '',
    secretKey: '',
    pathStyle: res.data.pathStyle ?? true,
    publicBaseUrl: res.data.publicBaseUrl || ''
  })
  tested.value = !!res.data.tested
}

const pickDriver = async (code: string) => {
  if (form.driver === code) return
  form.driver = code
  tested.value = code === 'local'
  if (code !== 'local' && current.value.driver && current.value.driver !== code) {
    await ElMessageBox.confirm(
      '切换驱动后新文件写入新驱动，历史文件仍按原路径访问。确认切换？',
      '切换驱动确认',
      { type: 'warning' }
    ).catch(() => {
      form.driver = current.value.driver
    })
  }
}

const test = async () => {
  testing.value = true
  try {
    const res: any = await testStorage({ ...form, accessKey: form.accessKey || undefined, secretKey: form.secretKey || undefined })
    tested.value = true
    ElMessage.success(res.data)
  } finally {
    testing.value = false
  }
}

const save = async () => {
  saving.value = true
  try {
    await saveStorage({ ...form, confirmSwitch: switching.value || form.driver === 'local' })
    ElMessage.success('配置已保存并生效')
    await load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.drivers { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px; }
:deep(.el-alert) { align-items: flex-start; border-radius: 10px; }
.driver-card {
  position: relative;
  display: flex; flex-direction: column; gap: 4px; padding: 14px 16px;
  border: 1px solid var(--line); border-radius: var(--radius-md);
  background: var(--surface); cursor: pointer;
  transition: border-color 0.15s ease, background 0.15s ease;
}
.driver-card strong { font-size: 13.5px; font-weight: 550; }
.driver-card:hover { border-color: var(--line-strong); background: var(--surface-sunken); }
.driver-card.active { border-color: var(--brand-500); background: var(--brand-50); }
.driver-card.active::after {
  content: ''; position: absolute; top: 12px; right: 12px;
  width: 7px; height: 7px; border-radius: 50%; background: var(--brand-600);
}

.kv-grid {
  display: grid; grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px 24px;
}
@media (max-width: 1000px) { .kv-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 640px) { .kv-grid { grid-template-columns: 1fr; } }
.kv-grid > div { display: flex; flex-direction: column; gap: 3px; }
.kv-grid span { font-size: 11.5px; color: var(--ink-400); }
.kv-grid b { font-size: 13px; font-weight: 500; color: var(--ink-900); }
</style>
