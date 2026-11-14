<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api } from '../api'

const packages = ref<any[]>([])
const rooms = ref<any[]>([])
const loading = ref(true)
const level = ref('')
const active = ref<any>(null)

const levels = computed(() => ['', ...Array.from(new Set(rooms.value.map((r) => r.level).filter(Boolean)))])
const filtered = computed(() => (level.value ? rooms.value.filter((r) => r.level === level.value) : rooms.value))

onMounted(async () => {
  try {
    const [pkg, room] = await Promise.all([api.packages(), api.rooms()])
    packages.value = pkg || []
    rooms.value = room || []
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="page with-bar">
    <h2 style="margin: 18px 0 4px; font-size: 20px; color: #1f2421">套餐与房型</h2>
    <p class="muted" style="margin: 0 0 18px">
      同档套餐可换房，档期以门店实时房态为准；价格含住宿、护理、月子餐与基础康复。
    </p>

    <div v-if="loading" class="card"><span class="muted">加载中…</span></div>

    <div v-for="pkg in packages" :key="pkg.id" class="card">
      <div class="between">
        <div>
          <strong>{{ pkg.name }}</strong>
          <div class="chips" style="margin-top: 8px">
            <span class="tag muted">{{ pkg.days }} 天</span>
            <span class="tag muted">{{ pkg.roomTypeLevel }}</span>
            <span class="tag warn">定金 {{ Math.round((pkg.depositRatio || 0.3) * 100) }}%</span>
          </div>
        </div>
        <div style="text-align: right">
          <div class="price">¥{{ pkg.price }}</div>
          <div class="muted" style="font-size: 12px">含 {{ pkg.careMode }}</div>
        </div>
      </div>
      <div class="divider"></div>
      <div class="kv"><span class="k">康复项目</span><span>{{ pkg.rehabItems || '基础康复评估' }}</span></div>
      <div class="kv"><span class="k">月子餐</span><span>四阶段调理，可登记忌口</span></div>
      <router-link to="/book" class="btn ghost" style="margin-top: 12px">预约参观该套餐</router-link>
    </div>

    <div class="section" id="rooms">
      <div class="section-head"><h2>房型</h2><span>{{ filtered.length }} 种</span></div>
      <div class="chips" style="margin-bottom: 12px">
        <span
          v-for="lv in levels"
          :key="lv || 'all'"
          class="chip"
          :class="{ on: level === lv }"
          @click="level = lv"
        >{{ lv || '全部' }}</span>
      </div>

      <div v-for="room in filtered" :key="room.id" class="card" @click="active = room">
        <div class="row" style="gap: 12px">
          <img v-if="room.images && room.images.length" class="thumb" style="width: 108px; height: 82px" :src="room.images[0]" alt="" />
          <div v-else class="thumb thumb-ph" style="width: 108px; height: 82px">房型图</div>
          <div class="grow">
            <div class="between">
              <strong>{{ room.name }}</strong>
              <span class="tag">{{ room.level }}</span>
            </div>
            <div class="muted" style="font-size: 12px; margin-top: 4px">
              {{ room.area }}㎡ · {{ room.bedType }} · 可住 {{ room.maxGuest }} 人
            </div>
            <div class="muted" style="font-size: 12px">{{ room.facilities }}</div>
          </div>
        </div>
      </div>

      <div v-if="!filtered.length && !loading" class="empty">
        <strong>该档位暂无房源</strong>
        <span>可预约参观，顾问为您推荐相近档期</span>
      </div>
    </div>
  </div>

  <div v-if="active" class="card" style="position: fixed; inset: 0; z-index: 20; background: #fbf9f5; padding: 16px; overflow-y: auto; border-radius: 0">
    <div class="between" style="margin-bottom: 12px">
      <strong style="font-size: 17px">{{ active.name }}</strong>
      <span class="back" @click="active = null">关闭</span>
    </div>
    <img v-if="active.images && active.images.length" class="thumb" style="height: 200px" :src="active.images[0]" alt="" />
    <div class="chips" style="margin: 12px 0">
      <span class="tag">{{ active.level }}</span>
      <span class="tag muted">{{ active.area }}㎡</span>
      <span class="tag muted">{{ active.bedType }}</span>
    </div>
    <p class="muted">{{ active.facilities }}</p>
    <div v-if="active.description" class="muted" v-html="active.description" style="line-height: 1.8"></div>
    <router-link to="/book" class="btn" style="margin-top: 16px">预约参观该房型</router-link>
  </div>

  <div class="bottombar">
    <router-link to="/book" class="btn">预约参观 · 领新客礼</router-link>
  </div>
</template>
