<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api } from '../api'

const list = ref<any[]>([])
const loading = ref(true)
const active = ref<any>(null)

onMounted(async () => {
  try {
    const data = await api.contents('案例')
    list.value = data || []
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="page with-bar" v-if="!active">
    <h2 style="margin: 18px 0 4px; font-size: 20px; color: #1f2421">客户案例</h2>
    <p class="muted" style="margin: 0 0 18px">
      所有案例均经客户书面授权展示，姓名与照片已做处理；未经授权不展示任何可识别信息。
    </p>

    <div v-if="loading" class="card"><span class="muted">加载中…</span></div>

    <div v-for="item in list" :key="item.id" class="card" @click="active = item">
      <div class="between">
        <strong>{{ item.title }}</strong>
        <span class="tag">{{ item.customerName || '已授权客户' }}</span>
      </div>
      <p class="muted" style="margin: 8px 0 0">{{ item.summary || item.description }}</p>
      <div class="muted" style="margin-top: 10px; font-size: 12px">查看全文 ›</div>
    </div>

    <div v-if="!list.length && !loading" class="empty">
      <strong>案例整理中</strong>
      <span>可先预约参观，现场看护理记录样本</span>
    </div>
  </div>

  <div v-else class="page">
    <div class="between" style="margin: 16px 0">
      <span class="back" @click="active = null">‹ 返回列表</span>
      <span class="tag">已授权</span>
    </div>
    <h2 style="margin: 0 0 6px; font-size: 20px; color: #1f2421">{{ active.title }}</h2>
    <p class="muted" style="margin: 0 0 16px">{{ active.publishTime ? String(active.publishTime).slice(0, 10) : '' }}</p>
    <div class="card">
      <div class="muted" style="line-height: 1.9" v-html="active.content || active.summary"></div>
    </div>
    <router-link to="/book" class="btn" style="margin-top: 16px">预约同款房型</router-link>
  </div>
</template>
