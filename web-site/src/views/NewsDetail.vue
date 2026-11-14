<template>
  <div>
    <section class="article-hero" :style="coverStyle">
      <div class="wrap">
        <router-link to="/news" class="back">← 返回资讯</router-link>
        <h1 class="serif">{{ article.title || '资讯详情' }}</h1>
        <p class="meta">
          <span>{{ (article.publishedTime || '').slice(0, 10) }}</span>
          <span v-if="article.seoKeywords">· {{ article.seoKeywords }}</span>
        </p>
      </div>
    </section>

    <section class="section tight">
      <div class="wrap article-wrap">
        <article class="article prose" v-html="article.bodyHtml || '<p>内容加载中或已被下线。</p>'" />
        <aside class="article-side">
          <div class="card">
            <div class="card-body">
              <h3 class="serif">想进一步了解？</h3>
              <p class="muted">留下联系方式，顾问会在 30 分钟内联系您安排参观。</p>
              <LeadForm compact source="官网-资讯" />
            </div>
          </div>
          <div class="card" v-if="related.length">
            <div class="card-body">
              <h3 class="serif">相关阅读</h3>
              <ul class="related">
                <li v-for="r in related" :key="r.id">
                  <router-link :to="`/news/${r.id}`">{{ r.title }}</router-link>
                </li>
              </ul>
            </div>
          </div>
        </aside>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { lang } from '../i18n'
import { siteApi } from '../api'
import LeadForm from '../components/LeadForm.vue'

const route = useRoute()
const article = ref<any>({})
const related = ref<any[]>([])

const coverStyle = computed(() =>
  article.value.cover
    ? { backgroundImage: `linear-gradient(180deg, rgba(20,42,34,0.72), rgba(20,42,34,0.86)), url(${article.value.cover})` }
    : {}
)

async function load(id: string) {
  try {
    article.value = ((await siteApi.content(id, lang.value)) as any) || {}
    document.title = `${article.value.seoTitle || article.value.title || '资讯'} · 月之孕月子中心`
    const list: any = await siteApi.contents('资讯', lang.value)
    related.value = (list || []).filter((x: any) => String(x.id) !== String(id)).slice(0, 4)
  } catch {
    article.value = {}
  }
}

watch(() => route.params.id, (id) => id && load(String(id)))
onMounted(() => load(String(route.params.id)))
</script>

<style scoped>
.article-hero { padding: 92px 0 44px; background: linear-gradient(135deg, #1e5344, #2f7d64); background-size: cover; background-position: center; color: #eef6f1; }
.back { font-size: 12.5px; color: #b6d3c6; }
.article-hero h1 { color: #f7fbf8; font-size: 32px; margin: 14px 0 10px; max-width: 24em; }
.meta { margin: 0; font-size: 12px; color: #b6d3c6; display: flex; gap: 10px; }
.article-wrap { display: grid; grid-template-columns: 1fr 340px; gap: 40px; align-items: start; }
.article { background: var(--surface); border: 1px solid var(--line); border-radius: var(--radius-xl); padding: 34px 38px; }
.article-side { display: flex; flex-direction: column; gap: 16px; position: sticky; top: 90px; }
.article-side h3 { font-size: 16px; margin-bottom: 8px; }
.article-side p { font-size: 12.5px; margin: 0 0 14px; }
.related { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 10px; }
.related a { font-size: 13px; color: var(--ink-700); }
.related a:hover { color: var(--brand-700); }
@media (max-width: 900px) {
  .article-wrap { grid-template-columns: 1fr; }
  .article { padding: 24px 20px; }
  .article-hero h1 { font-size: 24px; }
  .article-side { position: static; }
}
</style>
