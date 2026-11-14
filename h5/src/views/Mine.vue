<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { api, track } from '../api'

const token = ref(localStorage.getItem('memberToken') || '')
const step = ref<'login' | 'center'>(token.value ? 'center' : 'login')
const form = reactive({ phone: '', code: '', devCode: '' })
const countdown = ref(0)
const loading = ref(false)
const profile = ref<any>(null)
const coupons = ref<any[]>([])
const message = ref('')

function tick() {
  if (countdown.value <= 0) return
  setTimeout(() => {
    countdown.value -= 1
    tick()
  }, 1000)
}

async function sendCode() {
  if (!/^1[3-9]\d{9}$/.test(form.phone)) {
    message.value = '请输入正确的手机号'
    return
  }
  message.value = ''
  try {
    const res = await api.sms(form.phone)
    form.devCode = res.devCode || ''
    countdown.value = 60
    tick()
  } catch (e: any) {
    message.value = e?.message || '验证码发送失败'
  }
}

async function login() {
  if (!/^1[3-9]\d{9}$/.test(form.phone) || form.code.length < 4) {
    message.value = '请输入手机号与验证码'
    return
  }
  loading.value = true
  try {
    const res = await api.login(form.phone, form.code)
    localStorage.setItem('memberToken', res.token)
    token.value = res.token
    profile.value = res.profile
    step.value = 'center'
    track('h5_member_login')
    loadAssets()
  } catch (e: any) {
    message.value = e?.message || '登录失败'
  } finally {
    loading.value = false
  }
}

async function loadAssets() {
  try {
    const [p, c] = await Promise.all([api.profile(), api.coupons()])
    profile.value = p
    coupons.value = c || []
  } catch (e) {
    // 令牌失效时回到登录
    localStorage.removeItem('memberToken')
    token.value = ''
    step.value = 'login'
  }
}

function logout() {
  localStorage.removeItem('memberToken')
  token.value = ''
  profile.value = null
  step.value = 'login'
}

onMounted(() => {
  if (token.value) {
    loadAssets()
  }
})
</script>

<template>
  <div class="page">
    <section v-if="step === 'login'">
      <h2 style="margin: 18px 0 4px; font-size: 20px; color: #1f2421">会员登录</h2>
      <p class="muted" style="margin: 0 0 16px">同一手机号自动关联订单、护理记录与优惠券。</p>

      <div class="card">
        <div class="field">
          <label>手机号</label>
          <input v-model="form.phone" inputmode="numeric" maxlength="11" placeholder="11 位手机号" />
        </div>
        <div class="field">
          <label>验证码</label>
          <div class="row">
            <input v-model="form.code" inputmode="numeric" maxlength="6" placeholder="6 位验证码" />
            <button class="btn inline ghost" type="button" :disabled="countdown > 0" @click="sendCode">
              {{ countdown > 0 ? countdown + 's' : '获取验证码' }}
            </button>
          </div>
          <div v-if="form.devCode" class="hint">联调验证码：{{ form.devCode }}（正式环境不显示）</div>
        </div>
        <div v-if="message" class="error" style="margin-bottom: 10px">{{ message }}</div>
        <button class="btn" :disabled="loading" @click="login">{{ loading ? '登录中…' : '登录 / 注册' }}</button>
      </div>
    </section>

    <section v-else>
      <div class="card" style="margin-top: 18px">
        <div class="between">
          <div>
            <strong style="font-size: 17px">{{ profile?.nickname || '会员' }}</strong>
            <div class="muted" style="font-size: 12px">{{ profile?.phone }}</div>
          </div>
          <span class="back" @click="logout">退出</span>
        </div>
        <div class="divider"></div>
        <div class="row" style="justify-content: space-between">
          <div style="text-align: center; flex: 1">
            <div class="price">{{ profile?.points || 0 }}</div>
            <div class="muted" style="font-size: 12px">积分</div>
          </div>
          <div style="text-align: center; flex: 1">
            <div class="price">{{ profile?.balance || '0.00' }}</div>
            <div class="muted" style="font-size: 12px">储值余额</div>
          </div>
          <div style="text-align: center; flex: 1">
            <div class="price">{{ profile?.couponCount || 0 }}</div>
            <div class="muted" style="font-size: 12px">可用券</div>
          </div>
        </div>
      </div>

      <div class="section">
        <div class="section-head"><h2>我的优惠券</h2></div>
        <div v-if="coupons.length">
          <div v-for="row in coupons" :key="row.memberCoupon.id" class="card">
            <div class="between">
              <strong>{{ row.coupon?.name }}</strong>
              <span class="tag">{{ row.memberCoupon.status }}</span>
            </div>
            <div class="muted" style="font-size: 12px">
              面额 ¥{{ row.coupon?.amount }} · 满 {{ row.coupon?.threshold }} 可用
            </div>
          </div>
        </div>
        <div v-else class="empty"><strong>暂无优惠券</strong><span>签约后可领取专属复购券</span></div>
      </div>

      <div class="card" style="margin-top: 16px">
        <div class="kv"><span class="k">订单查询</span><router-link to="/orders" class="muted">去查询 ›</router-link></div>
        <div class="kv"><span class="k">预约参观</span><router-link to="/book" class="muted">去预约 ›</router-link></div>
      </div>
    </section>
  </div>
</template>
