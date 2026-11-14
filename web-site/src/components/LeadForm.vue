<template>
  <form class="lead-form" @submit.prevent="submit">
    <div class="row">
      <div class="field">
        <label>称呼 *</label>
        <input v-model="form.customerName" placeholder="如 王女士" />
      </div>
      <div class="field">
        <label>手机号 *</label>
        <input v-model="form.phone" inputmode="numeric" maxlength="11" placeholder="11 位手机号" />
      </div>
    </div>

    <div v-if="!compact" class="row">
      <div class="field">
        <label>预产期</label>
        <input v-model="form.dueDate" type="date" />
      </div>
      <div class="field">
        <label>意向房型</label>
        <select v-model="form.intendRoomType">
          <option value="">不限</option>
          <option v-for="room in rooms" :key="room.id" :value="room.name">{{ room.name }}</option>
        </select>
      </div>
    </div>

    <div v-if="!compact" class="row">
      <div class="field">
        <label>参观日期</label>
        <input v-model="visitForm.visitDate" type="date" />
      </div>
      <div class="field">
        <label>到店时段</label>
        <select v-model="visitForm.timeSlot">
          <option v-for="slot in slots" :key="slot" :value="slot">{{ slot }}</option>
        </select>
      </div>
    </div>

    <div class="field">
      <label>备注</label>
      <textarea v-model="form.remark" :rows="compact ? 2 : 3" placeholder="想了解的问题，比如套房档期、康复项目包含哪些" />
    </div>

    <label class="agree">
      <input v-model="agreed" type="checkbox" />
      <span>我已阅读并同意《隐私政策》，允许月子中心通过电话或微信与我联系</span>
    </label>

    <button class="btn btn-primary btn-block" type="submit" :disabled="loading">
      {{ loading ? '提交中…' : compact ? '预约参观' : '提交预约' }}
    </button>

    <p v-if="error" class="err">{{ error }}</p>
    <p v-if="success" class="ok">{{ success }}</p>
  </form>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { siteApi } from '../api'

const props = withDefaults(defineProps<{ compact?: boolean; source?: string; roomType?: string }>(), {
  compact: false,
  source: '官网'
})

const slots = ['09:00-10:30', '10:30-12:00', '14:00-15:30', '15:30-17:00', '17:00-18:30']
const rooms = ref<any[]>([])
const loading = ref(false)
const error = ref('')
const success = ref('')
const agreed = ref(true)

const form = reactive<any>({
  customerName: '',
  phone: '',
  dueDate: '',
  intendRoomType: props.roomType || '',
  remark: ''
})

const visitForm = reactive<any>({ visitDate: '', timeSlot: slots[0] })

function validate() {
  if (!form.customerName.trim()) return '请填写称呼'
  if (!/^1[3-9]\d{9}$/.test(form.phone)) return '请填写正确的 11 位手机号'
  if (!agreed.value) return '请先同意隐私政策'
  return ''
}

async function submit() {
  error.value = ''
  success.value = ''
  const msg = validate()
  if (msg) {
    error.value = msg
    return
  }
  loading.value = true
  try {
    const channel = new URLSearchParams(location.search).get('ch') || props.source
    const lead: any = await siteApi.lead({
      customerName: form.customerName,
      phone: form.phone,
      dueDate: form.dueDate || null,
      intendRoomType: form.intendRoomType || null,
      remark: `${form.remark || ''}${channel ? `（来源：${channel}）` : ''}`,
      sourceCode: channel
    })
    if (!props.compact && visitForm.visitDate) {
      try {
        await siteApi.visit({
          leadId: lead?.leadId || null,
          visitDate: visitForm.visitDate,
          timeSlot: visitForm.timeSlot,
          personCount: 2,
          feedback: `官网预约：${form.customerName}`
        })
      } catch {
        /* 参观预约失败不阻塞留资结果 */
      }
    }
    success.value = props.compact ? '已收到，顾问将在 30 分钟内联系您' : '预约成功，到店请出示预约信息'
    form.remark = ''
  } catch (e: any) {
    error.value = e?.message || '提交失败，请稍后再试或直接致电门店'
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  if (props.compact) return
  try {
    rooms.value = ((await siteApi.rooms()) as any) || []
  } catch {
    rooms.value = []
  }
})
</script>

<style scoped>
.lead-form { display: flex; flex-direction: column; gap: 12px; }
.row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.agree { display: flex; gap: 8px; align-items: flex-start; font-size: 11.5px; color: var(--ink-500); line-height: 1.6; }
.agree input { margin-top: 2px; }
.lead-form .btn { margin-top: 4px; }
.err { margin: 0; font-size: 12px; color: #b0503f; }
.ok { margin: 0; font-size: 12.5px; color: #2f7d55; background: #eef7f1; border-radius: 8px; padding: 9px 12px; }
@media (max-width: 560px) {
  .row { grid-template-columns: 1fr; }
}
</style>
