<template>
  <div>
    <section class="page-hero">
      <div class="wrap">
        <span class="eyebrow">ROOMS & PACKAGES</span>
        <h1 class="serif">房型与套餐</h1>
        <p>房型按面积、床型与配置区分；套餐按护理模式、康复项目与餐食标准组合，价格与定金比例全部公示。</p>
      </div>
    </section>

    <section class="section tight">
      <div class="wrap">
        <div class="tabs">
          <button v-for="lv in levels" :key="lv" :class="{ on: level === lv }" @click="level = lv">
            {{ lv || '全部房型' }}
          </button>
        </div>

        <div class="room-list">
          <article v-for="room in filteredRooms" :key="room.id" class="room-item">
            <div class="gallery">
              <img
                v-if="room.images?.length"
                :src="room.images[activeImage[room.id] || 0]"
                :alt="room.name"
                loading="lazy"
              />
              <div v-else class="gallery-ph">{{ room.name }}</div>
              <div v-if="(room.images?.length || 0) > 1" class="thumbs">
                <button
                  v-for="(img, i) in room.images"
                  :key="i"
                  :class="{ on: (activeImage[room.id] || 0) === i }"
                  @click="activeImage[room.id] = i"
                >
                  <img :src="img" alt="" loading="lazy" />
                </button>
              </div>
            </div>
            <div class="room-info">
              <div class="room-top">
                <h2 class="serif">{{ room.name }}</h2>
                <span class="tag">{{ room.level }}</span>
              </div>
              <p class="specs">
                <span>{{ room.area }} ㎡</span>
                <span>{{ room.bedType }}</span>
                <span>可住 {{ room.maxGuest }} 人</span>
              </p>
              <p class="fac">{{ room.facilities }}</p>
              <div class="desc prose" v-html="room.description || '<p>房型详细介绍由运营后台「房型与房间 → 房型介绍」维护。</p>'" />
              <div class="room-actions">
                <router-link :to="`/visit?room=${encodeURIComponent(room.name)}`" class="btn btn-primary">预约参观该房型</router-link>
                <router-link to="/meals" class="btn btn-ghost">看看月子餐</router-link>
              </div>
            </div>
          </article>
          <p v-if="!filteredRooms.length" class="empty">该档位暂无可售房型，可切换其他档位或直接咨询顾问。</p>
        </div>
      </div>
    </section>

    <section class="section alt">
      <div class="wrap">
        <div class="sec-head">
          <div>
            <span class="eyebrow">PACKAGES</span>
            <h2 class="serif">全部套餐</h2>
            <p>套餐含房型档位、护理模式、康复项目与餐食标准；定金比例用于在线锁档。</p>
          </div>
        </div>
        <div class="pkg-table">
          <div class="pkg-head-row">
            <span>套餐</span><span>天数</span><span>房型档位</span><span>护理模式</span><span>餐食</span><span class="right">价格</span><span class="right">定金</span><span />
          </div>
          <div v-for="pkg in packages" :key="pkg.id" class="pkg-row">
            <span class="name">
              <strong>{{ pkg.name }}</strong>
              <em class="muted">{{ pkg.rehabItems || '含基础康复评估' }}</em>
            </span>
            <span>{{ pkg.days }} 天</span>
            <span>{{ pkg.roomTypeLevel }}</span>
            <span>{{ pkg.careMode }}</span>
            <span>{{ pkg.mealStandard || '四阶段' }}</span>
            <span class="right price">¥{{ money(pkg.price) }}</span>
            <span class="right">{{ Math.round(Number(pkg.depositRatio || 0) * 100) }}%</span>
            <span class="right"><router-link to="/visit" class="link">咨询</router-link></span>
          </div>
          <p v-if="!packages.length" class="empty">暂无上架套餐。</p>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { lang } from '../i18n'
import { siteApi } from '../api'

const rooms = ref<any[]>([])
const packages = ref<any[]>([])
const level = ref('')
const activeImage = reactive<Record<number, number>>({})

const levels = computed(() => ['', ...Array.from(new Set(rooms.value.map((r) => r.level).filter(Boolean)))])
const filteredRooms = computed(() => (level.value ? rooms.value.filter((r) => r.level === level.value) : rooms.value))

