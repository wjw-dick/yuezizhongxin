<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api, track } from '../api'

const list = ref<any[]>([])
const loading = ref(true)

onMounted(async () => {
  try {
    const data = await api.activities()
    list.value = (data || []).filter((item) => item.type === '活动' || !item.type)
    track('h5_activity_view')
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="page with-bar">
    <section class="hero">
      <h1>到店礼遇</h1>
      <p>参观即赠新生礼包，签约再享月子餐升级与康复体验</p>
    </section>

    <div v-if="loading" class="card"><span class="muted">加载中…</span></div>

    <div v-else-if="list.length">
      <div v-for="item in list" :key="item.id" class="card">
        <div class="between">
          <strong>{{ item.title }}</strong>
          <span class="tag warn">{{ item.publishTime ? String(item.publishTime).slice(0, 10) : '进行中' }}</span>
        </div>
        <p class="muted" style="margin: 8px 0 0">{{ item.summary || item.description }}</p>
        <div class="divider"></div>
        <div class="kv"><span class="k">参与方式</span><span>预约到店，前台出示本页</span></div>
        <div class="kv"><span class="k">适用范围</span><span>新客首访，不可与其他优惠叠加</span></div>
      </div>
    </div>

    <div v-else class="empty">
      <strong>本月活动筹备中</strong>
      <span>预约参观后顾问会告知最新礼遇</span>
    </div>

    <div class="card" style="margin-top: 16px">
      <strong>为什么建议先到店</strong>
      <ul class="muted" style="margin: 8px 0 0; padding-left: 18px; line-height: 1.9">
        <li>试吃当日在供月子餐，尝过再决定</li>
        <li>参观真实在住房间与护理站，不看样板间</li>
        <li>现场做一次产后康复评估，了解恢复重点</li>
      </ul>
    </div>
  </div>

  <div class="bottombar">
    <router-link to="/book" class="btn">立即预约到店</router-link>
  </div>
</template>
