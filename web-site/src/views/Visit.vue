<template>
  <div>
    <section class="page-hero">
      <div class="wrap">
        <span class="eyebrow">BOOK A VISIT</span>
        <h1 class="serif">预约参观</h1>
        <p>填写信息后顾问会在 30 分钟内联系确认时段；到店可参观在售房型、试吃当日月子餐，并与护理主管当面沟通。</p>
      </div>
    </section>

    <section class="section tight">
      <div class="wrap visit-wrap">
        <div class="form-card">
          <h2 class="serif">预约信息</h2>
          <p class="muted">带 * 为必填项。我们仅在服务范围内使用您的信息。</p>
          <LeadForm :room-type="roomType" source="官网-预约页" />
        </div>

        <aside class="visit-aside">
          <div class="card">
            <div class="card-body">
              <h3 class="serif">到店流程</h3>
              <ol class="steps">
                <li v-for="(s, i) in steps" :key="i">
                  <span class="num">0{{ i + 1 }}</span>
                  <div>
                    <strong>{{ s.title }}</strong>
                    <p class="muted">{{ s.desc }}</p>
                  </div>
                </li>
              </ol>
            </div>
          </div>

          <div class="card">
            <div class="card-body">
              <h3 class="serif">门店信息</h3>
              <p class="muted info-line">{{ site.address || '地址待配置' }}</p>
              <p class="muted info-line">电话：{{ site.phone || '400-000-0000' }}</p>
              <p class="muted info-line">营业时间：09:00 - 20:00（护理 24 小时值班）</p>
              <div class="aside-actions">
                <a v-if="site.phone" class="btn btn-ghost" :href="`tel:${site.phone}`">电话咨询</a>
                <router-link to="/stores" class="btn btn-ghost">门店分布</router-link>
              </div>
            </div>
          </div>

          <div class="card qr-card" v-if="entries.length">
            <div class="card-body">
              <h3 class="serif">小程序 / App</h3>
              <div class="qr-row">
                <div v-for="e in entries.slice(0, 2)" :key="e.id" class="qr">
                  <img v-if="e.imageKey" :src="e.imageKey" :alt="e.name" loading="lazy" />
                  <span v-else class="qr-ph">未上传</span>
                  <em>{{ e.name }}</em>
                </div>
              </div>
            </div>
          </div>
        </aside>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { lang } from '../i18n'
import { siteApi } from '../api'
import LeadForm from '../components/LeadForm.vue'

const route = useRoute()
const site = ref<any>({})
const entries = ref<any[]>([])
const roomType = computed(() => String(route.query.room || ''))

const steps = [
  { title: '线上预约', desc: '提交称呼、手机号与期望时段，30 分钟内电话确认' },
  { title: '到店登记', desc: '前台核对预约信息，测量体温并做手部消毒' },
  { title: '房型参观', desc: '按在售房型带看，讲解采光、隔音与陪护配置' },
  { title: '试餐与沟通', desc: '试吃当日月子餐，护理主管介绍护理与康复方案' },
  { title: '方案与档期', desc: '确认房型档期、套餐与定金比例，可当场锁档' }
]

onMounted(async () => {
  try {
    const home: any = await siteApi.home(lang.value)
    site.value = home?.site || {}
  } catch {
    site.value = {}
  }
  try {
    entries.value = ((await siteApi.entries()) as any) || []
  } catch {
    entries.value = []
  }
})
</script>

<style scoped>
.visit-wrap { display: grid; grid-template-columns: 1fr 360px; gap: 34px; align-items: start; }
.form-card { background: var(--surface); border: 1px solid var(--line); border-radius: var(--radius-xl); padding: 30px 32px; box-shadow: var(--shadow-sm); }
.form-card h2 { font-size: 21px; margin-bottom: 6px; }
.form-card > p { margin: 0 0 20px; font-size: 12.5px; }
.visit-aside { display: flex; flex-direction: column; gap: 16px; }
.visit-aside h3 { font-size: 16.5px; margin-bottom: 12px; }
.steps { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 14px; }
.steps li { display: flex; gap: 12px; }
.steps .num { font-family: var(--font-serif); font-size: 18px; color: var(--brand-100); line-height: 1.3; }
.steps strong { font-size: 13px; color: var(--ink-900); }
.steps p { margin: 2px 0 0; font-size: 12px; line-height: 1.7; }
.info-line { margin: 0 0 8px; font-size: 12.5px; }
.aside-actions { display: flex; gap: 10px; margin-top: 14px; }
.aside-actions .btn { height: 38px; font-size: 12.5px; padding: 0 16px; }
.qr-row { display: flex; gap: 16px; }
.qr { display: flex; flex-direction: column; align-items: center; gap: 6px; }
.qr img { width: 96px; height: 96px; object-fit: contain; border: 1px solid var(--line); border-radius: 10px; }
.qr-ph { width: 96px; height: 96px; display: grid; place-items: center; background: var(--paper-2); border-radius: 10px; font-size: 11px; color: var(--ink-400); }
.qr em { font-style: normal; font-size: 11px; color: var(--ink-500); }
@media (max-width: 900px) {
  .visit-wrap { grid-template-columns: 1fr; }
  .form-card { padding: 22px; }
}
</style>