function money(v: any) {
  return Number(v || 0).toLocaleString('zh-CN')
}

onMounted(async () => {
  try {
    rooms.value = ((await siteApi.rooms()) as any) || []
    packages.value = ((await siteApi.packages()) as any) || []
  } catch {
    rooms.value = []
  }
  void lang.value
})
</script>

<style scoped>
.tabs { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 26px; }
.tabs button {
  border: 1px solid var(--line-strong); background: transparent; color: var(--ink-600);
  border-radius: 999px; padding: 7px 16px; font-size: 13px; cursor: pointer;
}
.tabs button.on { background: var(--brand-700); border-color: var(--brand-700); color: #fff; }

.room-list { display: flex; flex-direction: column; gap: 34px; }
.room-item { display: grid; grid-template-columns: 1fr 1fr; gap: 32px; background: var(--surface); border: 1px solid var(--line); border-radius: var(--radius-xl); overflow: hidden; }
.gallery { position: relative; background: var(--paper-2); min-height: 320px; }
.gallery > img { width: 100%; height: 100%; object-fit: cover; }
.gallery-ph { height: 100%; display: grid; place-items: center; color: var(--ink-400); font-size: 13px; }
.thumbs { position: absolute; left: 14px; right: 14px; bottom: 14px; display: flex; gap: 8px; }
.thumbs button { width: 52px; height: 40px; border-radius: 8px; overflow: hidden; border: 2px solid transparent; padding: 0; cursor: pointer; background: transparent; }
.thumbs button.on { border-color: #fff; box-shadow: 0 0 0 2px rgba(47, 139, 112, 0.6); }
.thumbs img { width: 100%; height: 100%; object-fit: cover; }
.room-info { padding: 28px 30px 30px 0; display: flex; flex-direction: column; }
.room-top { display: flex; align-items: center; gap: 10px; }
.room-top h2 { font-size: 24px; }
.specs { display: flex; gap: 16px; margin: 10px 0 0; font-size: 12.5px; color: var(--ink-500); }
.specs span { position: relative; padding-right: 16px; }
.specs span:not(:last-child)::after { content: ''; position: absolute; right: 6px; top: 8px; width: 3px; height: 3px; border-radius: 50%; background: var(--ink-300); }
.fac { margin: 10px 0 0; font-size: 12.5px; color: var(--ink-400); }
.desc { margin-top: 14px; font-size: 13.5px; color: var(--ink-500); }
.desc :deep(p) { margin: 0 0 8px; }
.room-actions { margin-top: auto; padding-top: 18px; display: flex; gap: 10px; flex-wrap: wrap; }

.pkg-table { background: var(--surface); border: 1px solid var(--line); border-radius: var(--radius-lg); overflow: hidden; }
.pkg-head-row, .pkg-row { display: grid; grid-template-columns: 2fr 0.7fr 1fr 1fr 0.9fr 0.9fr 0.7fr 0.6fr; gap: 12px; padding: 14px 20px; align-items: center; font-size: 12.5px; }
.pkg-head-row { background: var(--brand-50); color: var(--brand-800); font-weight: 600; font-size: 11.5px; letter-spacing: 0.04em; }
.pkg-row { border-top: 1px solid var(--line); color: var(--ink-600); }
.pkg-row .name { display: flex; flex-direction: column; gap: 2px; }
.pkg-row .name strong { font-size: 13.5px; color: var(--ink-900); font-weight: 550; }
.pkg-row .name em { font-style: normal; font-size: 11px; }
.price { font-family: var(--font-serif); font-size: 16px; color: var(--brand-800); }
.right { text-align: right; }
.link { color: var(--brand-700); font-size: 12.5px; }
.empty { padding: 34px 0; text-align: center; color: var(--ink-400); font-size: 13px; }

@media (max-width: 980px) {
  .room-item { grid-template-columns: 1fr; }
  .room-info { padding: 0 22px 24px; }
  .gallery { min-height: 240px; }
  .pkg-head-row { display: none; }
  .pkg-row { grid-template-columns: 1fr 1fr; gap: 6px 12px; }
}
</style>
