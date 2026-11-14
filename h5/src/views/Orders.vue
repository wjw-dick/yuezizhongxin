<script setup lang="ts">
import { reactive, ref } from 'vue'
import { api, track } from '../api'

const form = reactive({ orderNo: '', phone: '' })
const loading = ref(false)
const error = ref('')
const order = ref<any>(null)

async function query() {
  error.value = ''
  order.value = null
  if (!form.orderNo.trim()) {
    error.value = '请输入订单号'
    return
  }
  if (!/^1[3-9]\d{9}$/.test(form.phone)) {
    error.value = '请输入下单时使用的手机号'
    return
  }
  loading.value = true
  try {
    order.value = await api.queryOrder(form.orderNo.trim(), form.phone.trim())
    track('h5_order_query', { orderNo: form.orderNo })
  } catch (e: any) {
    error.value = e?.message || '订单号与手机号不匹配'
  } finally {
    loading.value = false
  }
}

function progress(item: any) {
  const total = Number(item.totalAmount || 0)
  const paid = Number(item.paidAmount || 0)
  return total > 0 ? Math.min(100, Math.round((paid / total) * 100)) : 0
}
</script>

<template>
  <div class="page">
    <h2 style="margin: 18px 0 4px; font-size: 20px; color: #1f2421">订单查询</h2>
    <p class="muted" style="margin: 0 0 16px">输入订单号与下单手机号即可查看进度，无需登录。</p>

    <form class="card" @submit.prevent="query">
      <div class="field">
        <label>订单号</label>
        <input v-model="form.orderNo" placeholder="如 YZ20261114000001" />
      </div>
      <div class="field">
        <label>下单手机号</label>
        <input v-model="form.phone" inputmode="numeric" maxlength="11" placeholder="11 位手机号" />
      </div>
      <div v-if="error" class="error" style="margin-bottom: 10px">{{ error }}</div>
      <button class="btn" type="submit" :disabled="loading">{{ loading ? '查询中…' : '查询订单' }}</button>
    </form>

    <div v-if="order" class="section">
      <div class="section-head"><h2>订单进度</h2><span class="tag">{{ order.status }}</span></div>
      <div class="card">
        <div class="between">
          <strong>{{ order.orderNo }}</strong>
          <span class="price">¥{{ order.totalAmount }}</span>
        </div>
        <div class="divider"></div>
        <div class="kv"><span class="k">入住人</span><span>{{ order.customerName }}</span></div>
        <div class="kv"><span class="k">入离馆</span><span>{{ order.checkinDate }} ~ {{ order.checkoutDate }}</span></div>
        <div class="kv"><span class="k">已付 / 待付</span><span>¥{{ order.paidAmount }} / ¥{{ order.tailAmount }}</span></div>
        <div style="margin-top: 10px">
          <div style="height: 8px; background: #eee9de; border-radius: 999px; overflow: hidden">
            <div :style="{ width: progress(order) + '%', height: '100%', background: 'linear-gradient(90deg,#4fae8d,#2f8b70)' }"></div>
          </div>
          <div class="muted" style="font-size: 12px; margin-top: 4px">支付进度 {{ progress(order) }}%</div>
        </div>
      </div>

      <div v-if="order.items && order.items.length" class="card">
        <div class="muted" style="margin-bottom: 8px">费用明细</div>
        <div v-for="item in order.items" :key="item.id" class="kv">
          <span class="k">{{ item.itemName }} ×{{ item.quantity }}</span>
          <span>¥{{ item.amount }}</span>
        </div>
      </div>
    </div>

    <div v-else class="card" style="margin-top: 16px">
      <strong>找不到订单？</strong>
      <p class="muted" style="margin: 6px 0 0; font-size: 13px">
        订单号为签约或下单时收到的 YZ 开头编号；仍找不到可致电门店核对。
      </p>
    </div>
  </div>
</template>
