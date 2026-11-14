<template>
  <div>
    <section class="page-hero">
      <div class="wrap">
        <span class="eyebrow">LOCATIONS</span>
        <h1 class="serif">门店分布</h1>
        <p>所有门店均为直营，护理标准与餐食标准统一；可点击直接调起地图导航。</p>
      </div>
    </section>

    <section class="section tight">
      <div class="wrap store-layout">
        <div class="store-list">
          <article v-for="(store, i) in stores" :key="store.id || i" class="store-card">
            <div class="store-head">
              <h2 class="serif">{{ store.title }}</h2>
              <span class="tag">{{ i === 0 ? '总店' : '直营店' }}</span>
            </div>
            <p class="addr">{{ text(store.bodyHtml) || site.address || '地址由运营后台「系统参数 → 门店地址」配置' }}</p>
            <div class="store-actions">
              <a
                class="btn btn-ghost"
                :href="`https://uri.amap.com/search?keyword=${encodeURIComponent((text(store.bodyHtml) || site.address || store.title))}`"
                target="_blank"
                rel="noreferrer"
              >地图导航</a>
              <a v-if="site.phone" class="btn btn-primary" :href="`tel:${site.phone}`">电话咨询</a>
            </div>
          </article>

          <article v-if="!stores.length" class="store-card">
            <div class="store-head">
              <h2 class="serif">{{ site.name || '月之孕月子中心' }}</h2>
              <span class="tag">总店</span>
            </div>
            <p class="addr">{{ site.address || '地址待运营后台配置（系统参数 → 门店地址）' }}</p>
            <div class="store-actions">
              <a class="btn btn-ghost" :href="`https://uri.amap.com/search?keyword=${encodeURIComponent(site.address || site.name || '月子中心')}`" target="_blank" rel="noreferrer">地图导航</a>
              <a v-if="site.phone" class="btn btn-primary" :href="`tel:${site.phone}`">{{ site.phone }}</a>
            </div>
          </article>
        </div>

        <aside class="visit-side">
          <div class="card">
            <div class="card-body">
              <h3 class="serif">到店路线与停车</h3>
              <ul class="tips">
                <li><b>停车</b><span>地面车位 20 个，到店报「参观」可免费 2 小时</span></li>
                <li><b>公共交通</b><span>地铁站步行约 8 分钟，公交 3 条线路可达</span></li>
                <li><b>参观时长</b><span>约 60 分钟：房型 → 公共区域 → 试餐 → 方案沟通</span></li>
                <li><b>携带建议</b><span>产检本或预产期证明，方便顾问给出档期建议</span></li>
              </ul>
              <router-link to="/visit" class="btn btn-primary btn-block" style="margin-top: 16px">预约参观</router-link>
            </div>
          </div>

          <div class="card map-placeholder">
            <div class="map-inner">
              <span class="serif">MAP</span>
              <p class="muted">地图组件在生产环境接入高德/腾讯地图 Key；当前提供「地图导航」外链，避免使用非合规底图。</p>
            </div>
          </div>
        </aside>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { lang } from '../i18n'
import { siteApi } from '../api'

const stores = ref<any[]>([])
const site = ref<any>({})

function text(html: string) {
  return String(html || '').replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim()
}

onMounted(async () => {
  try {
    const home: any = await siteApi.home(lang.value)
    site.value = home?.site || {}
  } catch {
    site.value = {}
  }
  try {
    stores.value = ((await siteApi.contents('门店', lang.value)) as any) || []
  } catch {
    stores.value = []
  }
})
</script>

<style scoped>
.store-layout { display: grid; grid-template-columns: 1fr 360px; gap: 34px; align-items: start; }
.store-list { display: flex; flex-direction: column; gap: 20px; }
.store-card { background: var(--surface); border: 1px solid var(--line); border-radius: var(--radius-xl); padding: 26px 28px; }
.store-head { display: flex; align-items: center; gap: 12px; }
.store-head h2 { font-size: 21px; }
.addr { margin: 10px 0 0; font-size: 13.5px; color: var(--ink-500); line-height: 1.85; }
.store-actions { margin-top: 18px; display: flex; gap: 10px; }
.visit-side { display: flex; flex-direction: column; gap: 18px; }
.visit-side h3 { font-size: 17px; margin-bottom: 12px; }
.tips { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 12px; }
.tips li { display: flex; gap: 12px; align-items: baseline; }
.tips b { flex: 0 0 66px; font-size: 12.5px; color: var(--ink-900); }
.tips span { font-size: 12.5px; color: var(--ink-500); line-height: 1.75; }
.map-placeholder { background: var(--paper-2); }
.map-inner { padding: 40px 24px; text-align: center; }
.map-inner span { font-family: var(--font-serif); font-size: 28px; color: var(--brand-100); letter-spacing: 0.2em; }
.map-inner p { margin: 10px 0 0; font-size: 12px; line-height: 1.8; }
@media (max-width: 900px) { .store-layout { grid-template-columns: 1fr; } }
</style>
