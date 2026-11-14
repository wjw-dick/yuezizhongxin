<template>
  <div class="page">
    <div class="page-title">
      <h2>SSO 配置</h2>
      <span class="hint">CAS / OAuth2 / OIDC / Sa-Token；只填根地址，协议路径自动拼接；Secret 加密掩码</span>
    </div>

    <div class="card">
      <el-form label-width="140px" label-position="left" style="max-width: 760px">
        <el-form-item label="启用 SSO">
          <el-switch v-model="form.enabled" />
          <span class="muted" style="margin-left: 12px">关闭时仅支持账号密码登录</span>
        </el-form-item>
        <el-form-item label="协议">
          <el-radio-group v-model="form.protocol">
            <el-radio-button v-for="p in protocols" :key="p" :value="p">{{ p }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="服务器根地址" required>
          <el-input v-model="form.serverUrl" placeholder="https://id.example.com" />
          <div class="muted">粘贴完整授权地址也可以，保存时会自动裁掉协议后缀</div>
        </el-form-item>
        <el-form-item label="回调地址">
          <el-input v-model="form.serviceUrl" placeholder="https://demo.example.com/api/sso/callback" />
        </el-form-item>
        <el-form-item label="客户端标识">
          <el-input v-model="form.clientId" placeholder="client_id" />
        </el-form-item>
        <el-form-item label="客户端密钥">
          <el-input
            v-model="form.clientSecret"
            type="password"
            show-password
            :placeholder="current.clientSecretMasked ? '已保存，留空表示不修改' : 'client_secret'"
          />
        </el-form-item>
        <el-form-item label="账号映射字段">
          <el-input v-model="form.accountMapping" placeholder="username / email / phone" />
        </el-form-item>
        <el-form-item label="故障降级">
          <el-switch v-model="form.fallbackEnabled" />
          <span class="muted" style="margin-left: 12px">SSO 不可用时允许账号密码登录</span>
        </el-form-item>
        <el-form-item label="账号不存在">
          <el-radio-group v-model="form.rejectStrategy">
            <el-radio value="拒绝">拒绝登录</el-radio>
            <el-radio value="自动创建">自动创建账号</el-radio>
          </el-radio-group>
        </el-form-item>
        <div class="toolbar">
          <el-button type="primary" :loading="saving" @click="save">保存配置</el-button>
          <el-button @click="load">重置</el-button>
        </div>
      </el-form>
    </div>

    <div class="card">
      <h3 class="card-title">自动拼接的协议地址</h3>
      <el-table :data="current.endpoints || []" size="small" border>
        <el-table-column prop="name" label="用途" width="140" />
        <el-table-column prop="url" label="地址">
          <template #default="{ row }"><span class="mono">{{ row.url }}</span></template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!(current.endpoints || []).length" description="保存服务器地址后展示" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getSsoConfig, saveSsoConfig } from '../../api/modules'

const protocols = ['CAS', 'OAuth2', 'OIDC', 'Sa-Token']
const current = ref<any>({})
const saving = ref(false)
const form = reactive<any>({
  enabled: false, protocol: 'OIDC', serverUrl: '', serviceUrl: '', clientId: '',
  clientSecret: '', accountMapping: 'username', fallbackEnabled: true, rejectStrategy: '拒绝'
})

const load = async () => {
  const res: any = await getSsoConfig()
  current.value = res.data
  Object.assign(form, {
    enabled: !!res.data.enabled,
    protocol: res.data.protocol || 'OIDC',
    serverUrl: res.data.serverUrl || '',
    serviceUrl: res.data.serviceUrl || '',
    clientId: res.data.clientId || '',
    clientSecret: '',
    accountMapping: res.data.accountMapping || 'username',
    fallbackEnabled: res.data.fallbackEnabled ?? true,
    rejectStrategy: res.data.rejectStrategy || '拒绝'
  })
}

const save = async () => {
  if (!form.serverUrl) {
    ElMessage.warning('服务器根地址必填')
    return
  }
  saving.value = true
  try {
    const res: any = await saveSsoConfig({ ...form, clientSecret: form.clientSecret || undefined })
    current.value = res.data
    form.clientSecret = ''
    ElMessage.success('SSO 配置已保存')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>
