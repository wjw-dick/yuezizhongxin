<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>会员与营销</h1>
        <p class="sub">UnionID 会员档案、标签分群、积分与优惠券、储值规则公示、转介绍奖励结算与分群触达</p>
      </div>
      <div class="actions">
        <el-button @click="loadAll"><Icon name="refresh" :size="15" /><span style="margin-left: 6px">刷新</span></el-button>
        <el-button type="primary" @click="openTouch">
          <Icon name="bell" :size="15" /><span style="margin-left: 6px">分群触达</span>
        </el-button>
      </div>
    </div>

    <div class="stat-row">
      <div v-for="item in stats" :key="item.label" class="stat">
        <span class="stat-label">{{ item.label }}</span>
        <strong class="stat-value">{{ item.value }}</strong>
        <em class="stat-hint">{{ item.hint }}</em>
      </div>
    </div>

    <el-tabs v-model="tab" class="biz-tabs" @tab-change="onTab">
      <el-tab-pane label="会员档案" name="member">
        <div class="card flush">
          <div class="toolbar filter-bar">
            <el-input v-model="keyword" placeholder="昵称 / 手机号" clearable size="small" style="width: 200px" @keyup.enter="loadMembers" @clear="loadMembers" />
            <el-select v-model="tagFilter" placeholder="按标签筛选" clearable size="small" style="width: 150px" @change="loadMembers">
              <el-option v-for="t in tags" :key="t.id" :label="t.name" :value="t.id" />
            </el-select>
            <el-button size="small" @click="loadMembers">查询</el-button>
            <span class="spacer" />
            <el-button size="small" type="primary" @click="openMember()">新增会员</el-button>
          </div>
          <el-table v-loading="loading" :data="members" size="small" stripe @row-click="openDetail">
            <el-table-column label="会员" min-width="180">
              <template #default="{ row }">
                <div class="member-cell">
                  <span class="avatar">{{ (row.nickname || '客').slice(0, 1) }}</span>
                  <div>
                    <strong>{{ row.nickname || '未命名' }}</strong>
                    <em class="mono">{{ maskPhone(row.phone) }}</em>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="source" label="来源" width="100" />
            <el-table-column prop="points" label="积分" width="100" align="right" />
            <el-table-column label="储值余额" width="130" align="right">
              <template #default="{ row }"><span class="money">¥{{ money(row.balance) }}</span></template>
            </el-table-column>
            <el-table-column prop="createdTime" label="注册时间" min-width="160" />
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click.stop="openDetail(row)">详情</el-button>
                <el-button link type="primary" @click.stop="openRecharge(row)">储值</el-button>
              </template>
            </el-table-column>
            <template #empty><div class="empty"><strong>还没有会员</strong>小程序与公众号授权后自动建档（UnionID 去重）</div></template>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="标签" name="tag">
        <div class="card">
          <div class="toolbar">
            <el-input v-model="tagName" placeholder="标签名称，如 高意向 / 已出馆" style="width: 240px" />
            <el-button type="primary" @click="addTag">新增标签</el-button>
            <span class="spacer" />
            <span class="muted">标签用于分群触达，触达频次限制每人每天 1 次</span>
          </div>
          <div class="tag-cloud">
            <span v-for="t in tags" :key="t.id" class="tag-chip">
              {{ t.name }}
              <em>{{ memberCountOfTag(t.id) }}</em>
            </span>
            <span v-if="!tags.length" class="muted">暂无标签</span>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="优惠券" name="coupon">
        <div class="card flush">
          <div class="toolbar filter-bar">
            <span class="spacer" />
            <el-button size="small" type="primary" @click="openCoupon()">新增券模板</el-button>
          </div>
          <el-table v-loading="loading" :data="coupons" size="small" stripe>
            <el-table-column prop="name" label="券名称" min-width="160" />
            <el-table-column prop="couponType" label="类型" width="110" />
            <el-table-column label="面额" width="100" align="right">
              <template #default="{ row }"><span class="money">¥{{ money(row.amount) }}</span></template>
            </el-table-column>
            <el-table-column label="门槛" width="110" align="right">
              <template #default="{ row }">满 ¥{{ money(row.threshold) }}</template>
            </el-table-column>
            <el-table-column label="已发/总量" width="120" align="center">
              <template #default="{ row }">{{ row.issuedCount || 0 }} / {{ row.totalCount || '不限' }}</template>
            </el-table-column>
            <el-table-column prop="startTime" label="生效" min-width="150" />
            <el-table-column prop="endTime" label="失效" min-width="150" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }"><span class="badge" :class="row.status === '进行中' ? 'ok' : 'muted'">{{ row.status }}</span></template>
            </el-table-column>
            <template #empty><div class="empty"><strong>暂无券模板</strong></div></template>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="营销活动" name="activity">
        <div class="card flush">
          <div class="toolbar filter-bar">
            <span class="muted">活动状态按开始/结束时间自动推进，无需手工改状态</span>
            <span class="spacer" />
            <el-button size="small" type="primary" @click="openActivity()">新增活动</el-button>
          </div>
          <el-table v-loading="loading" :data="activities" size="small" stripe>
            <el-table-column prop="name" label="活动名称" min-width="170" />
            <el-table-column prop="activityType" label="类型" width="110" />
            <el-table-column prop="startTime" label="开始" min-width="150" />
            <el-table-column prop="endTime" label="结束" min-width="150" />
            <el-table-column prop="joinCount" label="参与人数" width="100" align="right" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }"><span class="badge" :class="row.status === '进行中' ? 'ok' : row.status === '已结束' ? 'muted' : 'warn'">{{ row.status }}</span></template>
            </el-table-column>
            <template #empty><div class="empty"><strong>暂无活动</strong>支持拼团与限时活动</div></template>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="转介绍" name="referral">
        <div class="card flush">
          <el-table v-loading="loading" :data="referrals" size="small" stripe>
            <el-table-column label="推荐人" min-width="140">
              <template #default="{ row }">{{ memberName(row.referrerMember) }}</template>
            </el-table-column>
            <el-table-column label="被推荐线索" min-width="140">
              <template #default="{ row }">{{ row.referredName || `线索 #${row.referredLead}` }}</template>
            </el-table-column>
            <el-table-column prop="rewardPoints" label="奖励积分" width="110" align="right" />
            <el-table-column label="奖励金额" width="120" align="right">
              <template #default="{ row }"><span class="money">¥{{ money(row.rewardAmount) }}</span></template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="110">
              <template #default="{ row }"><span class="badge" :class="row.status === '已结算' ? 'ok' : 'warn'">{{ row.status }}</span></template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.status !== '已结算'" link type="primary" @click="settle(row)">结算奖励</el-button>
                <span v-else class="muted">已结算</span>
              </template>
            </el-table-column>
            <template #empty><div class="empty"><strong>暂无转介绍记录</strong>小程序会员中心分享后自动建立关系</div></template>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 会员详情 -->
    <el-drawer v-model="detailDrawer" :title="current.nickname || '会员详情'" size="640px">
      <div v-if="current.id" class="detail">
        <div class="profile">
          <span class="avatar lg">{{ (current.nickname || '客').slice(0, 1) }}</span>
          <div>
            <strong>{{ current.nickname || '未命名' }}</strong>
            <p class="muted mono">{{ maskPhone(current.phone) }} · {{ current.source || '未知来源' }}</p>
            <p class="muted mono">UnionID {{ current.unionId || '未绑定' }}</p>
          </div>
          <div class="profile-stats">
            <div><span>积分</span><b>{{ current.points || 0 }}</b></div>
            <div><span>储值</span><b>¥{{ money(current.balance) }}</b></div>
          </div>
        </div>

        <div class="mini-actions">
          <el-input-number v-model="pointDelta" :min="-10000" :max="10000" size="small" controls-position="right" style="width: 140px" />
          <el-input v-model="pointReason" size="small" placeholder="调整原因" style="width: 180px" />
          <el-button size="small" type="primary" @click="changePoints">调整积分</el-button>
          <span class="spacer" />
          <el-select v-model="tagToBind" size="small" placeholder="选择标签" style="width: 140px">
            <el-option v-for="t in tags" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
          <el-button size="small" @click="bindTag">打标签</el-button>
        </div>

        <p class="section-label">积分流水</p>
        <ul class="log-list">
          <li v-for="log in points" :key="log.id">
            <span class="mono">{{ log.createdTime }}</span>
            <b :class="log.changeValue > 0 ? 'plus' : 'minus'">{{ log.changeValue > 0 ? '+' : '' }}{{ log.changeValue }}</b>
            <span class="muted">{{ log.reason }}</span>
          </li>
          <li v-if="!points.length" class="muted">暂无积分变动</li>
        </ul>

        <p class="section-label">持券</p>
        <div class="coupon-list">
          <div v-for="c in memberCoupons" :key="c.id" class="coupon-card">
            <strong>¥{{ money(c.amount) }}</strong>
            <span>{{ c.couponName || c.name }}</span>
            <em :class="c.status === '未使用' ? 'ok' : 'muted'">{{ c.status }}</em>
          </div>
          <p v-if="!memberCoupons.length" class="muted">暂无优惠券</p>
        </div>

        <p class="section-label">储值记录</p>
        <ul class="log-list">
          <li v-for="r in recharges" :key="r.id">
            <span class="mono">{{ r.createdTime }}</span>
            <b class="plus">¥{{ money(r.amount) }}</b>
            <span class="muted">赠送 ¥{{ money(r.giftAmount) }} · {{ r.status }}</span>
          </li>
          <li v-if="!recharges.length" class="muted">暂无储值记录</li>
        </ul>
      </div>
    </el-drawer>

    <!-- 新增会员 -->
    <el-dialog v-model="memberDialog" title="新增会员" width="520px">
      <el-form :model="memberForm" label-position="top" class="two-col">
        <el-form-item label="昵称"><el-input v-model="memberForm.nickname" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="memberForm.phone" /></el-form-item>
        <el-form-item label="UnionID"><el-input v-model="memberForm.unionId" placeholder="微信授权后自动带出" /></el-form-item>
        <el-form-item label="来源">
          <el-select v-model="memberForm.source" style="width: 100%">
            <el-option v-for="s in ['小程序', '公众号', '官网', '门店', '转介绍']" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="memberDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveMember">保存</el-button>
      </template>
    </el-dialog>

    <!-- 储值 -->
    <el-dialog v-model="rechargeDialog" title="会员储值" width="480px">
      <el-alert type="info" :closable="false" show-icon title="储值规则已公示：单笔满 5000 元赠送 5%，赠送金额不可提现" style="margin-bottom: 14px" />
      <el-form :model="rechargeForm" label-position="top">
        <el-form-item label="储值金额"><el-input-number v-model="rechargeForm.amount" :min="1" :step="500" controls-position="right" style="width: 100%" /></el-form-item>
        <el-form-item label="赠送金额（留空按规则计算）"><el-input-number v-model="rechargeForm.giftAmount" :min="0" :step="100" controls-position="right" style="width: 100%" /></el-form-item>
        <el-form-item label="已向会员公示规则"><el-switch v-model="rechargeForm.ruleShown" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rechargeDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitRecharge">确认储值</el-button>
      </template>
    </el-dialog>

    <!-- 券模板 -->
    <el-dialog v-model="couponDialog" title="新增优惠券模板" width="520px">
      <el-form :model="couponForm" label-position="top" class="two-col">
        <el-form-item label="券名称" required><el-input v-model="couponForm.name" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="couponForm.couponType" style="width: 100%">
            <el-option label="满减券" value="满减券" />
            <el-option label="折扣券" value="折扣券" />
            <el-option label="现金券" value="现金券" />
          </el-select>
        </el-form-item>
        <el-form-item label="面额"><el-input-number v-model="couponForm.amount" :min="0" :step="50" controls-position="right" style="width: 100%" /></el-form-item>
        <el-form-item label="使用门槛"><el-input-number v-model="couponForm.threshold" :min="0" :step="100" controls-position="right" style="width: 100%" /></el-form-item>
        <el-form-item label="发放总量"><el-input-number v-model="couponForm.totalCount" :min="0" controls-position="right" style="width: 100%" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="couponForm.status" style="width: 100%">
            <el-option label="进行中" value="进行中" />
            <el-option label="已停用" value="已停用" />
          </el-select>
        </el-form-item>
        <el-form-item label="生效时间"><el-date-picker v-model="couponForm.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" /></el-form-item>
        <el-form-item label="失效时间"><el-date-picker v-model="couponForm.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="couponDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveCoupon">保存</el-button>
      </template>
    </el-dialog>

    <!-- 活动 -->
    <el-dialog v-model="activityDialog" title="新增营销活动" width="520px">
      <el-form :model="activityForm" label-position="top" class="two-col">
        <el-form-item label="活动名称" required><el-input v-model="activityForm.name" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="activityForm.activityType" style="width: 100%">
            <el-option label="拼团" value="拼团" />
            <el-option label="限时优惠" value="限时优惠" />
            <el-option label="转介绍奖励" value="转介绍奖励" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始时间"><el-date-picker v-model="activityForm.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" /></el-form-item>
        <el-form-item label="结束时间"><el-date-picker v-model="activityForm.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" /></el-form-item>
        <el-form-item label="规则 JSON" class="full">
          <el-input v-model="activityForm.ruleJson" type="textarea" :rows="3" placeholder='{"minPersons":2,"discount":0.9}' />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="activityDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveActivity">保存</el-button>
      </template>
    </el-dialog>

    <!-- 分群触达 -->
    <el-dialog v-model="touchDialog" title="分群触达" width="480px">
      <el-form :model="touchForm" label-position="top">
        <el-form-item label="目标分群">
          <el-select v-model="touchForm.tagId" clearable placeholder="全部会员" style="width: 100%">
            <el-option v-for="t in tags" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="消息内容" required>
          <el-input v-model="touchForm.content" type="textarea" :rows="4" placeholder="支持小程序订阅消息与站内信" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="touchDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitTouch">发送</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import Icon from '../../components/Icon.vue'
