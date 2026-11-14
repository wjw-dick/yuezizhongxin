<template>
  <div class="login">
    <aside class="visual">
      <div class="visual-inner">
        <div class="brand">
          <div class="logo">月</div>
          <div>
            <strong>月之孕</strong>
            <span>月子中心数字化运营平台</span>
          </div>
        </div>

        <h1>一家月子中心，<br />一套自己的运营系统</h1>
        <p class="lede">
          房态、订单、护理、康复、餐食与会员，共用一套底座；<br />
          每家门店独立租户，数据互不可见。
        </p>

        <ul class="features">
          <li v-for="f in features" :key="f.title">
            <span class="fico"><Icon :name="f.icon" :size="16" /></span>
            <div>
              <strong>{{ f.title }}</strong>
              <span>{{ f.desc }}</span>
            </div>
          </li>
        </ul>

        <div class="visual-foot">多租户隔离 · 五态生命周期 · 全链路审计</div>
      </div>
      <div class="glow a" />
      <div class="glow b" />
    </aside>

    <main class="panel">
      <div class="form-wrap">
        <div class="segmented">
          <button :class="{ active: mode === 'platform' }" @click="switchMode('platform')">平台运营端</button>
          <button :class="{ active: mode === 'tenant' }" @click="switchMode('tenant')">租户后台</button>
        </div>

        <h2>{{ mode === 'platform' ? '平台运营登录' : '租户后台登录' }}</h2>
        <p class="hint">
          {{
            mode === 'platform'
              ? '平台账号可管理租户、套餐与账单，不查看租户业务明细。'
              : '请输入租户标识与管理员账号，租户标识决定数据边界。'
          }}
        </p>

        <el-form :model="form" label-position="top" size="large" @submit.prevent="submit">
          <el-form-item v-if="mode === 'tenant'" label="租户标识">
            <el-input v-model="form.tenantCode" placeholder="如 demo" @keyup.enter="submit">
              <template #prefix><Icon name="building" :size="16" /></template>
            </el-input>
          </el-form-item>
          <el-form-item label="账号">
            <el-input v-model="form.username" placeholder="登录账号" @keyup.enter="submit">
              <template #prefix><Icon name="users" :size="16" /></template>
            </el-input>
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="form.password" type="password" show-password placeholder="登录密码" @keyup.enter="submit">
              <template #prefix><Icon name="key" :size="16" /></template>
            </el-input>
          </el-form-item>

          <el-button type="primary" class="submit" :loading="loading" @click="submit">
            登录{{ mode === 'platform' ? '平台' : '' }}
          </el-button>
        </el-form>

        <div class="demo">
          <span class="demo-label">演示账号</span>
          <button v-for="d in demos" :key="d.label" class="demo-pill" @click="fill(d)">
            {{ d.label }}
          </button>
        </div>

        <p class="foot">
          口令统一为 <code>Admin@123456</code>；生产环境请先修改初始口令并配置 SSO。
        </p>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import Icon from '../components/Icon.vue'
import { login } from '../api/modules'
import { platformLogin } from '../api/platform'
import {
  PLATFORM_TOKEN_KEY,
  PLATFORM_USER_KEY,
  TENANT_KEY,
  TOKEN_KEY,
  USER_KEY
} from '../api/client'

const router = useRouter()
const route = useRoute()
const loading = ref(false)

const mode = ref<'platform' | 'tenant'>(route.query.app === 'platform' ? 'platform' : 'tenant')

const form = reactive({
  tenantCode: localStorage.getItem(TENANT_KEY) || 'demo',
  username: mode.value === 'platform' ? 'platform' : 'admin',
  password: ''
})

const features = [
  { icon: 'shield', title: '租户级数据隔离', desc: '请求头、令牌、DAO 三层校验，越权一律 403' },
  { icon: 'layers', title: '一套底座多端复用', desc: '后台、官网、小程序、H5 共用业务服务' },
  { icon: 'chart', title: '全链路留痕', desc: '开通、停用、改价、导出均写审计' }
]

const demos = computed(() =>
  mode.value === 'platform'
    ? [
        { label: '平台超管', tenantCode: '', username: 'platform' },
        { label: '运营', tenantCode: '', username: 'oper' },
        { label: '财务', tenantCode: '', username: 'finance' }
      ]
    : [
        { label: '租户 demo', tenantCode: 'demo', username: 'admin' }
      ]
)

const switchMode = (next: 'platform' | 'tenant') => {
  mode.value = next
  form.password = ''
  form.username = next === 'platform' ? 'platform' : 'admin'
}

const fill = (d: { tenantCode: string; username: string }) => {
  if (d.tenantCode) form.tenantCode = d.tenantCode
  form.username = d.username
  form.password = 'Admin@123456'
}

