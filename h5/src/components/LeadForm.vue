<script setup lang="ts">
import { reactive, ref } from 'vue'
import { api, track } from '../api'

const emit = defineEmits<{ (e: 'done', leadId: number): void }>()

const form = reactive({
  name: '',
  phone: '',
  dueDate: '',
  roomType: '',
  visitDate: '',
  visitSlot: '10:00-11:00',
  remark: '',
  agree: false,
  honeypot: ''
})
const roomTypes = ['标准月子房', '阳光月子房', '套房', '暂不确定']
const slots = ['10:00-11:00', '14:00-15:00', '16:00-17:00']
const errors = reactive<Record<string, string>>({})
const submitting = ref(false)
const done = ref(false)

function validate() {
  Object.keys(errors).forEach((k) => delete errors[k])
  if (!form.name.trim()) errors.name = '请填写您的称呼'
  if (!/^1[3-9]\d{9}$/.test(form.phone)) errors.phone = '请填写正确的 11 位手机号'
  if (!form.agree) errors.agree = '请勾选隐私授权后再提交'
  return Object.keys(errors).length === 0
}

async function submit() {
  if (form.honeypot) {
    // 简单反爬：隐藏字段被填写视为机器提交，直接丢弃
    return
  }
  if (!validate()) return
  submitting.value = true
  try {
    const lead = await api.lead({
      customerName: form.name.trim(),
      phone: form.phone.trim(),
      sourceCode: 'H5留资',
      intendRoomType: form.roomType || null,
      remark: form.remark || null
    })
    if (form.visitDate) {
      await api.visit({
        leadId: lead?.leadId || null,
        visitDate: form.visitDate,
        timeSlot: form.visitSlot,
        personCount: 2
      })
    }
    track('lead_submit', { hasVisit: !!form.visitDate })
    done.value = true
    emit('done', lead?.leadId || lead?.id || 0)
  } catch (e: any) {
    errors.phone = e?.message || '提交失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div v-if="done" class="card" style="text-align: center">
    <div style="font-size: 34px; color: #246a54">✓</div>
    <h3 style="margin: 8px 0 6px; color: #1f2421">预约已提交</h3>
    <p class="muted" style="margin: 0">
      顾问会在 30 分钟内与您电话确认参观时间，请留意 400 开头来电。
    </p>
    <p v-if="form.visitDate" class="muted" style="margin: 8px 0 0">
      参观时间：{{ form.visitDate }} {{ form.visitSlot }}
    </p>
  </div>

  <form v-else class="card" @submit.prevent="submit">
    <div class="field">
      <label>您的称呼</label>
      <input v-model="form.name" placeholder="如：王女士" maxlength="20" />
      <div v-if="errors.name" class="error">{{ errors.name }}</div>
    </div>

    <div class="field">
      <label>手机号</label>
      <input v-model="form.phone" inputmode="numeric" maxlength="11" placeholder="用于顾问联系与订单通知" />
      <div v-if="errors.phone" class="error">{{ errors.phone }}</div>
    </div>

    <div class="field">
      <label>预产期（选填）</label>
      <input v-model="form.dueDate" type="date" />
    </div>

    <div class="field">
      <label>意向房型</label>
      <div class="chips">
        <span
          v-for="rt in roomTypes"
          :key="rt"
          class="chip"
          :class="{ on: form.roomType === rt }"
          @click="form.roomType = form.roomType === rt ? '' : rt"
        >{{ rt }}</span>
      </div>
    </div>

    <div class="field">
      <label>到店参观日期（选填）</label>
      <input v-model="form.visitDate" type="date" :min="new Date().toISOString().slice(0, 10)" />
    </div>

    <div v-if="form.visitDate" class="field">
      <label>参观时段</label>
      <div class="chips">
        <span
          v-for="s in slots"
          :key="s"
          class="chip"
          :class="{ on: form.visitSlot === s }"
          @click="form.visitSlot = s"
        >{{ s }}</span>
      </div>
    </div>

    <div class="field">
      <label>备注（选填）</label>
      <textarea v-model="form.remark" maxlength="200" placeholder="如：想了解月子餐试吃、康复项目" />
    </div>

    <input v-model="form.honeypot" style="display: none" tabindex="-1" autocomplete="off" />

    <label class="row" style="align-items: flex-start; margin: 4px 0 14px">
      <input v-model="form.agree" type="checkbox" style="width: 18px; height: 18px; margin-top: 3px" />
      <span class="muted" style="font-size: 12px">
        我已阅读并同意《隐私政策》，同意门店使用上述信息与我联系；信息仅用于预约与服务，不会外传。
      </span>
    </label>
    <div v-if="errors.agree" class="error" style="margin: -8px 0 10px">{{ errors.agree }}</div>

    <button class="btn" type="submit" :disabled="submitting">
      {{ submitting ? '提交中…' : '免费预约参观' }}
    </button>
    <p class="muted" style="text-align: center; margin: 10px 0 0; font-size: 12px">
      到店可试吃月子餐、参观真实在住房间
    </p>
  </form>
</template>
