<template>
  <div class="page">
    <div class="page-title">
      <h2>消息配置</h2>
      <span class="hint">4 个消息通道 + 6 个短信渠道（单选启用）；未启用短信渠道时验证码降级写日志</span>
    </div>

    <div class="card">
      <h3 class="card-title">消息通道</h3>
      <div class="grid-2">
        <div v-for="channel in channels.message" :key="channel.channelCode" class="channel-row">
          <div>
            <strong>{{ channel.channelName }}</strong>
            <div class="muted mono">{{ channel.channelCode }}</div>
          </div>
          <el-switch
            :model-value="channel.enabled"
            @change="(val: boolean) => toggle(channel, val)"
          />
        </div>
      </div>
    </div>

    <div class="card">
      <h3 class="card-title">短信渠道（单选）</h3>
      <el-radio-group :model-value="enabledSms" @change="(val: any) => enableSms(String(val))">
        <div v-for="channel in channels.sms" :key="channel.channelCode" class="sms-row">
          <el-radio :value="channel.channelCode">
            <span>{{ channel.channelName }}</span>
            <span class="muted mono" style="margin-left: 8px">{{ channel.channelCode }}</span>
            <el-tag v-if="channel.configured" size="small" type="success" style="margin-left: 8px">已配置</el-tag>
          </el-radio>
        </div>
      </el-radio-group>
      <div class="toolbar" style="margin-top: 12px">
        <el-input v-model="testPhone" placeholder="测试手机号" style="width: 200px" />
        <el-button :disabled="!enabledSms" @click="sendTest">测试发送</el-button>
        <span class="muted">测试结果会写入服务端日志，真实渠道需接入网关凭证</span>
      </div>
    </div>

    <div class="card">
      <div class="toolbar" style="justify-content: space-between">
        <h3 class="card-title" style="margin: 0">消息模板</h3>
        <el-button size="small" type="primary" @click="openTemplate()">新增模板</el-button>
      </div>
      <el-table :data="templates" size="small" stripe style="margin-top: 12px">
        <el-table-column prop="triggerNode" label="触发节点" min-width="140" />
        <el-table-column prop="channel" label="通道" min-width="140" />
        <el-table-column prop="templateCode" label="模板编号" min-width="140" />
        <el-table-column prop="fieldMapping" label="字段映射" min-width="220" show-overflow-tooltip />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="primary" @click="openTemplate(row)">编辑</el-button>
            <el-button link type="danger" @click="removeTemplate(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!templates.length" description="尚未配置模板，同节点同通道只能存在一条" />
    </div>

    <el-dialog v-model="dialog" :title="form.id ? '编辑模板' : '新增模板'" width="520px">
      <el-form label-width="110px">
        <el-form-item label="触发节点" required>
          <el-select v-model="form.triggerNode" filterable allow-create style="width: 100%">
            <el-option v-for="node in nodes" :key="node" :label="node" :value="node" />
          </el-select>
        </el-form-item>
        <el-form-item label="通道" required>
          <el-select v-model="form.channel" style="width: 100%">
            <el-option v-for="c in channelOptions" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="模板编号" required><el-input v-model="form.templateCode" /></el-form-item>
        <el-form-item label="字段映射">
          <el-input v-model="form.fieldMapping" type="textarea" :rows="2" placeholder="thing1=客户姓名,time2=入住时间" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="submitTemplate">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  deleteTemplate, getChannels, listTemplates, saveChannel, saveTemplate, testSend
} from '../../api/modules'

const channels = ref<any>({ message: [], sms: [] })
const templates = ref<any[]>([])
const dialog = ref(false)
const testPhone = ref('13800000000')
const form = reactive<any>({ id: null, triggerNode: '', channel: '', templateCode: '', fieldMapping: '' })

const nodes = ['预约成功', '预约参观确认', '定金支付成功', '入住提醒', '护理异常', '尾款提醒', '出馆满意度回访', '42 天复查提醒']
const channelOptions = ['公众号模板消息', '小程序订阅消息', 'App 推送', '站内信', '短信']
const enabledSms = computed(() => channels.value.sms.find((c: any) => c.enabled)?.channelCode || '')

const load = async () => {
  const res: any = await getChannels()
  channels.value = res.data
  const tpl: any = await listTemplates()
  templates.value = tpl.data
}

const toggle = async (channel: any, enabled: boolean) => {
  await saveChannel({ channelCode: channel.channelCode, enabled })
  await load()
  ElMessage.success(enabled ? '通道已启用' : '通道已停用')
}

const enableSms = async (code: string) => {
  await saveChannel({ channelCode: code, enabled: true })
  await load()
  ElMessage.success('短信渠道已切换（其余渠道自动停用）')
}

const sendTest = async () => {
  const res: any = await testSend({ channelCode: enabledSms.value, phone: testPhone.value })
  ElMessage.success(res.data)
}

const openTemplate = (row?: any) => {
  Object.assign(form, row ? { ...row } : { id: null, triggerNode: nodes[0], channel: channelOptions[0], templateCode: '', fieldMapping: '' })
  dialog.value = true
}

const submitTemplate = async () => {
  if (!form.triggerNode || !form.channel || !form.templateCode) {
    ElMessage.warning('触发节点、通道与模板编号必填')
    return
  }
  await saveTemplate({
    triggerNode: form.triggerNode, channel: form.channel,
    templateCode: form.templateCode, fieldMapping: form.fieldMapping
  })
  dialog.value = false
  await load()
  ElMessage.success('模板已保存')
}

const removeTemplate = async (row: any) => {
  await ElMessageBox.confirm(`确认删除「${row.triggerNode}·${row.channel}」模板？`, '二次确认', { type: 'warning' })
  await deleteTemplate(row.id)
  await load()
}

onMounted(load)
</script>

<style scoped>
.channel-row { display: flex; align-items: center; justify-content: space-between; padding: var(--space-3); border: 1px solid var(--color-border); border-radius: var(--radius-md); }
.sms-row { padding: 8px 0; }
</style>
