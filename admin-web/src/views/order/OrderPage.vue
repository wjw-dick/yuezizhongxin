<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>订单与收款</h1>
        <p class="sub">定金锁档 → 入住 → 尾款与押金结算；退订走「申请 → 审核 → 退款」两段流程，全部留痕</p>
      </div>
      <div class="actions">
        <el-button @click="openCreate()">
          <Icon name="plus" :size="15" /><span style="margin-left: 6px">代客下单</span>
        </el-button>
        <el-button @click="loadAll">
          <Icon name="refresh" :size="15" /><span style="margin-left: 6px">刷新</span>
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
      <el-tab-pane label="订单列表" name="order">
        <div class="card flush">
          <div class="toolbar filter-bar">
            <el-input v-model="query.keyword" placeholder="订单号 / 客户 / 手机号" clearable size="small" style="width: 210px" @keyup.enter="reload(1)" @clear="reload(1)" />
            <el-select v-model="query.status" placeholder="全部状态" clearable size="small" style="width: 140px" @change="reload(1)">
              <el-option v-for="s in statuses" :key="s" :label="s" :value="s" />
            </el-select>
            <el-date-picker v-model="range" type="daterange" size="small" value-format="YYYY-MM-DD" start-placeholder="下单起" end-placeholder="下单止" style="width: 240px" @change="reload(1)" />
            <el-button size="small" @click="reload(1)">查询</el-button>
            <el-button size="small" text @click="reset">重置</el-button>
            <span class="spacer" />
            <span class="muted">共 {{ total }} 笔</span>
          </div>

          <el-table v-loading="loading" :data="rows" size="small" stripe @row-click="openDetail">
            <el-table-column prop="orderNo" label="订单号" width="180">
              <template #default="{ row }"><span class="mono">{{ row.orderNo }}</span></template>
            </el-table-column>
            <el-table-column prop="customerName" label="客户" width="100" />
            <el-table-column label="套餐" min-width="150">
              <template #default="{ row }">{{ pkgName(row.packageId) }}</template>
            </el-table-column>
            <el-table-column label="房间" width="110">
              <template #default="{ row }">{{ roomName(row.roomId) }}</template>
            </el-table-column>
            <el-table-column label="入住区间" min-width="190">
              <template #default="{ row }">
                <span class="mono">{{ row.checkinDate }} → {{ row.checkoutDate }}</span>
              </template>
            </el-table-column>
            <el-table-column label="总额" width="120" align="right">
              <template #default="{ row }"><span class="money">¥{{ money(row.totalAmount) }}</span></template>
            </el-table-column>
            <el-table-column label="已收 / 待收" width="150" align="right">
              <template #default="{ row }">
                <span class="money">¥{{ money(row.paidAmount) }}</span>
                <span class="muted"> / ¥{{ money(Number(row.totalAmount || 0) - Number(row.paidAmount || 0)) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }"><span class="badge" :class="statusClass(row.status)">{{ row.status }}</span></template>
            </el-table-column>
            <el-table-column prop="source" label="来源" width="90" />
            <el-table-column label="操作" width="170" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click.stop="openDetail(row)">详情</el-button>
                <el-button
                  v-if="!['已取消', '已退订', '已完成'].includes(row.status)"
                  link
                  type="primary"
                  @click.stop="openPay(row)"
                >收款</el-button>
                <el-button
                  v-if="!['已取消', '已退订', '已完成'].includes(row.status)"
                  link
                  type="danger"
                  @click.stop="openRefund(row)"
                >退订</el-button>
              </template>
            </el-table-column>
            <template #empty>
              <div class="empty"><strong>暂无订单</strong>可从线索「转订单」或右上角代客下单</div>
            </template>
          </el-table>

          <div class="pager">
            <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="prev, pager, next, total" small background @current-change="load" />
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane name="refund">
        <template #label>
          退款审核
          <span v-if="pendingRefundCount" class="tab-badge">{{ pendingRefundCount }}</span>
        </template>
        <div class="card flush">
          <el-table v-loading="loading" :data="refunds" size="small" stripe>
            <el-table-column prop="refundNo" label="退款单号" width="180">
              <template #default="{ row }"><span class="mono">{{ row.refundNo }}</span></template>
            </el-table-column>
            <el-table-column label="订单" width="180">
              <template #default="{ row }"><span class="mono">{{ orderNo(row.orderId) }}</span></template>
            </el-table-column>
            <el-table-column label="退款金额" width="120" align="right">
              <template #default="{ row }"><span class="money">¥{{ money(row.amount) }}</span></template>
            </el-table-column>
            <el-table-column label="违约金" width="120" align="right">
              <template #default="{ row }"><span class="money">¥{{ money(row.penaltyAmount) }}</span></template>
            </el-table-column>
            <el-table-column prop="reasonCode" label="原因分类" width="120" />
            <el-table-column prop="reason" label="说明" min-width="180" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }"><span class="badge" :class="row.status === '已退款' ? 'ok' : row.status === '已驳回' ? 'danger' : 'warn'">{{ row.status }}</span></template>
            </el-table-column>
            <el-table-column prop="createdTime" label="申请时间" min-width="150" />
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <template v-if="row.status === '待审核'">
                  <el-button link type="primary" @click="audit(row, true)">通过</el-button>
                  <el-button link type="danger" @click="audit(row, false)">驳回</el-button>
                </template>
                <span v-else class="muted">已处理</span>
              </template>
            </el-table-column>
            <template #empty>
              <div class="empty"><strong>暂无退款单</strong>在订单详情或列表点「退订」发起申请</div>
            </template>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane name="pending">
        <div class="card flush">
          <div class="toolbar filter-bar">
            <span class="muted">支付异常与待支付收款单，可主动查单或模拟支付成功（仅未配置商户参数时可用）</span>
            <span class="spacer" />
            <el-button size="small" @click="loadPending">刷新</el-button>
          </div>
          <el-table v-loading="loading" :data="pending" size="small" stripe>
            <el-table-column prop="paymentNo" label="收款单号" width="190">
              <template #default="{ row }"><span class="mono">{{ row.paymentNo }}</span></template>
            </el-table-column>
            <el-table-column label="订单" width="180">
              <template #default="{ row }"><span class="mono">{{ orderNo(row.orderId) }}</span></template>
            </el-table-column>
            <el-table-column prop="payType" label="款项" width="90" />
            <el-table-column prop="method" label="方式" width="110" />
            <el-table-column label="金额" width="120" align="right">
              <template #default="{ row }"><span class="money">¥{{ money(row.amount) }}</span></template>
            </el-table-column>
            <el-table-column label="状态" width="110">
              <template #default="{ row }"><span class="badge warn">{{ row.status }}</span></template>
            </el-table-column>
            <el-table-column prop="createdTime" label="创建时间" min-width="150" />
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="queryPay(row)">查单</el-button>
                <el-button link type="primary" @click="mockPay(row)">模拟成功</el-button>
              </template>
            </el-table-column>
            <template #empty>
              <div class="empty"><strong>没有待对账收款单</strong>所有微信支付收款均已确认</div>
            </template>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 订单详情 -->
    <el-drawer v-model="detailDrawer" :title="detail.order?.orderNo || '订单详情'" size="720px">
      <div v-if="detail.order" class="detail">
        <div class="amount-grid">
          <div class="amount-cell">
            <span>订单总额</span>
            <strong>¥{{ money(detail.order.totalAmount) }}</strong>
          </div>
          <div class="amount-cell">
            <span>已收金额</span>
            <strong class="ok">¥{{ money(detail.order.paidAmount) }}</strong>
          </div>
          <div class="amount-cell">
            <span>待收金额</span>
            <strong class="warn">¥{{ money(remain) }}</strong>
          </div>
          <div class="amount-cell">
            <span>状态</span>
            <strong><span class="badge" :class="statusClass(detail.order.status)">{{ detail.order.status }}</span></strong>
          </div>
        </div>

        <div class="breakdown">
          <div><span>套餐金额</span><b>¥{{ money(detail.order.packageAmount) }}</b></div>
          <div><span>增值服务</span><b>¥{{ money(detail.order.extraAmount) }}</b></div>
          <div><span>商品</span><b>¥{{ money(detail.order.productAmount) }}</b></div>
          <div><span>优惠</span><b>-¥{{ money(detail.order.discountAmount) }}</b></div>
          <div><span>定金</span><b>¥{{ money(detail.order.depositAmount) }}</b></div>
          <div><span>尾款</span><b>¥{{ money(detail.order.tailAmount) }}</b></div>
          <div><span>押金</span><b>¥{{ money(detail.order.bondAmount) }}</b></div>
        </div>

        <div class="detail-grid">
          <div class="detail-item"><span>客户</span><strong>{{ detail.order.customerName }}</strong></div>
          <div class="detail-item"><span>手机号</span><strong class="mono">{{ maskPhone(detail.order.phone) }}</strong></div>
          <div class="detail-item"><span>套餐</span><strong>{{ pkgName(detail.order.packageId) }}</strong></div>
          <div class="detail-item"><span>房间</span><strong>{{ roomName(detail.order.roomId) }}</strong></div>
          <div class="detail-item"><span>入住</span><strong>{{ detail.order.checkinDate }}</strong></div>
          <div class="detail-item"><span>离馆</span><strong>{{ detail.order.checkoutDate }}</strong></div>
          <div class="detail-item"><span>护理模式</span><strong>{{ detail.order.careMode || '-' }}</strong></div>
          <div class="detail-item"><span>来源</span><strong>{{ detail.order.source || '-' }}</strong></div>
        </div>

        <div class="drawer-actions">
          <el-button size="small" @click="openPay(detail.order)">发起收款</el-button>
          <el-button size="small" @click="openChange(detail.order)">订单变更</el-button>
          <el-button size="small" @click="openRefund(detail.order)">申请退订</el-button>
          <el-button v-if="detail.order.status === '待支付'" size="small" type="danger" plain @click="cancelOrder(detail.order)">取消订单</el-button>
          <span class="spacer" />
          <el-button size="small" @click="pickContract">上传合同</el-button>
        </div>

        <p class="section-label">收款单</p>
        <el-table :data="detail.payments || []" size="small" stripe>
          <el-table-column prop="paymentNo" label="单号" width="170">
            <template #default="{ row }"><span class="mono">{{ row.paymentNo }}</span></template>
          </el-table-column>
          <el-table-column prop="payType" label="款项" width="80" />
          <el-table-column prop="method" label="方式" width="100" />
          <el-table-column label="金额" width="110" align="right">
            <template #default="{ row }"><span class="money">¥{{ money(row.amount) }}</span></template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }"><span class="badge" :class="row.status === '已支付' ? 'ok' : 'warn'">{{ row.status }}</span></template>
          </el-table-column>
          <el-table-column prop="paidTime" label="支付时间" min-width="150" />
          <template #empty><div class="empty">还没有收款记录</div></template>
        </el-table>

        <p class="section-label">退款单</p>
        <el-table :data="detail.refunds || []" size="small" stripe>
          <el-table-column prop="refundNo" label="单号" width="170">
            <template #default="{ row }"><span class="mono">{{ row.refundNo }}</span></template>
          </el-table-column>
          <el-table-column label="金额" width="110" align="right">
            <template #default="{ row }"><span class="money">¥{{ money(row.amount) }}</span></template>
          </el-table-column>
          <el-table-column prop="penaltyAmount" label="违约金" width="100" align="right" />
          <el-table-column prop="reason" label="原因" min-width="180" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }"><span class="badge" :class="row.status === '已退款' ? 'ok' : 'warn'">{{ row.status }}</span></template>
          </el-table-column>
          <template #empty><div class="empty">没有退款记录</div></template>
        </el-table>

        <p class="section-label">变更留痕</p>
        <ul class="change-list">
          <li v-for="c in detail.changes || []" :key="c.id">
            <span class="badge info">{{ c.changeType }}</span>
            <span class="change-diff">差价 ¥{{ money(c.amountDiff) }}</span>
            <span class="muted">{{ c.reason || '无说明' }}</span>
            <em class="mono">{{ c.createdTime }}</em>
          </li>
          <li v-if="!(detail.changes || []).length" class="muted">没有变更记录</li>
        </ul>

        <p class="section-label">合同附件</p>
        <ul class="contract-list">
          <li v-for="c in detail.contracts || []" :key="c.id">
            <Icon name="book" :size="14" />
            <a :href="`/api/files/${c.fileKey}`" target="_blank" rel="noreferrer">{{ c.fileName || c.fileKey }}</a>
            <em class="muted">{{ c.createdTime }}</em>
          </li>
          <li v-if="!(detail.contracts || []).length" class="muted">未上传合同</li>
        </ul>
        <input ref="contractInput" type="file" hidden @change="uploadContract" />
      </div>
    </el-drawer>

    <!-- 收款 -->
    <el-dialog v-model="payDialog" title="发起收款" width="480px">
      <el-form :model="payForm" label-position="top">
        <el-form-item label="款项类型">
          <el-radio-group v-model="payForm.payType">
            <el-radio-button label="定金" />
            <el-radio-button label="尾款" />
            <el-radio-button label="押金" />
            <el-radio-button label="增值消费" />
          </el-radio-group>
        </el-form-item>
        <el-form-item label="支付方式">
          <el-select v-model="payForm.method" style="width: 100%">
            <el-option label="微信支付（生成预支付单）" value="微信支付" />
            <el-option label="现金" value="现金" />
            <el-option label="银行转账" value="银行转账" />
            <el-option label="对公转账" value="对公转账" />
          </el-select>
        </el-form-item>
        <el-form-item label="收款金额">
          <el-input-number v-model="payForm.amount" :min="0.01" :precision="2" :step="100" controls-position="right" style="width: 100%" />
          <p class="hint">留空按系统规则自动取定金或尾款金额</p>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="payForm.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="payDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitPay">确认收款</el-button>
      </template>
    </el-dialog>

    <!-- 退订 -->
    <el-dialog v-model="refundDialog" title="申请退订" width="520px">
      <el-alert type="warning" :closable="false" show-icon title="退款金额不得超过已收金额扣除违约金后的余额" style="margin-bottom: 14px" />
      <el-form :model="refundForm" label-position="top" class="two-col">
        <el-form-item label="退款金额"><el-input-number v-model="refundForm.amount" :min="0" :precision="2" :step="100" controls-position="right" style="width: 100%" /></el-form-item>
        <el-form-item label="违约金"><el-input-number v-model="refundForm.penaltyAmount" :min="0" :precision="2" :step="100" controls-position="right" style="width: 100%" /></el-form-item>
        <el-form-item label="原因分类">
          <el-select v-model="refundForm.reasonCode" style="width: 100%">
            <el-option v-for="r in reasonCodes" :key="r" :label="r" :value="r" />
          </el-select>
        </el-form-item>
        <el-form-item label="详细说明"><el-input v-model="refundForm.reason" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="refundDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitRefund">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 变更 -->
    <el-dialog v-model="changeDialog" title="订单变更" width="520px">
      <el-form :model="changeForm" label-position="top">
        <el-form-item label="变更类型">
          <el-radio-group v-model="changeForm.changeType">
            <el-radio-button label="换房" />
            <el-radio-button label="延期" />
            <el-radio-button label="改套餐" />
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="changeForm.changeType === '换房'" label="新房间">
          <el-select v-model="changeForm.newRoomId" style="width: 100%">
            <el-option v-for="r in rooms" :key="r.id" :label="`${r.roomNo}（${r.floor || '-'}）`" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="changeForm.changeType === '延期'" label="新离馆日期">
          <el-date-picker v-model="changeForm.newCheckoutDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item v-if="changeForm.changeType === '改套餐'" label="新套餐">
          <el-select v-model="changeForm.newPackageId" style="width: 100%">
            <el-option v-for="p in packages" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="变更原因"><el-input v-model="changeForm.reason" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="changeDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitChange">确认变更</el-button>
      </template>
    </el-dialog>

    <!-- 代客下单 -->
    <el-dialog v-model="createDialog" title="代客下单" width="640px">
      <el-form :model="createForm" label-position="top" class="two-col">
        <el-form-item label="客户姓名" required><el-input v-model="createForm.customerName" /></el-form-item>
        <el-form-item label="手机号" required><el-input v-model="createForm.phone" /></el-form-item>
        <el-form-item label="套餐" required>
          <el-select v-model="createForm.packageId" style="width: 100%" @change="autoCheckout">
            <el-option v-for="p in packages" :key="p.id" :label="`${p.name}（${p.days} 天 · ¥${money(p.price)}）`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="房间" required>
          <el-select v-model="createForm.roomId" filterable style="width: 100%">
            <el-option v-for="r in rooms" :key="r.id" :label="`${r.roomNo}（${r.floor || '-'}）`" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="入住日期" required>
          <el-date-picker v-model="createForm.checkinDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" @change="autoCheckout" />
        </el-form-item>
        <el-form-item label="离馆日期" required>
          <el-date-picker v-model="createForm.checkoutDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="护理模式">
          <el-select v-model="createForm.careMode" style="width: 100%">
            <el-option label="一对一" value="一对一" />
            <el-option label="一对二" value="一对二" />
            <el-option label="集中护理" value="集中护理" />
          </el-select>
        </el-form-item>
        <el-form-item label="来源">
          <el-select v-model="createForm.source" style="width: 100%">
            <el-option v-for="s in ['门店', '官网', '小程序', '公众号', 'H5', '转介绍']" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="优惠金额"><el-input-number v-model="createForm.discountAmount" :min="0" :step="100" controls-position="right" style="width: 100%" /></el-form-item>
        <el-form-item label="定金（留空按套餐比例）"><el-input-number v-model="createForm.bondAmount" :min="0" :step="100" controls-position="right" style="width: 100%" /></el-form-item>
        <el-form-item label="备注" class="full"><el-input v-model="createForm.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitCreate">创建并锁定档期</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import Icon from '../../components/Icon.vue'
