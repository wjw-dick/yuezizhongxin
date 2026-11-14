<script setup lang="ts">
import { ref } from 'vue'
import LeadForm from '../components/LeadForm.vue'

const steps = [
  { title: '线上预约', desc: '选择到店日期与时段，30 分钟内顾问回电确认' },
  { title: '到店参观', desc: '看真实在住房间、护理站与月子餐后厨' },
  { title: '试吃与评估', desc: '试吃当日在供月子餐，做一次产后康复评估' },
  { title: '定制方案', desc: '按预产期与预算匹配房型、套餐与康复项目' },
  { title: '签约锁定', desc: '支付定金锁定档期，入馆前 7 天确认细节' }
]
const faq = ref([
  { q: '可以只参观不下单吗？', a: '可以。参观与试吃均免费，不签约也不产生任何费用。' },
  { q: '家属可以一起住吗？', a: '可以，套餐含 1 位陪护床位，额外陪护需按门店标准加收。' },
  { q: '临时改期怎么办？', a: '入馆前可免费改期一次，具体以合同约定期限为准。' }
])
const open = ref<number>(-1)
</script>

<template>
  <div class="page">
    <h2 style="margin: 18px 0 4px; font-size: 20px; color: #1f2421">预约参观</h2>
    <p class="muted" style="margin: 0 0 16px">
      填写信息后顾问会电话确认；到店可试吃月子餐并参观真实在住房间。
    </p>

    <LeadForm />

    <section class="section">
      <div class="section-head"><h2>到店流程</h2><span>约 60 分钟</span></div>
      <div class="card">
        <div v-for="(step, index) in steps" :key="step.title" class="row" style="align-items: flex-start; padding: 10px 0">
          <div
            style="width: 26px; height: 26px; border-radius: 50%; background: #eef7f2; color: #246a54;
                   display: flex; align-items: center; justify-content: center; font-size: 13px; flex-shrink: 0"
          >{{ index + 1 }}</div>
          <div class="grow">
            <div style="font-size: 14px; color: #1f2421">{{ step.title }}</div>
            <div class="muted" style="font-size: 12px">{{ step.desc }}</div>
          </div>
        </div>
      </div>
    </section>

    <section class="section">
      <div class="section-head"><h2>常见问题</h2></div>
      <div class="card">
        <div v-for="(item, index) in faq" :key="item.q" style="padding: 10px 0; border-bottom: 1px solid var(--line)">
          <div class="between" @click="open = open === index ? -1 : index">
            <span style="font-size: 14px; color: #1f2421">{{ item.q }}</span>
            <span class="muted">{{ open === index ? '−' : '+' }}</span>
          </div>
          <p v-if="open === index" class="muted" style="margin: 6px 0 0; font-size: 13px">{{ item.a }}</p>
        </div>
      </div>
    </section>
  </div>
</template>
