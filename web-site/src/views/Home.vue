<template>
  <div class="home">
    <!-- 主视觉 -->
    <section class="hero">
      <div class="hero-media" :style="heroStyle" />
      <div class="hero-veil" />
      <div class="wrap hero-inner">
        <span class="hero-eyebrow">月之孕 · 母婴护理中心</span>
        <h1 class="serif">{{ hero.title || '把第一个月，交给懂得的人' }}</h1>
        <p class="hero-sub">
          {{ hero.summary || '24 小时一对一母婴护理、循证产后康复与阶段月子餐，从入馆评估到 42 天复查，全程有记录、有回顾。' }}
        </p>
        <div class="hero-actions">
          <router-link to="/visit" class="btn btn-primary">预约到店参观</router-link>
          <router-link to="/rooms" class="btn btn-ghost light">查看房型与套餐</router-link>
        </div>
        <div class="hero-stats">
          <div v-for="s in heroStats" :key="s.label">
            <strong>{{ s.value }}</strong>
            <span>{{ s.label }}</span>
          </div>
        </div>
      </div>
    </section>

    <!-- 品牌 -->
    <section class="section">
      <div class="wrap brand-grid">
        <div class="brand-text">
          <span class="eyebrow">BRAND STORY</span>
          <h2 class="serif">我们相信，产后第一个月<br />决定了一个家庭很久的状态</h2>
          <div class="brand-body" v-html="brand?.bodyHtml || defaultBrand" />
          <ul class="brand-points">
            <li><b>护理有记录</b><span>母婴体征、喂养与睡眠逐项记录，可回看曲线</span></li>
            <li><b>康复有评估</b><span>入馆评估 + 疗程卡，腹直肌与盆底肌变化可量化</span></li>
            <li><b>膳食有标准</b><span>分四阶段餐单，忌口与过敏原强校验</span></li>
          </ul>
        </div>
        <div class="brand-visual">
          <div class="visual-card">
            <img v-if="brand?.cover" :src="brand.cover" alt="品牌故事" loading="lazy" />
            <div v-else class="visual-ph">
              <span class="serif">月</span>
              <p>品牌故事配图待运营后台发布</p>
            </div>
          </div>
          <div class="badge-float">
            <strong>{{ stats.years }} 年</strong>
            <span>母婴护理经验</span>
          </div>
        </div>
      </div>
    </section>

    <!-- 房型与套餐 -->
    <section class="section alt">
      <div class="wrap">
        <div class="sec-head">
          <div>
            <span class="eyebrow">ROOMS & PACKAGES</span>
            <h2 class="serif">房型与套餐</h2>
            <p>从温馨单间到尊享套房，套餐按护理模式、康复项目与餐食标准区分，价格与定金比例透明公示。</p>
          </div>
          <router-link to="/rooms" class="more">全部房型 →</router-link>
        </div>

        <div class="room-grid">
          <article v-for="room in rooms.slice(0, 3)" :key="room.id" class="room-card">
            <div class="room-media">
              <img v-if="room.images?.length" :src="room.images[0]" :alt="room.name" loading="lazy" />
              <div v-else class="media-ph">{{ room.name }}</div>
              <span class="room-level">{{ room.level }}</span>
            </div>
            <div class="card-body">
              <h3 class="serif">{{ room.name }}</h3>
              <p class="room-meta">{{ room.area }}㎡ · {{ room.bedType }} · 可住 {{ room.maxGuest }} 人</p>
              <p class="room-fac">{{ room.facilities || '独立卫浴 / 家属陪护床 / 空气净化' }}</p>
            </div>
          </article>
          <p v-if="!rooms.length" class="empty">房型由运营后台「房型与房间」发布后展示。</p>
        </div>

        <div class="pkg-grid">
          <article v-for="pkg in packages.slice(0, 3)" :key="pkg.id" class="pkg-card">
            <div class="pkg-head">
              <strong class="serif">{{ pkg.name }}</strong>
              <span class="tag">{{ pkg.roomTypeLevel }}</span>
            </div>
            <p class="pkg-price"><em>¥</em>{{ money(pkg.price) }}<small>/ {{ pkg.days }} 天</small></p>
            <ul class="pkg-list">
              <li>护理模式：{{ pkg.careMode }}</li>
              <li>餐食标准：{{ pkg.mealStandard || '四阶段月子餐' }}</li>
              <li>含康复项目：{{ pkg.rehabItems || '基础评估与盆底修复' }}</li>
              <li>定金比例：{{ Math.round(Number(pkg.depositRatio || 0) * 100) }}%</li>
            </ul>
            <router-link to="/visit" class="btn btn-ghost btn-block">咨询该套餐</router-link>
          </article>
          <p v-if="!packages.length" class="empty">套餐由运营后台「套餐与商品」上架后展示。</p>
        </div>
      </div>
    </section>

    <!-- 康复 -->
    <section class="section">
      <div class="wrap">
        <div class="sec-head">
          <div>
            <span class="eyebrow">POSTPARTUM REHAB</span>
            <h2 class="serif">产后康复</h2>
            <p>入馆 48 小时内完成首次评估，疗程卡按次核销，康复室与治疗师双重排程校验。</p>
          </div>
          <router-link to="/rehab" class="more">康复详情 →</router-link>
        </div>
        <div class="rehab-grid">
          <article v-for="item in rehabItems.slice(0, 4)" :key="item.id" class="rehab-card">
            <span class="rehab-dot" />
            <h3>{{ item.title }}</h3>
            <p v-html="excerpt(item.bodyHtml, 92)" />
          </article>
          <div v-if="!rehabItems.length" class="rehab-card default">
            <span class="rehab-dot" />
            <h3>盆底肌修复</h3>
            <p>生物反馈 + 电刺激，按疗程评估肌力分级变化。</p>
          </div>
          <div v-if="!rehabItems.length" class="rehab-card default">
            <span class="rehab-dot" />
            <h3>腹直肌闭合</h3>
            <p>手法与呼吸训练结合，逐周测量分离度。</p>
          </div>
          <div v-if="!rehabItems.length" class="rehab-card default">
            <span class="rehab-dot" />
            <h3>体态与疼痛管理</h3>
            <p>针对腰痛、圆肩与骨盆前倾的矫正训练。</p>
          </div>
        </div>
      </div>
    </section>

    <!-- 月子餐 -->
    <section class="section alt">
      <div class="wrap meal-wrap">
        <div class="meal-text">
          <span class="eyebrow">MEAL PLANS</span>
          <h2 class="serif">月子餐，是每天六次的照顾</h2>
          <p>
            四个阶段循序渐进：排恶露、修复、滋补、调养。每份餐单生成前都会校验忌口与过敏原，
            命中冲突时直接拦截并提示替换方案。
          </p>
          <ul class="meal-stages">
            <li v-for="s in mealStages" :key="s.name">
              <b>{{ s.name }}</b>
              <span>{{ s.desc }}</span>
            </li>
          </ul>
          <router-link to="/meals" class="btn btn-ghost">查看阶段餐单</router-link>
        </div>
        <div class="meal-visual">
          <img v-if="mealCover" :src="mealCover" alt="月子餐" loading="lazy" />
          <div v-else class="meal-ph">
            <div v-for="i in 6" :key="i" class="meal-chip">{{ ['早餐','早加餐','午餐','午加餐','晚餐','晚加餐'][i-1] }}</div>
          </div>
        </div>
      </div>
    </section>

    <!-- 团队 -->
    <section class="section">
      <div class="wrap">
        <div class="sec-head">
          <div>
            <span class="eyebrow">CARE TEAM</span>
            <h2 class="serif">护理团队</h2>
            <p>护理主管、护理师、康复治疗师与营养师组成固定小组，一对一配比排班。</p>
          </div>
          <router-link to="/team" class="more">团队介绍 →</router-link>
        </div>
        <div class="team-grid">
          <article v-for="member in team" :key="member.id" class="team-card">
            <div class="team-avatar">
              <img v-if="member.cover" :src="member.cover" :alt="member.title" loading="lazy" />
              <span v-else class="serif">{{ member.title.slice(0, 1) }}</span>
            </div>
            <strong>{{ member.title }}</strong>
            <p v-html="excerpt(member.bodyHtml, 64)" />
          </article>
          <p v-if="!team.length" class="empty">团队信息由运营后台「内容管理 → 团队」发布后展示。</p>
        </div>
      </div>
    </section>

    <!-- 案例 -->
    <section class="section alt">
      <div class="wrap">
        <div class="sec-head">
          <div>
            <span class="eyebrow">STORIES</span>
            <h2 class="serif">案例口碑</h2>
            <p>所有案例均已取得客户书面授权后发布，可按类型筛选。</p>
          </div>
          <router-link to="/cases" class="more">更多案例 →</router-link>
        </div>
        <div class="case-grid">
          <article v-for="c in cases" :key="c.id" class="case-card">
            <div class="quote">“</div>
            <p v-html="excerpt(c.bodyHtml, 110)" />
            <footer>
              <span class="tag bronze">{{ c.title }}</span>
            </footer>
          </article>
          <p v-if="!cases.length" class="empty">案例发布时会校验客户授权标记。</p>
        </div>
      </div>
    </section>

    <!-- 资讯 + 预约 -->
    <section class="section">
      <div class="wrap news-cta">
        <div class="news-col">
          <div class="sec-head">
            <div>
              <span class="eyebrow">JOURNAL</span>
              <h2 class="serif">资讯</h2>
            </div>
            <router-link to="/news" class="more">全部 →</router-link>
          </div>
          <ul class="news-list">
            <li v-for="n in news" :key="n.id">
              <router-link :to="`/news/${n.id}`">
                <strong>{{ n.title }}</strong>
                <span class="muted">{{ (n.publishedTime || '').slice(0, 10) }}</span>
              </router-link>
            </li>
            <li v-if="!news.length" class="muted">暂无资讯</li>
          </ul>
        </div>

        <div class="cta-card">
          <h3 class="serif">先来看看房间，再决定</h3>
          <p class="muted">填写信息后顾问会在 30 分钟内联系，安排到店参观与试餐。</p>
          <LeadForm compact source="官网" />
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { lang } from '../i18n'
import { siteApi } from '../api'
import LeadForm from '../components/LeadForm.vue'