import { tenantHttp } from '../../api/client'
import { uploadFile } from '../../api/resource'

const route = useRoute()
const tab = ref('order')
const loading = ref(false)
const saving = ref(false)
const rows = ref<any[]>([])
const refunds = ref<any[]>([])
const pending = ref<any[]>([])
const packages = ref<any[]>([])
const rooms = ref<any[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const range = ref<string[] | null>(null)
const query = reactive<{ keyword: string; status: string }>({ keyword: '', status: '' })

const statuses = ['待支付', '待入住', '在住', '待结算', '已完成', '已取消', '已退订']
const reasonCodes = ['个人原因', '身体原因', '医疗建议', '家庭原因', '服务不满意', '其他']

const pendingRefundCount = computed(() => refunds.value.filter((r) => r.status === '待审核').length)
const remain = computed(() => {
  const o = detail.value.order
  return o ? Number(o.totalAmount || 0) - Number(o.paidAmount || 0) : 0
})

const stats = computed(() => {
  const living = rows.value.filter((r) => r.status === '在住').length
  const unpaid = rows.value.filter((r) => r.status === '待支付').length
  const revenue = rows.value.reduce((sum, r) => sum + Number(r.paidAmount || 0), 0)
  return [
    { label: '本页订单', value: rows.value.length, hint: `累计 ${total.value} 笔` },
    { label: '待支付', value: unpaid, hint: '需催收定金' },
    { label: '在住', value: living, hint: '当前在住订单' },
    { label: '已收合计', value: `¥${money(revenue)}`, hint: '本页口径' }
  ]
})

function money(v: any) {
  if (v === null || v === undefined || v === '') return '0.00'
  return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function maskPhone(phone: string) {
  if (!phone) return '-'
  return phone.length >= 7 ? `${phone.slice(0, 3)}****${phone.slice(-4)}` : phone
}

function pkgName(id: number) {
  return packages.value.find((p) => p.id === id)?.name || (id ? `套餐 #${id}` : '-')
}

function roomName(id: number) {
  const room = rooms.value.find((r) => r.id === id)
  return room ? room.roomNo : id ? `房间 #${id}` : '-'
}

function orderNo(id: number) {
  return rows.value.find((r) => r.id === id)?.orderNo || `#${id}`
}

function statusClass(status: string) {
  return {
    待支付: 'warn',
    待入住: 'info',
    在住: 'ok',
    待结算: 'warn',
    已完成: 'ok',
    已取消: 'muted',
    已退订: 'danger'
  }[status] || 'info'
}

async function load() {
  loading.value = true
  try {
    const res: any = await tenantHttp.get('/api/tenant/orders', {
      params: {
        status: query.status || undefined,
        keyword: query.keyword || undefined,
        from: range.value?.[0],
        to: range.value?.[1],
        pageNum: pageNum.value,
        pageSize: pageSize.value
      }
    })
    rows.value = res.data?.records || []
    total.value = Number(res.data?.total || 0)
  } finally {
    loading.value = false
  }
}

function reload(page?: number) {
  if (page) pageNum.value = page
  load()
}

function reset() {
  query.keyword = ''
  query.status = ''
  range.value = null
  reload(1)
}

async function loadRefunds() {
  const res: any = await tenantHttp.get('/api/tenant/orders/refunds')
  refunds.value = res.data || []
}

async function loadPending() {
  const res: any = await tenantHttp.get('/api/tenant/orders/payments/pending')
  pending.value = res.data || []
}

async function loadRefs() {
  const [pkgs, rms] = await Promise.all([
    tenantHttp.get('/api/tenant/catalog/packages'),
    tenantHttp.get('/api/tenant/rooms')
  ])
  packages.value = (pkgs as any).data?.records || (pkgs as any).data || []
  rooms.value = (rms as any).data?.records || (rms as any).data || []
}

function loadAll() {
  load()
  loadRefunds()
  loadPending()
}

function onTab(name: string) {
  if (name === 'refund') loadRefunds()
  if (name === 'pending') loadPending()
  if (name === 'order') load()
}

/* ---------------- 详情 ---------------- */
const detailDrawer = ref(false)
const detail = ref<any>({})

async function openDetail(row: any) {
  const res: any = await tenantHttp.get(`/api/tenant/orders/${row.id}`)
  detail.value = res.data || {}
  detailDrawer.value = true
}

/* ---------------- 收款 ---------------- */
const payDialog = ref(false)
const payForm = reactive<any>({})
let payOrder: any = null

function openPay(order: any) {
  payOrder = order
  Object.keys(payForm).forEach((k) => delete payForm[k])
  Object.assign(payForm, { payType: '定金', method: '微信支付', amount: undefined, remark: '' })
  payDialog.value = true
}

async function submitPay() {
  saving.value = true
  try {
    const res: any = await tenantHttp.post(`/api/tenant/orders/${payOrder.id}/pay`, {
      payType: payForm.payType,
      method: payForm.method,
      amount: payForm.amount || null,
      remark: payForm.remark
    })
    if (payForm.method === '微信支付') {
      ElMessage.success(`已生成预支付单 ${res.data?.paymentNo || ''}，请客户扫码支付`)
    } else {
      ElMessage.success('收款已登记')
    }
    payDialog.value = false
    detailDrawer.value = false
    loadAll()
  } finally {
    saving.value = false
  }
}

async function queryPay(row: any) {
  const res: any = await tenantHttp.post(`/api/tenant/orders/payments/${row.id}/query`)
  ElMessage.success(`查单结果：${res.data || '未知'}`)
  loadPending()
}

async function mockPay(row: any) {
  await ElMessageBox.confirm('模拟支付成功会真实改写订单与档期状态，仅用于本地联调。确认继续？', '模拟支付', { type: 'warning' })
  await tenantHttp.post(`/api/tenant/orders/payments/${row.id}/mock-success`)
  ElMessage.success('已置为支付成功')
  loadAll()
}

/* ---------------- 退订 ---------------- */
const refundDialog = ref(false)
const refundForm = reactive<any>({})
let refundOrder: any = null

function openRefund(order: any) {
  refundOrder = order
  Object.keys(refundForm).forEach((k) => delete refundForm[k])
  Object.assign(refundForm, {
    amount: Number(order.paidAmount || 0),
    penaltyAmount: 0,
    reasonCode: '个人原因',
    reason: ''
  })
  refundDialog.value = true
}

async function submitRefund() {
  saving.value = true
  try {
    await tenantHttp.post(`/api/tenant/orders/${refundOrder.id}/refunds`, {
      amount: refundForm.amount,
      penaltyAmount: refundForm.penaltyAmount,
      reasonCode: refundForm.reasonCode,
      reason: refundForm.reason
    })
    ElMessage.success('退订申请已提交，等待审核')
    refundDialog.value = false
    detailDrawer.value = false
    loadAll()
  } finally {
    saving.value = false
  }
}

async function audit(row: any, approved: boolean) {
  let remark = ''
  if (!approved) {
    const { value } = await ElMessageBox.prompt('请填写驳回原因', '驳回退订', {
      inputValidator: (v: string) => (v && v.trim() ? true : '驳回原因不能为空')
    })
    remark = value
  } else {
    await ElMessageBox.confirm(`确认通过退款 ¥${money(row.amount)}？通过后将调用微信退款接口。`, '退款审核', { type: 'warning' })
  }
  await tenantHttp.post(`/api/tenant/orders/refunds/${row.id}/audit`, {
    approved,
    auditRemark: remark,
    amount: row.amount,
    penaltyAmount: row.penaltyAmount,
    reasonCode: row.reasonCode,
    reason: row.reason
  })
  ElMessage.success(approved ? '已通过并退款' : '已驳回')
  loadAll()
}

/* ---------------- 变更 ---------------- */
const changeDialog = ref(false)
const changeForm = reactive<any>({})
let changeOrder: any = null

function openChange(order: any) {
  changeOrder = order
  Object.keys(changeForm).forEach((k) => delete changeForm[k])
  Object.assign(changeForm, { changeType: '换房', reason: '' })
  changeDialog.value = true
}

async function submitChange() {
  saving.value = true
  try {
    const res: any = await tenantHttp.post(`/api/tenant/orders/${changeOrder.id}/change`, {
      changeType: changeForm.changeType,
      newRoomId: changeForm.newRoomId || null,
      newPackageId: changeForm.newPackageId || null,
      newCheckoutDate: changeForm.newCheckoutDate || null,
      reason: changeForm.reason
    })
    ElMessage.success(`变更已完成，差价 ¥${money(res.data?.amountDiff || 0)}`)
    changeDialog.value = false
    detailDrawer.value = false
    loadAll()
  } finally {
    saving.value = false
  }
}

async function cancelOrder(order: any) {
  const { value } = await ElMessageBox.prompt('请填写取消原因（会释放档期）', '取消订单', {
    inputValidator: (v: string) => (v && v.trim() ? true : '取消原因不能为空')
  })
  await tenantHttp.post(`/api/tenant/orders/${order.id}/cancel`, { reason: value })
  ElMessage.success('订单已取消，档期已释放')
  detailDrawer.value = false
  loadAll()
}

/* ---------------- 合同 ---------------- */
const contractInput = ref<HTMLInputElement>()

function pickContract() {
  contractInput.value?.click()
}

async function uploadContract(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file || !detail.value.order) return
  const uploaded = await uploadFile(file, 'contract')
  await tenantHttp.post(`/api/tenant/orders/${detail.value.order.id}/contracts`, {
    fileKey: uploaded.objectKey,
    fileName: file.name
  })
  ElMessage.success('合同已上传')
  ;(event.target as HTMLInputElement).value = ''
  await openDetail(detail.value.order)
}

/* ---------------- 代客下单 ---------------- */
const createDialog = ref(false)
const createForm = reactive<any>({})

function openCreate() {
  Object.keys(createForm).forEach((k) => delete createForm[k])
  Object.assign(createForm, {
    careMode: '一对一',
    source: '门店',
    companionCount: 1,
    discountAmount: 0
  })
  createDialog.value = true
}

function autoCheckout() {
  const pkg = packages.value.find((p) => p.id === createForm.packageId)
  if (!pkg || !createForm.checkinDate) return
  const d = new Date(createForm.checkinDate)
  d.setDate(d.getDate() + Number(pkg.days || 28))
  createForm.checkoutDate = d.toISOString().slice(0, 10)
}

async function submitCreate() {
  if (!createForm.customerName || !createForm.phone || !createForm.packageId || !createForm.roomId) {
    ElMessage.warning('请完善客户、套餐与房间')
    return
  }
  saving.value = true
  try {
    const res: any = await tenantHttp.post('/api/tenant/orders', createForm)
    ElMessage.success(`订单已创建并锁定档期：${res.data?.orderNo || ''}`)
    createDialog.value = false
    loadAll()
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await loadRefs()
  await loadAll()
  if (route.query.status) {
    query.status = String(route.query.status)
    load()
  }
  if (route.query.tab === 'refund') {
    tab.value = 'refund'
    loadRefunds()
  }
})
</script>

<style scoped>
.stat-row { display: grid; grid-template-columns: repeat(auto-fit, minmax(170px, 1fr)); gap: var(--space-3); }
.stat { background: var(--surface); border: 1px solid var(--line); border-radius: var(--radius-lg); padding: 14px 16px; display: flex; flex-direction: column; gap: 2px; box-shadow: var(--shadow-xs); }
.stat-label { font-size: 11.5px; color: var(--ink-400); letter-spacing: 0.04em; }
.stat-value { font-size: 22px; font-weight: 650; color: var(--ink-900); font-variant-numeric: tabular-nums; }
.stat-hint { font-style: normal; font-size: 11.5px; color: var(--ink-400); }

.biz-tabs :deep(.el-tabs__header) { margin-bottom: 0; padding: 0 4px; border-bottom: 1px solid var(--line); }
.biz-tabs :deep(.el-tabs__nav-wrap::after) { display: none; }
.biz-tabs :deep(.el-tabs__item) { height: 42px; font-size: 13.5px; color: var(--ink-500); }
.biz-tabs :deep(.el-tabs__item.is-active) { color: var(--brand-700); font-weight: 600; }
.biz-tabs :deep(.el-tabs__active-bar) { background: var(--brand-600); }
.tab-badge { display: inline-block; margin-left: 6px; padding: 0 5px; height: 16px; line-height: 16px; border-radius: 8px; background: #fbeeed; color: #a83c37; font-size: 10.5px; }

.filter-bar { padding: 14px var(--space-5); border-bottom: 1px solid var(--line); background: var(--surface-sunken); }
.pager { display: flex; justify-content: flex-end; padding: 12px var(--space-5); border-top: 1px solid var(--line); }

.badge { display: inline-flex; align-items: center; height: 22px; padding: 0 8px; border-radius: 999px; font-size: 12px; background: #eef2f7; color: #4a5f7a; white-space: nowrap; }
.badge.ok { background: #eef7f1; color: #2f7d55; }
.badge.warn { background: #fdf5e8; color: #9a6a18; }
.badge.danger { background: #fbeeed; color: #a83c37; }
.badge.muted { background: #f4f3ef; color: var(--ink-500); }
.badge.info { background: #eef2f7; color: #4a5f7a; }
.money { font-variant-numeric: tabular-nums; }

.detail { display: flex; flex-direction: column; gap: var(--space-4); }
.amount-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; }
.amount-cell { background: var(--surface-sunken); border: 1px solid var(--line); border-radius: var(--radius-md); padding: 10px 12px; display: flex; flex-direction: column; gap: 4px; }
.amount-cell span { font-size: 11.5px; color: var(--ink-400); }
.amount-cell strong { font-size: 17px; font-variant-numeric: tabular-nums; }
.amount-cell strong.ok { color: #2f7d55; }
.amount-cell strong.warn { color: #a8672c; }
.breakdown { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; }
.breakdown div { display: flex; justify-content: space-between; font-size: 12.5px; padding: 6px 10px; border: 1px dashed var(--line); border-radius: var(--radius-sm); }
.breakdown span { color: var(--ink-400); }
.breakdown b { font-variant-numeric: tabular-nums; }
.detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px 16px; }
.detail-item { display: flex; flex-direction: column; gap: 2px; }
.detail-item span { font-size: 11.5px; color: var(--ink-400); }
.detail-item strong { font-size: 13px; font-weight: 550; }
.drawer-actions { display: flex; align-items: center; gap: 8px; padding: 10px 0; border-top: 1px solid var(--line); border-bottom: 1px solid var(--line); }
.drawer-actions .spacer { flex: 1; }
.section-label { margin: 4px 0 0; font-size: 11.5px; letter-spacing: 0.08em; color: var(--ink-400); font-weight: 600; }
.change-list, .contract-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 8px; }
.change-list li { display: flex; align-items: center; gap: 10px; font-size: 12.5px; }
.change-diff { color: var(--ink-700); }
.change-list em { margin-left: auto; font-style: normal; font-size: 11.5px; color: var(--ink-400); }
.contract-list li { display: flex; align-items: center; gap: 8px; font-size: 12.5px; }
.contract-list a { color: var(--brand-700); text-decoration: none; }
.contract-list a:hover { text-decoration: underline; }
.contract-list em { margin-left: auto; font-style: normal; font-size: 11.5px; }
.empty { padding: 26px 0; text-align: center; color: var(--ink-400); font-size: 12px; }
.empty strong { display: block; color: var(--ink-700); font-size: 13px; margin-bottom: 4px; }
.two-col { display: grid; grid-template-columns: 1fr 1fr; gap: 0 16px; }
.two-col :deep(.full) { grid-column: 1 / -1; }
.hint { margin: 4px 0 0; font-size: 11.5px; color: var(--ink-400); }
</style>