const submit = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning('请填写账号与密码')
    return
  }
  if (mode.value === 'tenant' && !form.tenantCode) {
    ElMessage.warning('请填写租户标识')
    return
  }
  loading.value = true
  try {
    if (mode.value === 'platform') {
      const res: any = await platformLogin({ username: form.username, password: form.password })
      localStorage.setItem(PLATFORM_TOKEN_KEY, res.data.token)
      localStorage.setItem(PLATFORM_USER_KEY, JSON.stringify(res.data))
      router.push('/platform/dashboard')
    } else {
      const res: any = await login({
        tenantCode: form.tenantCode,
        username: form.username,
        password: form.password
      })
      localStorage.setItem(TOKEN_KEY, res.data.token)
      localStorage.setItem(TENANT_KEY, form.tenantCode)
      localStorage.setItem(USER_KEY, JSON.stringify(res.data))
      router.push(String(route.query.redirect || '/dashboard'))
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login { display: grid; grid-template-columns: 1.05fr 0.95fr; height: 100%; background: var(--surface); }

/* 左侧品牌区 */
.visual {
  position: relative;
  overflow: hidden;
  background: linear-gradient(158deg, #10241d 0%, #17382c 46%, #1d4a3a 100%);
  color: #eaf3ee;
  display: flex;
  align-items: center;
  padding: 64px 64px;
}
.visual-inner { position: relative; z-index: 2; max-width: 460px; }
.glow { position: absolute; border-radius: 50%; filter: blur(2px); opacity: 0.5; }
.glow.a {
  width: 460px; height: 460px; right: -160px; top: -120px;
  background: radial-gradient(circle, rgba(88, 178, 146, 0.32), transparent 68%);
}
.glow.b {
  width: 380px; height: 380px; left: -120px; bottom: -140px;
  background: radial-gradient(circle, rgba(185, 138, 90, 0.22), transparent 70%);
}

.brand { display: flex; align-items: center; gap: 12px; margin-bottom: 56px; }
.logo {
  width: 40px; height: 40px; border-radius: 12px;
  display: grid; place-items: center;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.16);
  font-size: 19px; font-weight: 600; color: #f0f8f4;
}
.brand strong { display: block; font-size: 15px; letter-spacing: 0.04em; color: #f3f9f6; }
.brand span { font-size: 11.5px; color: #89a79a; letter-spacing: 0.06em; }

h1 {
  margin: 0 0 18px;
  font-size: 34px;
  line-height: 1.35;
  font-weight: 600;
  letter-spacing: -0.015em;
  color: #f6fbf8;
}
.lede { margin: 0 0 40px; font-size: 14px; line-height: 1.9; color: #a9c3b7; }

.features { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 18px; }
.features li { display: flex; gap: 12px; align-items: flex-start; }
.fico {
  width: 30px; height: 30px; flex: 0 0 30px; border-radius: 9px;
  display: grid; place-items: center;
  background: rgba(255, 255, 255, 0.07);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: #8fd0b6;
}
.features strong { display: block; font-size: 13.5px; font-weight: 550; color: #eef7f2; margin-bottom: 2px; }
.features span { font-size: 12.5px; color: #8fa89c; }

.visual-foot {
  margin-top: 56px;
  font-size: 11.5px;
  letter-spacing: 0.08em;
  color: #6f897d;
}

/* 右侧表单 */
.panel { display: flex; align-items: center; justify-content: center; padding: 40px; background: var(--surface); }
.form-wrap { width: 100%; max-width: 360px; }

.segmented {
  display: inline-flex; padding: 3px; gap: 3px;
  background: var(--surface-sunken);
  border: 1px solid var(--line);
  border-radius: 11px;
  margin-bottom: 34px;
}
.segmented button {
  border: 0; background: transparent; cursor: pointer;
  padding: 6px 16px; border-radius: 8px;
  font-size: 12.5px; color: var(--ink-500);
  transition: all 0.15s ease;
}
.segmented button.active {
  background: var(--surface);
  color: var(--ink-900);
  font-weight: 550;
  box-shadow: var(--shadow-xs);
}

h2 { margin: 0 0 8px; font-size: 24px; font-weight: 650; letter-spacing: -0.02em; }
.hint { margin: 0 0 28px; font-size: 12.5px; line-height: 1.7; color: var(--ink-500); }

:deep(.el-form-item__label) { font-size: 12.5px; color: var(--ink-500); padding-bottom: 4px; }
:deep(.el-input__prefix) { color: var(--ink-300); }
:deep(.el-input__wrapper) { padding: 4px 12px; }

.submit { width: 100%; height: 42px; margin-top: 6px; font-size: 14px; letter-spacing: 0.02em; }

.demo { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; margin-top: 26px; }
.demo-label { font-size: 11.5px; color: var(--ink-400); letter-spacing: 0.06em; }
.demo-pill {
  border: 1px dashed var(--line-strong);
  background: transparent;
  color: var(--ink-500);
  font-size: 11.5px;
  padding: 3px 10px;
  border-radius: 999px;
  cursor: pointer;
}
.demo-pill:hover { border-color: var(--brand-500); color: var(--brand-700); background: var(--brand-50); }

.foot { margin-top: 22px; font-size: 11.5px; line-height: 1.8; color: var(--ink-400); }
.foot code { background: var(--surface-sunken); border: 1px solid var(--line); border-radius: 5px; padding: 1px 5px; }

@media (max-width: 900px) {
  .login { grid-template-columns: 1fr; }
  .visual { display: none; }
}
</style>