import { tenantHttp } from '../../api/client'

const tab = ref('member')
const loading = ref(false)
const saving = ref(false)
const members = ref<any[]>([])
const tags = ref<any[]>([])
const coupons = ref<any[]>([])
const activities = ref<any[]>([])
const referrals = ref<any[]>([])
const keyword = ref('')
const tagFilter = ref<number>()

const stats = computed(() => {
  const points = members.value.reduce((sum, m) => sum + Number(m.points || 0), 0)
  const balance = members.value.reduce((sum, m) => sum + Number(m.balance || 0), 0)
  return [
    { label: '会员总数', value: members.value.length, hint: 'UnionID 去重' },
    { label: '积分总量', value: points, hint: '当前累计' },
    { label: '储值余额', value: `¥${money(balance)}`, hint: '待消费' },
    { label: '待结算推荐', value: referrals.value.filter((r) => r.status !== '已结算').length, hint: '转介绍奖励' }
  ]
})

function money(v: any) {
  if (v === null || v === undefined || v === '') return '0.00'
  return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function maskPhone(phone: string) {
  if (!phone) return '未留手机号'
  return phone.length >= 7 ? `${phone.slice(0, 3)}****${phone.slice(-4)}` : phone
}

function memberName(id: number) {
  const m = members.value.find((x) => x.id === id)
  return m ? m.nickname || maskPhone(m.phone) : `会员 #${id}`
}

function memberCountOfTag(tagId: number) {
  return members.value.filter((m) => (m.tagIds || []).includes(tagId)).length
}

async function loadMembers() {
  loading.value = true
  try {
    const res: any = await tenantHttp.get('/api/tenant/members', {
      params: { keyword: keyword.value || undefined, tagId: tagFilter.value }
    })
    members.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function loadTags() {
  const res: any = await tenantHttp.get('/api/tenant/members/tags')
  tags.value = res.data || []
}

async function loadCoupons() {
  const res: any = await tenantHttp.get('/api/tenant/members/coupons')
  coupons.value = res.data || []
}

async function loadActivities() {
  const res: any = await tenantHttp.get('/api/tenant/members/activities')
  activities.value = res.data || []
}

async function loadReferrals() {
  const res: any = await tenantHttp.get('/api/tenant/members/referrals')
  referrals.value = res.data || []
}

function loadAll() {
  loadMembers()
  loadTags()
  loadCoupons()
  loadActivities()
  loadReferrals()
}

function onTab(name: string) {
  if (name === 'member') loadMembers()
  if (name === 'tag') loadTags()
  if (name === 'coupon') loadCoupons()
  if (name === 'activity') loadActivities()
  if (name === 'referral') loadReferrals()
}

/* 详情 */
const detailDrawer = ref(false)
const current = ref<any>({})
const points = ref<any[]>([])
const memberCoupons = ref<any[]>([])
const recharges = ref<any[]>([])
const pointDelta = ref(0)
const pointReason = ref('人工调整')
const tagToBind = ref<number>()

async function openDetail(row: any) {
  current.value = { ...row }
  detailDrawer.value = true
  const [p, c, r] = await Promise.all([
    tenantHttp.get(`/api/tenant/members/${row.id}/points`),
    tenantHttp.get(`/api/tenant/members/${row.id}/coupons`),
    tenantHttp.get(`/api/tenant/members/${row.id}/recharges`)
  ])
  points.value = (p as any).data || []
  memberCoupons.value = (c as any).data || []
  recharges.value = (r as any).data || []
}

async function changePoints() {
  if (!pointDelta.value) {
    ElMessage.warning('请输入积分变动值')
    return
  }
  await tenantHttp.post(`/api/tenant/members/${current.value.id}/points`, {
    delta: pointDelta.value,
    reason: pointReason.value
  })
  ElMessage.success('积分已调整')
  pointDelta.value = 0
  await openDetail(current.value)
  loadMembers()
}

async function bindTag() {
  if (!tagToBind.value) {
    ElMessage.warning('请选择标签')
    return
  }
  await tenantHttp.post(`/api/tenant/members/${current.value.id}/tags/${tagToBind.value}`)
  ElMessage.success('已打标签')
}

/* 会员 */
const memberDialog = ref(false)
const memberForm = reactive<any>({})

function openMember() {
  Object.keys(memberForm).forEach((k) => delete memberForm[k])
  memberForm.source = '门店'
  memberDialog.value = true
}

async function saveMember() {
  saving.value = true
  try {
    await tenantHttp.post('/api/tenant/members', { ...memberForm })
    ElMessage.success('会员已保存')
    memberDialog.value = false
    loadMembers()
  } finally {
    saving.value = false
  }
}

/* 储值 */
const rechargeDialog = ref(false)
const rechargeForm = reactive<any>({})

function openRecharge(row: any) {
  current.value = { ...row }
  Object.keys(rechargeForm).forEach((k) => delete rechargeForm[k])
  Object.assign(rechargeForm, { amount: 5000, giftAmount: null, ruleShown: true })
  rechargeDialog.value = true
}

async function submitRecharge() {
  saving.value = true
  try {
    const res: any = await tenantHttp.post(`/api/tenant/members/${current.value.id}/recharge`, {
      amount: rechargeForm.amount,
      giftAmount: rechargeForm.giftAmount || null,
      ruleShown: rechargeForm.ruleShown
    })
    ElMessage.success(`储值成功，赠送 ¥${money(res.data?.giftAmount || 0)}`)
    rechargeDialog.value = false
    loadMembers()
  } finally {
    saving.value = false
  }
}

/* 标签 */
const tagName = ref('')

async function addTag() {
  if (!tagName.value) {
    ElMessage.warning('请输入标签名称')
    return
  }
  await tenantHttp.post('/api/tenant/members/tags', { name: tagName.value })
  ElMessage.success('标签已新增')
  tagName.value = ''
  loadTags()
}

/* 券 */
const couponDialog = ref(false)
const couponForm = reactive<any>({})

function openCoupon() {
  Object.keys(couponForm).forEach((k) => delete couponForm[k])
  Object.assign(couponForm, { couponType: '满减券', amount: 1000, threshold: 10000, status: '进行中' })
  couponDialog.value = true
}

async function saveCoupon() {
  saving.value = true
  try {
    await tenantHttp.post('/api/tenant/members/coupons', { ...couponForm })
    ElMessage.success('券模板已保存')
    couponDialog.value = false
    loadCoupons()
  } finally {
    saving.value = false
  }
}

/* 活动 */
const activityDialog = ref(false)
const activityForm = reactive<any>({})

function openActivity() {
  Object.keys(activityForm).forEach((k) => delete activityForm[k])
  Object.assign(activityForm, { activityType: '拼团' })
  activityDialog.value = true
}

async function saveActivity() {
  saving.value = true
  try {
    await tenantHttp.post('/api/tenant/members/activities', { ...activityForm })
    ElMessage.success('活动已创建')
    activityDialog.value = false
    loadActivities()
  } finally {
    saving.value = false
  }
}

/* 转介绍 */
async function settle(row: any) {
  await ElMessageBox.confirm('结算后将发放积分与奖励金额，确认？', '结算转介绍奖励', { type: 'info' })
  await tenantHttp.post(`/api/tenant/members/referrals/${row.id}/settle`)
  ElMessage.success('已结算')
  loadReferrals()
}

/* 触达 */
const touchDialog = ref(false)
const touchForm = reactive<any>({ content: '' })

function openTouch() {
  touchForm.content = ''
  touchForm.tagId = undefined
  touchDialog.value = true
}

async function submitTouch() {
  if (!touchForm.content) {
    ElMessage.warning('请填写消息内容')
    return
  }
  saving.value = true
  try {
    const res: any = await tenantHttp.post('/api/tenant/members/touch', { ...touchForm })
    ElMessage.success(`触达完成，成功 ${res.data?.sent || 0} 人，因频次限制跳过 ${res.data?.skipped || 0} 人`)
    touchDialog.value = false
  } finally {
    saving.value = false
  }
}

onMounted(loadAll)
</script>

<style scoped>
.stat-row { display: grid; grid-template-columns: repeat(auto-fit, minmax(170px, 1fr)); gap: var(--space-3); }
.stat { background: var(--surface); border: 1px solid var(--line); border-radius: var(--radius-lg); padding: 14px 16px; display: flex; flex-direction: column; gap: 2px; box-shadow: var(--shadow-xs); }
.stat-label { font-size: 11.5px; color: var(--ink-400); }
.stat-value { font-size: 22px; font-weight: 650; font-variant-numeric: tabular-nums; }
.stat-hint { font-style: normal; font-size: 11.5px; color: var(--ink-400); }
.biz-tabs :deep(.el-tabs__header) { margin-bottom: 0; padding: 0 4px; border-bottom: 1px solid var(--line); }
.biz-tabs :deep(.el-tabs__nav-wrap::after) { display: none; }
.biz-tabs :deep(.el-tabs__item) { height: 42px; font-size: 13.5px; color: var(--ink-500); }
.biz-tabs :deep(.el-tabs__item.is-active) { color: var(--brand-700); font-weight: 600; }
.biz-tabs :deep(.el-tabs__active-bar) { background: var(--brand-600); }
.filter-bar { padding: 14px var(--space-5); border-bottom: 1px solid var(--line); background: var(--surface-sunken); }

.member-cell { display: flex; align-items: center; gap: 9px; }
.member-cell .avatar { width: 30px; height: 30px; border-radius: 50%; background: var(--brand-50); color: var(--brand-700); display: grid; place-items: center; font-size: 12px; font-weight: 600; }
.member-cell div { display: flex; flex-direction: column; line-height: 1.3; }
.member-cell strong { font-size: 13px; }
.member-cell em { font-style: normal; font-size: 11px; color: var(--ink-400); }

.tag-cloud { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 14px; }
.tag-chip { display: inline-flex; align-items: center; gap: 7px; padding: 5px 11px; border-radius: 999px; background: var(--brand-50); color: var(--brand-700); font-size: 12.5px; border: 1px solid var(--brand-100); }
.tag-chip em { font-style: normal; font-size: 11px; color: var(--ink-400); }

.badge { display: inline-flex; align-items: center; height: 22px; padding: 0 8px; border-radius: 999px; font-size: 12px; background: #eef2f7; color: #4a5f7a; }
.badge.ok { background: #eef7f1; color: #2f7d55; }
.badge.warn { background: #fdf5e8; color: #9a6a18; }
.badge.muted { background: #f4f3ef; color: var(--ink-500); }

.detail { display: flex; flex-direction: column; gap: var(--space-4); }
.profile { display: flex; align-items: center; gap: 14px; background: var(--surface-sunken); border-radius: var(--radius-lg); padding: 14px; }
.avatar.lg { width: 46px; height: 46px; border-radius: 50%; background: var(--brand-50); color: var(--brand-700); display: grid; place-items: center; font-size: 17px; font-weight: 600; }
.profile p { margin: 2px 0 0; font-size: 11.5px; }
.profile-stats { margin-left: auto; display: flex; gap: 18px; }
.profile-stats div { display: flex; flex-direction: column; align-items: flex-end; }
.profile-stats span { font-size: 11px; color: var(--ink-400); }
.profile-stats b { font-size: 16px; font-variant-numeric: tabular-nums; }
.mini-actions { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.mini-actions .spacer { flex: 1; }
.section-label { margin: 4px 0 0; font-size: 11.5px; letter-spacing: 0.08em; color: var(--ink-400); font-weight: 600; }
.log-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 7px; }
.log-list li { display: flex; align-items: center; gap: 12px; font-size: 12.5px; }
.log-list span.mono { flex: 0 0 145px; color: var(--ink-400); font-size: 11.5px; }
.log-list b { min-width: 60px; font-variant-numeric: tabular-nums; }
.log-list b.plus { color: #2f7d55; }
.log-list b.minus { color: #a83c37; }
.coupon-list { display: flex; flex-wrap: wrap; gap: 8px; }
.coupon-card { border: 1px dashed var(--line-strong); border-radius: var(--radius-md); padding: 8px 12px; display: flex; flex-direction: column; gap: 1px; min-width: 120px; }
.coupon-card strong { font-size: 15px; color: var(--brand-700); }
.coupon-card span { font-size: 11.5px; color: var(--ink-600); }
.coupon-card em { font-style: normal; font-size: 11px; color: var(--ink-400); }
.coupon-card em.ok { color: #2f7d55; }
.two-col { display: grid; grid-template-columns: 1fr 1fr; gap: 0 16px; }
.two-col :deep(.full) { grid-column: 1 / -1; }
.empty { padding: 30px 0; text-align: center; color: var(--ink-400); font-size: 12px; }
.empty strong { display: block; color: var(--ink-700); font-size: 13px; margin-bottom: 4px; }
</style>
