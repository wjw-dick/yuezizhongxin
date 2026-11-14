<template>
  <div>
    <section class="page-hero">
      <div class="wrap">
        <span class="eyebrow">{{ eyebrow }}</span>
        <h1 class="serif">{{ title }}</h1>
        <p>{{ desc }}</p>
      </div>
    </section>

    <section class="section tight">
      <div class="wrap">
        <div v-if="loading" class="skeleton-grid">
          <div v-for="i in 4" :key="i" class="skeleton" />
        </div>

        <template v-else>
          <!-- 图文网格（康复 / 月子餐） -->
          <div v-if="variant === 'grid'" class="grid-cards">
            <article v-for="item in list" :key="item.id" class="g-card">
              <div v-if="item.cover" class="g-cover"><img :src="item.cover" :alt="item.title" loading="lazy" /></div>
              <div class="g-body">
                <h2 class="serif">{{ item.title }}</h2>
                <div class="prose" v-html="item.bodyHtml || '<p>内容待完善。</p>'" />
              </div>
            </article>
          </div>

          <!-- 团队 -->
          <div v-else-if="variant === 'team'" class="team-cards">
            <article v-for="item in list" :key="item.id" class="t-card">
              <div class="t-avatar">
                <img v-if="item.cover" :src="item.cover" :alt="item.title" loading="lazy" />
                <span v-else class="serif">{{ item.title.slice(0, 1) }}</span>
              </div>
              <h2>{{ item.title }}</h2>
              <div class="t-body" v-html="item.bodyHtml || '<p>资历介绍待完善。</p>'" />
            </article>
          </div>

          <!-- 案例 -->
          <div v-else-if="variant === 'case'" class="case-cards">
            <article v-for="item in list" :key="item.id" class="c-card">
              <div class="c-cover" v-if="item.cover"><img :src="item.cover" :alt="item.title" loading="lazy" /></div>
              <span class="quote">“</span>
              <h2 class="serif">{{ item.title }}</h2>
              <div class="prose" v-html="item.bodyHtml || '<p>案例内容待完善。</p>'" />
              <footer>
                <span class="tag">已获客户授权</span>
                <span class="muted">{{ (item.publishedTime || '').slice(0, 10) }}</span>
              </footer>
            </article>
          </div>

          <!-- 套餐式（月子餐阶段） -->
          <div v-else class="stage-cards">
            <article v-for="(item, i) in list" :key="item.id" class="s-card">
              <span class="step">0{{ i + 1 }}</span>
              <h2 class="serif">{{ item.title }}</h2>
              <div class="prose" v-html="item.bodyHtml || '<p>餐单说明待完善。</p>'" />
            </article>
          </div>

          <div v-if="!list.length" class="empty">
            <strong>该栏目内容正在准备中</strong>
            <p class="muted">运营后台「内容管理」发布后会自动出现在这里；也可以直接预约到店了解。</p>
            <router-link to="/visit" class="btn btn-primary" style="margin-top: 14px">预约参观</router-link>
          </div>
        </template>
      </div>
    </section>

    <section class="section alt tight">
      <div class="wrap cta-strip">
        <div>
          <h2 class="serif">{{ ctaTitle }}</h2>
          <p class="muted">{{ ctaDesc }}</p>
        </div>
        <router-link to="/visit" class="btn btn-primary">预约到店</router-link>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { lang } from '../i18n'
import { siteApi } from '../api'

const props = withDefaults(
  defineProps<{
    contentType: string
    eyebrow: string
    title: string
    desc: string
    variant?: 'grid' | 'team' | 'case' | 'stage'
    ctaTitle?: string
    ctaDesc?: string
  }>(),
  {
    variant: 'grid',
    ctaTitle: '想看看真实的房间和餐食？',
    ctaDesc: '预约到店可参观在售房型、试吃当日月子餐，并与护理主管当面沟通。'
  }
)

const list = ref<any[]>([])
const loading = ref(true)

onMounted(async () => {
  try {
    list.value = ((await siteApi.contents(props.contentType, lang.value)) as any) || []
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.grid-cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 24px; }
.g-card { background: var(--surface); border: 1px solid var(--line); border-radius: var(--radius-xl); overflow: hidden; }
.g-cover { aspect-ratio: 16 / 9; background: var(--paper-2); }
.g-cover img { width: 100%; height: 100%; object-fit: cover; }
.g-body { padding: 24px 26px 26px; }
.g-body h2 { font-size: 20px; margin-bottom: 10px; }
.g-body :deep(p) { font-size: 13.5px; }

.team-cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 22px; }
.t-card { background: var(--surface); border: 1px solid var(--line); border-radius: var(--radius-xl); padding: 28px 24px; text-align: center; }
.t-avatar { width: 92px; height: 92px; border-radius: 50%; overflow: hidden; margin: 0 auto 16px; background: var(--brand-50); display: grid; place-items: center; }
.t-avatar img { width: 100%; height: 100%; object-fit: cover; }
.t-avatar span { font-size: 32px; color: var(--brand-600); }
.t-card h2 { font-size: 16.5px; margin-bottom: 8px; }
.t-body :deep(p) { font-size: 12.5px; color: var(--ink-500); text-align: left; line-height: 1.85; }

.case-cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 24px; }
.c-card { background: var(--surface); border: 1px solid var(--line); border-radius: var(--radius-xl); padding: 26px; display: flex; flex-direction: column; }
.c-cover { border-radius: var(--radius-md); overflow: hidden; margin-bottom: 14px; aspect-ratio: 16 / 10; background: var(--paper-2); }
.c-cover img { width: 100%; height: 100%; object-fit: cover; }
.quote { font-family: var(--font-serif); font-size: 46px; line-height: 1; color: var(--brand-100); }
.c-card h2 { font-size: 18px; margin: 4px 0 10px; }
.c-card footer { margin-top: auto; padding-top: 16px; display: flex; align-items: center; justify-content: space-between; }

.stage-cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 22px; }
.s-card { background: var(--surface); border: 1px solid var(--line); border-radius: var(--radius-xl); padding: 26px; position: relative; }
.step { font-family: var(--font-serif); font-size: 30px; color: var(--brand-100); }
.s-card h2 { font-size: 19px; margin: 4px 0 10px; }

.skeleton-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 22px; }
.skeleton { height: 260px; border-radius: var(--radius-xl); background: linear-gradient(100deg, #f1eee6 20%, #f7f5ef 40%, #f1eee6 60%); background-size: 200% 100%; animation: shine 1.3s infinite; }
@keyframes shine { to { background-position: -200% 0; } }

.empty { text-align: center; padding: 60px 0; }
.empty strong { display: block; font-size: 16px; color: var(--ink-900); margin-bottom: 6px; font-family: var(--font-serif); }
.empty p { margin: 0; font-size: 13px; }

.cta-strip { display: flex; align-items: center; justify-content: space-between; gap: 24px; }
.cta-strip h2 { font-size: 22px; }
.cta-strip p { margin: 8px 0 0; font-size: 13px; }

@media (max-width: 760px) {
  .cta-strip { flex-direction: column; align-items: flex-start; }
}
</style>
