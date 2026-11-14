<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { track } from '../api'

const route = useRoute()
const status = computed(() => String(route.query.status || 'paid'))
const orderNo = computed(() => String(route.query.orderNo || ''))
const paymentId = computed(() => String(route.query.paymentId || ''))

const preset = computed(() => {
  const map: Record<string, { title: string; desc: string; tip: string; icon: string }> = {
    paid: {
      title: '支付成功',
      desc: '档期已为您锁定，顾问会尽快电话确认入馆细节',
      tip: '请保留订单号，到店签约时出示',
      icon: '✓'
    },
    pending: {
      title: '订单已提交',
      desc: '当前未开通在线支付，请到店或联系顾问完成定金支付',
      tip: '未支付定金的订单 24 小时后会自动释放档期',
      icon: '…'
    },
    cancel: {
      title: '支付未完成',
      desc: '您已取消支付，档期会为您保留 24 小时',
      tip: '可在「订单查询」中重新发起支付或联系顾问',
      icon: '!'
    }
  }
  return map[status.value] || map.paid
})

onMounted(() => {
  track('h5_pay_result', { status: status.value, orderNo: orderNo.value })
})
</script>

<template>
  <div class="page">
    <div class="card" style="text-align: center; margin-top: 28px">
      <div
        style="width: 64px; height: 64px; border-radius: 50%; margin: 0 auto 12px;
               display: flex; align-items: center; justify-content: center; font-size: 28px;
               background: #eef7f2; color: #246a54"
      >{{ preset.icon }}</div>
      <h2 style="margin: 0 0 6px; font-size: 19px; color: #1f2421">{{ preset.title }}</h2>
      <p class="muted" style="margin: 0">{{ preset.desc }}</p>

      <div v-if="orderNo" class="divider"></div>
      <div v-if="orderNo" class="kv"><span class="k">订单号</span><span>{{ orderNo }}</span></div>
      <div v-if="paymentId" class="kv"><span class="k">支付流水</span><span>{{ paymentId }}</span></div>
    </div>

    <div class="card" style="margin-top: 12px">
      <span class="muted">{{ preset.tip }}</span>
    </div>

    <div style="margin-top: 20px; display: flex; flex-direction: column; gap: 10px">
      <router-link to="/orders" class="btn">查看订单进度</router-link>
      <router-link to="/" class="btn ghost">返回首页</router-link>
    </div>
  </div>
</template>
