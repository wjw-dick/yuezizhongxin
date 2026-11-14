<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api, track } from '../api'
import LeadForm from '../components/LeadForm.vue'

const loading = ref(true)
const site = ref<any>({ name: '月之孕月子中心', phone: '400-000-0000', address: '' })
const packages = ref<any[]>([])
const rooms = ref<any[]>([])
const articles = ref<any[]>([])
const error = ref('')

onMounted(async () => {
  try {
    const data = await api.home()
    site.value = data.site || site.value
    packages.value = (data.packages || []).slice(0, 3)
    rooms.value = (data.roomTypes || []).slice(0, 4)
    articles.value = ((data as any).articles || []).slice(0, 3)
    track('h5_home_view')
  } catch (e: any) {
    error.value = '内容加载失败，请下拉重试或直接拨打门店电话'
  } finally {
    loading.value = false
  }
})

function call() {
  location.href = `tel:${(site.value.phone || '').replace(/-/g, '')}`
}
</script>

<template>
  <div class="page with-bar">
    <section class="hero">
      <h1>28 天，把坐月子<br />交给一支持证团队</h1>
      <p>独立月子房 · 一对一护理 · 四阶段月子餐 · 产后康复评估</p>
      <div class="metrics">
        <div class="metric"><strong>1:1</strong><span>护理配比</span></div>
        <div class="metric"><strong>42</strong><span>天康复跟踪</span></div>
        <div class="metric"><strong>4</strong><span>阶段月子餐</span></div>
      </div>
    </section>

    <div v-if="loading" class="card">
      <div class="muted">正在加载门店信息…</div>
    </div>
    <div v-else-if="error" class="card">
      <strong>{{ error }}</strong>
      <div style="margin-top: 10px"><button class="btn ghost inline" @click="call">拨打门店电话</button></div>
    </div>

    <section class="section">
      <div class="section-head">
        <h2>月子套餐</h2>
        <router-link to="/packages" class="muted">全部套餐 ›</router-link>
      </div>
      <div v-if="packages.length">
        <div v-for="pkg in packages" :key="pkg.id" class="card">
          <div class="between">
            <strong>{{ pkg.name }}</strong>
            <span class="price">¥{{ pkg.price }}</span>
          </div>
          <div class="chips" style="margin-top: 10px">
            <span class="tag muted">{{ pkg.days }} 天</span>
            <span class="tag muted">{{ pkg.roomTypeLevel }}</span>
            <span class="tag muted">{{ pkg.careMode }}</span>
          </div>
          <p class="muted" style="margin: 10px 0 0">{{ pkg.rehabItems || '含基础康复评估与月子餐' }}</p>
        </div>
      </div>
      <div v-else class="empty">
        <strong>套餐整理中</strong>
        <span>可先预约参观，顾问现场为您匹配档期</span>
      </div>
    </section>

    <section class="section">
      <div class="section-head">
        <h2>房型实拍</h2>
        <router-link to="/packages#rooms" class="muted">看全部房型 ›</router-link>
      </div>
      <div class="grid2">
        <div v-for="room in rooms" :key="room.id" class="card" style="padding: 10px">
          <img v-if="room.images && room.images.length" class="thumb" :src="room.images[0]" alt="" />
          <div v-else class="thumb thumb-ph">房型图</div>
          <div style="margin-top: 8px; font-size: 14px; color: #1f2421">{{ room.name }}</div>
          <div class="muted" style="font-size: 12px">{{ room.area }}㎡ · {{ room.bedType }}</div>
        </div>
      </div>
    </section>

    <section class="section">
      <div class="section-head"><h2>预约参观</h2><span>30 分钟内回电</span></div>
      <LeadForm />
    </section>

    <section v-if="articles.length" class="section">
      <div class="section-head"><h2>新手爸妈必读</h2></div>
      <div class="card" v-for="item in articles" :key="item.id">
        <div style="font-size: 15px; color: #1f2421">{{ item.title }}</div>
        <div class="muted" style="font-size: 12px; margin-top: 4px">{{ item.summary || item.description }}</div>
      </div>
    </section>
  </div>

  <div class="bottombar">
    <button class="btn ghost" style="flex: 0 0 40%" @click="call">电话咨询</button>
    <router-link to="/book" class="btn" style="text-align: center">预约到店</router-link>
  </div>
</template>
