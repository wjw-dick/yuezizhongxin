<template>
  <div>
    <section class="page-hero">
      <div class="wrap">
        <span class="eyebrow">JOURNAL</span>
        <h1 class="serif">资讯</h1>
        <p>护理知识、门店动态与真实服务记录，均由运营后台内容管理发布。</p>
      </div>
    </section>

    <section class="section tight">
      <div class="wrap news-wrap">
        <div class="news-main">
          <router-link v-for="item in list" :key="item.id" :to="`/news/${item.id}`" class="news-item">
            <div class="news-cover">
              <img v-if="item.cover" :src="item.cover" :alt="item.title" loading="lazy" />
              <div v-else class="cover-ph" />
            </div>
            <div class="news-body">
              <span class="muted">{{ (item.publishedTime || '').slice(0, 10) }}</span>
              <h2 class="serif">{{ item.title }}</h2>
              <p v-html="excerpt(item.bodyHtml)" />
              <span class="more">阅读全文 →</span>
            </div>
          </router-link>

          <div v-if="!list.length" class="empty">
            <strong>暂无资讯</strong>
            <p class="muted">内容发布后自动展示，可先看看房型与套餐。</p>
            <router-link to="/rooms" class="btn btn-primary" style="margin-top: 14px">查看房型</router-link>
          </div>
        </div>

        <aside class="news-side">
          <div class="card">
            <div class="card-body">
              <h3 class="serif">常见问题</h3>
              <ul class="faq">
                <li v-for="(f, i) in faq" :key="i">
                  <button @click="openFaq = openFaq === i ? -1 : i">
                    <span>{{ f.title }}</span>
                    <em>{{ openFaq === i ? '−' : '+' }}</em>
                  </button>
                  <div v-if="openFaq === i" class="faq-body prose" v-html="f.bodyHtml" />
                </li>
                <li v-if="!faq.length" class="muted">常见问题待补充</li>
              </ul>
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

const list = ref<any[]>([])
const faq = ref<any[]>([])
const openFaq = ref(-1)

function excerpt(html: string) {
  const text = String(html || '').replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim()
  return text.length > 110 ? `${text.slice(0, 110)}…` : text
}

onMounted(async () => {
  try {
    list.value = ((await siteApi.contents('资讯', lang.value)) as any) || []
    faq.value = ((await siteApi.contents('常见问题', lang.value)) as any) || []
  } catch {
    list.value = []
  }
})
</script>

<style scoped>
.news-wrap { display: grid; grid-template-columns: 1fr 340px; gap: 36px; align-items: start; }
.news-main { display: flex; flex-direction: column; gap: 18px; }
.news-item { display: grid; grid-template-columns: 220px 1fr; gap: 20px; background: var(--surface); border: 1px solid var(--line); border-radius: var(--radius-lg); overflow: hidden; transition: box-shadow 0.2s ease, transform 0.2s ease; }
.news-item:hover { box-shadow: var(--shadow-md); transform: translateY(-2px); }
.news-cover { aspect-ratio: 4 / 3; background: var(--paper-2); }
.news-cover img { width: 100%; height: 100%; object-fit: cover; }
.cover-ph { height: 100%; background: linear-gradient(140deg, #eef5f0, #f6f1e7); }
.news-body { padding: 20px 22px 20px 0; display: flex; flex-direction: column; gap: 6px; }
.news-body .muted { font-size: 11.5px; }
.news-body h2 { font-size: 19px; }
.news-body p { margin: 0; font-size: 13px; color: var(--ink-500); line-height: 1.8; }
.more { margin-top: auto; font-size: 12.5px; color: var(--brand-700); }
.faq { list-style: none; margin: 0; padding: 0; }
.faq li { border-bottom: 1px dashed var(--line); }
.faq button { width: 100%; display: flex; justify-content: space-between; gap: 10px; align-items: center; background: transparent; border: 0; padding: 12px 0; font-size: 13px; color: var(--ink-800); cursor: pointer; text-align: left; }
.faq button em { font-style: normal; color: var(--brand-600); font-size: 15px; }
.faq-body { font-size: 12.5px; color: var(--ink-500); padding-bottom: 12px; }
.empty { padding: 50px 0; text-align: center; }
.empty strong { display: block; font-family: var(--font-serif); font-size: 16px; color: var(--ink-900); margin-bottom: 6px; }
.empty p { margin: 0; font-size: 13px; }
@media (max-width: 900px) {
  .news-wrap { grid-template-columns: 1fr; }
  .news-item { grid-template-columns: 1fr; }
  .news-body { padding: 16px 20px 20px; }
  .news-cover { aspect-ratio: 16 / 9; }
}
</style>
