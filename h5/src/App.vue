<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const showBack = computed(() => route.name !== 'home')

function back() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/')
  }
}
</script>

<template>
  <header class="topbar">
    <div class="brand">
      <span class="mark">月</span>
      <span>月之孕月子中心</span>
    </div>
    <span v-if="showBack" class="back" @click="back">返回</span>
    <router-link v-else to="/mine" class="back">我的</router-link>
  </header>

  <router-view />

  <footer class="footer">
    <div>月之孕月子中心 · 母婴护理服务</div>
    <div>预约与咨询请拨打 400-000-0000</div>
    <div style="margin-top: 6px; color: #b6bdb6">本页信息以门店实际签约为准</div>
  </footer>
</template>