const home = ref<any>({})
const rehabItems = ref<any[]>([])
const mealContents = ref<any[]>([])

const hero = computed(() => home.value.banners?.[0] || {})
const brand = computed(() => home.value.brand)
const rooms = computed(() => home.value.roomTypes || [])
const packages = computed(() => home.value.packages || [])
const team = computed(() => home.value.team || [])
const cases = computed(() => home.value.cases || [])
const news = computed(() => home.value.news || [])

const heroStyle = computed(() => {
  const cover = hero.value.cover || brand.value?.cover
  return cover
    ? { backgroundImage: `url(${cover})` }
    : { background: 'linear-gradient(135deg, #1e5344 0%, #2f7d64 46%, #6ba98f 100%)' }
})

const heroStats = computed(() => [
  { value: `${rooms.value.length || 3} 种`, label: '房型可选' },
  { value: `${packages.value.length || 3} 档`, label: '护理套餐' },
  { value: '24h', label: '一对一护理' },
  { value: '42 天', label: '复查跟踪' }
])

const stats = ref({ years: 8 })

const mealCover = computed(() => mealContents.value[0]?.cover || '')

const mealStages = [
  { name: '第一阶段 · 排', desc: '产后 0-7 天，清淡易消化，促恶露排出' },
  { name: '第二阶段 · 修', desc: '8-14 天，补充优质蛋白，帮助伤口修复' },
  { name: '第三阶段 · 补', desc: '15-28 天，滋补气血，兼顾乳汁质量' },
  { name: '第四阶段 · 调', desc: '29 天以后，调理体质，平稳过渡出馆' }
]

const defaultBrand = `<p>月之孕从一间产房开始。我们发现，新手家庭最需要的不是更多建议，
而是有人把每一件小事都记下来、做到位：几点喂的奶、体温有没有波动、伤口恢复得怎么样。</p>
<p>于是我们把护理流程拆成可执行、可核查的动作，让专业变成日常。</p>`

function money(value: any) {
  return Number(value || 0).toLocaleString('zh-CN')
}

function excerpt(html: string, len: number) {
  const text = String(html || '').replace(/<[^>]+>/g, '').replace(/\s+/g, ' ').trim()
  return text.length > len ? `${text.slice(0, len)}…` : text
}

onMounted(async () => {
  try {
    home.value = (await siteApi.home(lang.value)) || {}
  } catch {
    home.value = {}
  }
  try {
    rehabItems.value = ((await siteApi.contents('康复项目', lang.value)) as any) || []
    mealContents.value = ((await siteApi.contents('月子餐', lang.value)) as any) || []
  } catch {
    rehabItems.value = []
  }
})
</script>

<style scoped>
/* ---------------- hero ---------------- */
.hero { position: relative; min-height: 640px; display: flex; align-items: center; overflow: hidden; }
.hero-media { position: absolute; inset: 0; background-size: cover; background-position: center; }
.hero-veil {
  position: absolute; inset: 0;
  background: linear-gradient(100deg, rgba(16, 38, 31, 0.86) 0%, rgba(22, 60, 48, 0.66) 48%, rgba(26, 70, 56, 0.3) 100%);
}
.hero-inner { position: relative; z-index: 1; padding-top: 60px; padding-bottom: 60px; color: #eef6f1; }
.hero-eyebrow { font-size: 11.5px; letter-spacing: 0.22em; text-transform: uppercase; color: #a8d3c1; }
.hero h1 { color: #f7fbf8; font-size: 46px; line-height: 1.22; margin: 16px 0 18px; max-width: 15em; letter-spacing: -0.01em; }
.hero-sub { max-width: 34em; color: #cfe3d8; font-size: 15px; line-height: 1.85; margin: 0 0 30px; }
.hero-actions { display: flex; gap: 12px; flex-wrap: wrap; }
.btn-ghost.light { border-color: rgba(255, 255, 255, 0.4); color: #eef6f1; }
.btn-ghost.light:hover { border-color: #fff; color: #fff; background: rgba(255, 255, 255, 0.08); }
.hero-stats { display: flex; gap: 46px; margin-top: 54px; padding-top: 26px; border-top: 1px solid rgba(255, 255, 255, 0.18); flex-wrap: wrap; }
.hero-stats div { display: flex; flex-direction: column; gap: 3px; }
.hero-stats strong { font-size: 25px; color: #fff; font-family: var(--font-serif); font-variant-numeric: tabular-nums; }
.hero-stats span { font-size: 11.5px; color: #a8c4b7; letter-spacing: 0.06em; }

/* ---------------- brand ---------------- */
.brand-grid { display: grid; grid-template-columns: 1.15fr 0.85fr; gap: 60px; align-items: center; }
.eyebrow { display: inline-block; font-size: 11.5px; letter-spacing: 0.18em; color: var(--bronze); margin-bottom: 12px; }
.brand-text h2 { font-size: 33px; line-height: 1.4; }
.brand-body { margin-top: 18px; color: var(--ink-500); font-size: 14.5px; line-height: 1.9; }
.brand-body :deep(p) { margin: 0 0 12px; }
.brand-points { list-style: none; margin: 26px 0 0; padding: 0; display: flex; flex-direction: column; gap: 14px; }
.brand-points li { display: flex; gap: 14px; align-items: baseline; padding-bottom: 13px; border-bottom: 1px dashed var(--line); }
.brand-points b { flex: 0 0 92px; font-size: 13.5px; color: var(--ink-900); }
.brand-points span { font-size: 13px; color: var(--ink-500); }

.brand-visual { position: relative; }
.visual-card { border-radius: var(--radius-xl); overflow: hidden; box-shadow: var(--shadow-lg); background: var(--paper-2); aspect-ratio: 4 / 5; }
.visual-card img { width: 100%; height: 100%; object-fit: cover; }
.visual-ph { height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; color: var(--ink-400); }
.visual-ph span { font-size: 62px; color: var(--brand-100); }
.visual-ph p { font-size: 12.5px; margin: 0; }
.badge-float {
  position: absolute; right: -18px; bottom: 28px; background: var(--surface); border: 1px solid var(--line);
  border-radius: var(--radius-lg); padding: 14px 20px; box-shadow: var(--shadow-md); text-align: center;
}
.badge-float strong { display: block; font-family: var(--font-serif); font-size: 21px; color: var(--brand-700); }
.badge-float span { font-size: 11.5px; color: var(--ink-400); }

/* ---------------- rooms ---------------- */
.room-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 22px; }
.room-card { background: var(--surface); border: 1px solid var(--line); border-radius: var(--radius-lg); overflow: hidden; transition: box-shadow 0.2s ease, transform 0.2s ease; }
.room-card:hover { box-shadow: var(--shadow-md); transform: translateY(-3px); }
.room-media { position: relative; aspect-ratio: 4 / 3; background: var(--paper-2); }
.room-media img { width: 100%; height: 100%; object-fit: cover; }
.media-ph { height: 100%; display: grid; place-items: center; color: var(--ink-400); font-size: 13px; }
.room-level { position: absolute; left: 12px; top: 12px; background: rgba(255, 255, 255, 0.92); border-radius: 999px; padding: 3px 11px; font-size: 11.5px; color: var(--brand-700); }
.room-card h3 { font-size: 19px; margin-bottom: 6px; }
.room-meta { margin: 0; font-size: 12.5px; color: var(--ink-500); }
.room-fac { margin: 8px 0 0; font-size: 12.5px; color: var(--ink-400); line-height: 1.7; }

.pkg-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 22px; margin-top: 26px; }
.pkg-card { background: var(--surface); border: 1px solid var(--line); border-radius: var(--radius-lg); padding: 22px 24px; display: flex; flex-direction: column; gap: 12px; }
.pkg-head { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.pkg-head strong { font-size: 17px; }
.pkg-price { margin: 0; font-family: var(--font-serif); font-size: 30px; color: var(--brand-800); font-variant-numeric: tabular-nums; }
.pkg-price em { font-style: normal; font-size: 15px; margin-right: 3px; }
.pkg-price small { font-size: 12px; color: var(--ink-400); margin-left: 6px; font-family: var(--font-sans); }
.pkg-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 7px; font-size: 12.5px; color: var(--ink-500); }
.pkg-list li { padding-left: 14px; position: relative; }
.pkg-list li::before { content: ''; position: absolute; left: 0; top: 9px; width: 5px; height: 5px; border-radius: 50%; background: var(--brand-500); }

/* ---------------- rehab ---------------- */
.rehab-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(230px, 1fr)); gap: 18px; }
.rehab-card { background: var(--surface); border: 1px solid var(--line); border-radius: var(--radius-lg); padding: 22px; }
.rehab-dot { display: block; width: 9px; height: 9px; border-radius: 50%; background: var(--bronze); margin-bottom: 14px; }
.rehab-card h3 { font-size: 16px; margin-bottom: 8px; }
.rehab-card p { margin: 0; font-size: 12.5px; color: var(--ink-500); line-height: 1.8; }

/* ---------------- meals ---------------- */
.meal-wrap { display: grid; grid-template-columns: 1fr 0.85fr; gap: 56px; align-items: center; }
.meal-text h2 { font-size: 31px; }
.meal-text > p { color: var(--ink-500); font-size: 14.5px; line-height: 1.9; margin: 16px 0 22px; }
.meal-stages { list-style: none; margin: 0 0 26px; padding: 0; display: flex; flex-direction: column; gap: 12px; }
.meal-stages li { display: flex; gap: 14px; align-items: baseline; }
.meal-stages b { flex: 0 0 112px; font-size: 13px; color: var(--brand-800); }
.meal-stages span { font-size: 12.5px; color: var(--ink-500); }
.meal-visual { border-radius: var(--radius-xl); overflow: hidden; background: var(--surface); border: 1px solid var(--line); aspect-ratio: 4 / 5; }
.meal-visual img { width: 100%; height: 100%; object-fit: cover; }
.meal-ph { height: 100%; display: grid; grid-template-columns: 1fr 1fr; gap: 12px; padding: 22px; background: linear-gradient(150deg, #f6f1e7, #eef5f0); }
.meal-chip { display: grid; place-items: center; border-radius: var(--radius-md); background: rgba(255, 255, 255, 0.8); border: 1px solid var(--line); font-size: 13px; color: var(--ink-700); font-family: var(--font-serif); }

/* ---------------- team ---------------- */
.team-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; }
.team-card { background: var(--surface); border: 1px solid var(--line); border-radius: var(--radius-lg); padding: 24px; text-align: center; }
.team-avatar { width: 76px; height: 76px; border-radius: 50%; margin: 0 auto 14px; overflow: hidden; background: var(--brand-50); display: grid; place-items: center; }
.team-avatar img { width: 100%; height: 100%; object-fit: cover; }
.team-avatar span { font-size: 28px; color: var(--brand-600); }
.team-card strong { display: block; font-size: 14.5px; color: var(--ink-900); margin-bottom: 6px; }
.team-card p { margin: 0; font-size: 12.5px; color: var(--ink-500); line-height: 1.75; }

/* ---------------- cases ---------------- */
.case-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 20px; }
.case-card { background: var(--surface); border: 1px solid var(--line); border-radius: var(--radius-lg); padding: 24px; position: relative; }
.quote { font-family: var(--font-serif); font-size: 44px; line-height: 1; color: var(--brand-100); }
.case-card p { margin: 6px 0 18px; font-size: 13.5px; color: var(--ink-700); line-height: 1.9; }
.case-card footer { display: flex; justify-content: flex-end; }

/* ---------------- news + cta ---------------- */
.news-cta { display: grid; grid-template-columns: 1fr 420px; gap: 56px; align-items: start; }
.news-list { list-style: none; margin: 0; padding: 0; }
.news-list li { border-bottom: 1px dashed var(--line); }
.news-list a { display: flex; align-items: baseline; justify-content: space-between; gap: 20px; padding: 16px 0; }
.news-list strong { font-size: 14px; color: var(--ink-900); font-weight: 500; }
.news-list a:hover strong { color: var(--brand-700); }
.news-list span { font-size: 11.5px; white-space: nowrap; }

.cta-card { background: var(--surface); border: 1px solid var(--line); border-radius: var(--radius-xl); padding: 28px; box-shadow: var(--shadow-md); }
.cta-card h3 { font-size: 20px; margin-bottom: 8px; }
.cta-card > p { margin: 0 0 18px; font-size: 12.5px; }

.empty { grid-column: 1 / -1; text-align: center; color: var(--ink-400); font-size: 13px; padding: 30px 0; }

@media (max-width: 1000px) {
  .hero h1 { font-size: 34px; }
  .brand-grid, .meal-wrap, .news-cta { grid-template-columns: 1fr; gap: 36px; }
  .hero-stats { gap: 28px; margin-top: 36px; }
  .cta-card { padding: 22px; }
}
</style>
