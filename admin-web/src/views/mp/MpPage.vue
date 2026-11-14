<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>公众号运营</h1>
        <p class="sub">账号台账 → 接口基础层 → 统计/标签/粉丝/消息/模板/菜单/自动回复/素材图文；凭证未完善时其余子页只读</p>
      </div>
      <div class="actions">
        <el-select v-model="accountId" placeholder="选择公众号" style="width: 220px" @change="onAccountChange">
          <el-option v-for="a in accounts" :key="a.id" :label="`${a.accountName}（${a.accountType || '订阅号'}）`" :value="a.id" />
        </el-select>
        <el-button type="primary" @click="openAccount()">
          <Icon name="plus" :size="15" /><span style="margin-left: 6px">新增公众号</span>
        </el-button>
      </div>
    </div>

    <div v-if="!accounts.length" class="card empty-card">
      <Icon name="wechat" :size="26" />
      <strong>还没有接入公众号</strong>
      <p>先录入 AppID 与 AppSecret，服务器地址会自动生成为 <span class="mono">/api/open/mp/callback/{第 1 个 AppID}</span></p>
      <el-button type="primary" @click="openAccount()">开始接入</el-button>
    </div>

    <template v-else>
      <div class="ready-bar" :class="{ warn: !ready }">
        <Icon :name="ready ? 'check' : 'alert'" :size="15" />
        <span v-if="ready">凭证已完善，可调用微信接口（本地未配置真实凭证时数据由本地事件流降级提供）</span>
        <span v-else>凭证未完善：请输入 AppSecret、消息令牌与加解密密钥后，标签/粉丝/消息/菜单等子页才可操作</span>
        <span class="spacer" />
        <el-button size="small" text @click="openAccount(currentAccount)">编辑凭证</el-button>
      </div>

      <el-tabs v-model="tab" class="biz-tabs" @tab-change="onTab">
        <el-tab-pane label="账号台账" name="account">
          <div class="card flush">
            <el-table v-loading="loading" :data="accounts" size="small" stripe>
              <el-table-column prop="accountName" label="公众号名称" min-width="150" />
              <el-table-column prop="wechatId" label="微信号" width="140" />
              <el-table-column prop="appId" label="AppID" min-width="170">
                <template #default="{ row }"><span class="mono">{{ row.appId }}</span></template>
              </el-table-column>
              <el-table-column prop="appSecret" label="AppSecret" width="140">
                <template #default="{ row }"><span class="mono">{{ mask(row.appSecret) }}</span></template>
              </el-table-column>
              <el-table-column prop="accountType" label="类型" width="100" />
              <el-table-column label="认证" width="90">
                <template #default="{ row }">
                  <span class="badge" :class="row.verified ? 'ok' : 'muted'">{{ row.verified ? '已认证' : '未认证' }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="serverUrl" label="服务器地址" min-width="220">
                <template #default="{ row }"><span class="mono">{{ row.serverUrl || '-' }}</span></template>
              </el-table-column>
              <el-table-column label="操作" width="220" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click="openAccount(row)">编辑</el-button>
                  <el-button link type="primary" @click="qrcode(row)">生成二维码</el-button>
                  <el-button link type="danger" @click="clearQuota(row)">清配额</el-button>
                  <el-button link type="danger" @click="removeAccount(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>

        <el-tab-pane label="数据统计" name="stats">
          <div class="toolbar">
            <el-date-picker v-model="statRange" type="daterange" size="small" value-format="YYYY-MM-DD" start-placeholder="起" end-placeholder="止" style="width: 240px" @change="loadStats" />
            <el-button size="small" text @click="last7">近 7 天</el-button>
            <span class="spacer" />
            <span class="badge" :class="stats.simulated ? 'warn' : 'ok'">{{ stats.simulated ? '本地降级数据' : '微信实时数据' }}</span>
          </div>
          <div class="stat-charts">
            <div v-for="block in statBlocks" :key="block.key" class="chart-box">
              <p class="card-title">{{ block.label }}<small>{{ block.hint }}</small></p>
              <div v-if="block.rows.length" class="mini-bars">
                <div v-for="(row, i) in block.rows" :key="i" class="mini-bar-row">
                  <span class="date mono">{{ row.date }}</span>
                  <div class="mini-bar"><span :style="{ width: barWidth(row.value, block.rows) }" /></div>
                  <span class="num">{{ row.value }}</span>
                </div>
              </div>
              <div v-else class="empty-mini">该区间暂无数据</div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="标签管理" name="tag">
          <div class="card">
            <div class="toolbar">
              <el-input v-model="tagName" placeholder="标签名称" style="width: 220px" />
              <el-button type="primary" :disabled="!ready" @click="addTag">新增标签</el-button>
              <el-button :disabled="!ready" @click="syncTags">同步微信标签</el-button>
              <span class="spacer" />
              <span class="muted">先落库后推送，推送失败标记为同步失败，可重试</span>
            </div>
            <el-table :data="tags" size="small" stripe style="margin-top: 14px">
              <el-table-column prop="tagName" label="标签" min-width="160" />
              <el-table-column prop="fanCount" label="粉丝数" width="110" align="right" />
              <el-table-column label="同步状态" width="130">
                <template #default="{ row }">
                  <span class="badge" :class="row.syncStatus === '已同步' ? 'ok' : row.syncStatus === '同步失败' ? 'danger' : 'warn'">{{ row.syncStatus }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="createdTime" label="创建时间" width="170" />
              <el-table-column label="操作" width="100">
                <template #default="{ row }">
                  <el-button link type="danger" @click="removeTag(row)">删除</el-button>
                </template>
              </el-table-column>
              <template #empty><div class="empty">暂无标签，新增后会自动推送到微信</div></template>
            </el-table>
          </div>
        </el-tab-pane>

        <el-tab-pane label="粉丝管理" name="fan">
          <div class="card flush">
            <div class="toolbar filter-bar">
              <el-input v-model="fanKeyword" placeholder="昵称模糊搜索" clearable size="small" style="width: 200px" />
              <el-button size="small" :disabled="!ready" @click="syncFans">同步粉丝</el-button>
              <span class="spacer" />
              <span class="muted">共 {{ fans.length }} 位粉丝</span>
            </div>
            <el-table :data="filteredFans" size="small" stripe>
              <el-table-column label="粉丝" min-width="180">
                <template #default="{ row }">
                  <div class="fan-cell">
                    <img v-if="row.avatar" :src="row.avatar" alt="" />
                    <span v-else class="avatar">{{ (row.nickname || '粉').slice(0, 1) }}</span>
                    <div>
                      <strong>{{ row.nickname || '微信用户' }}</strong>
                      <em class="mono">{{ row.openId }}</em>
                    </div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="gender" label="性别" width="80" />
              <el-table-column prop="region" label="地区" width="140" />
              <el-table-column label="订阅" width="100">
                <template #default="{ row }">
                  <span class="badge" :class="row.subscribeStatus === '已关注' ? 'ok' : 'muted'">{{ row.subscribeStatus }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="remark" label="备注" min-width="140" />
              <el-table-column prop="subscribeTime" label="关注时间" width="160" />
              <el-table-column label="操作" width="200" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click="copyOpenId(row)">复制标识</el-button>
                  <el-button link type="primary" @click="editRemark(row)">备注</el-button>
                  <el-button link type="primary" @click="bindFanTag(row)">打标签</el-button>
                </template>
              </el-table-column>
              <template #empty><div class="empty">暂无粉丝，点击「同步粉丝」从微信拉取</div></template>
            </el-table>
          </div>
        </el-tab-pane>

        <el-tab-pane label="消息管理" name="message">
          <div class="card flush">
            <div class="toolbar filter-bar">
              <el-select v-model="msgType" placeholder="全部类型" clearable size="small" style="width: 140px" @change="loadMessages">
                <el-option v-for="t in msgTypes" :key="t" :label="t" :value="t" />
              </el-select>
              <span class="spacer" />
              <span class="muted">共 {{ messages.length }} 条（保留期按系统参数配置）</span>
            </div>
            <el-table :data="messages" size="small" stripe>
              <el-table-column prop="createdTime" label="时间" width="160" />
              <el-table-column prop="msgType" label="类型" width="100" />
              <el-table-column prop="sender" label="方向" width="100" />
              <el-table-column prop="openId" label="用户标识" width="200">
                <template #default="{ row }"><span class="mono">{{ row.openId }}</span></template>
              </el-table-column>
              <el-table-column prop="content" label="内容" min-width="240" />
              <el-table-column label="结果" width="120">
                <template #default="{ row }">
                  <span v-if="row.errorCode" class="badge danger">{{ row.errorCode }}</span>
                  <span v-else class="badge ok">正常</span>
                </template>
              </el-table-column>
              <el-table-column label="报文" width="80">
                <template #default="{ row }">
                  <el-button link type="primary" @click="showRaw(row)">查看</el-button>
                </template>
              </el-table-column>
              <template #empty><div class="empty">暂无消息，公众号回调会实时落库</div></template>
            </el-table>
          </div>
        </el-tab-pane>

        <el-tab-pane label="模版消息" name="template">
          <el-alert
            type="info"
            :closable="false"
            show-icon
            title="模板只读：发送与节点映射统一在「系统设置 → 消息配置」维护，避免两处定义冲突"
            style="margin-bottom: 12px"
          />
          <div class="card flush">
            <el-table :data="templates" size="small" stripe>
              <el-table-column prop="templateId" label="模板编号" width="240">
                <template #default="{ row }"><span class="mono">{{ row.templateId || row.id }}</span></template>
              </el-table-column>
              <el-table-column prop="title" label="标题" min-width="180" />
              <el-table-column prop="content" label="内容" min-width="260" />
              <el-table-column prop="industry" label="行业" width="140" />
              <template #empty><div class="empty">未同步模板，凭证完善后可拉取</div></template>
            </el-table>
          </div>
        </el-tab-pane>

        <el-tab-pane label="菜单管理" name="menu">
          <div class="menu-layout">
            <div class="card">
              <p class="card-title">菜单配置<small>一级 ≤3、二级 ≤5，层级超限会被拦截</small></p>
              <el-select v-model="menuConfigId" placeholder="选择菜单配置" style="width: 100%; margin-bottom: 10px" @change="pickMenu">
                <el-option v-for="m in menus" :key="m.id" :label="`${m.configName}${m.published ? '（已发布）' : ''}`" :value="m.id" />
              </el-select>
              <el-input v-model="menuName" placeholder="配置名称，如 默认菜单" style="margin-bottom: 10px" />
              <div class="menu-editor">
                <div v-for="(top, ti) in menuTree" :key="ti" class="menu-top">
                  <div class="menu-top-head">
                    <el-input v-model="top.name" size="small" placeholder="一级菜单名称" style="width: 150px" />
                    <el-button size="small" text @click="addChild(ti)">加子菜单</el-button>
                    <el-button size="small" text type="danger" @click="removeTop(ti)">删除</el-button>
                  </div>
                  <div v-for="(child, ci) in top.children" :key="ci" class="menu-child">
                    <el-input v-model="child.name" size="small" placeholder="子菜单名称" style="width: 130px" />
                    <el-select v-model="child.type" size="small" style="width: 100px">
                      <el-option label="跳转链接" value="view" />
                      <el-option label="关键字" value="click" />
                      <el-option label="小程序" value="miniprogram" />
                    </el-select>
                    <el-input v-model="child.value" size="small" placeholder="链接 / 关键字 / 小程序路径" style="flex: 1" />
                    <el-button size="small" text type="danger" @click="top.children.splice(ci, 1)">×</el-button>
                  </div>
                </div>
                <el-button size="small" @click="addTop" :disabled="menuTree.length >= 3">新增一级菜单</el-button>
              </div>
              <div class="menu-actions">
                <el-button :disabled="!ready" @click="clearMenu">清空菜单</el-button>
                <el-button type="primary" :loading="saving" :disabled="!ready" @click="saveMenu">保存并发布</el-button>
              </div>
            </div>

            <div class="card preview-card">
              <p class="card-title">实时预览</p>
              <div class="phone">
                <div class="phone-body">
                  <div class="phone-content">
                    <p class="muted">公众号会话</p>
                  </div>
                  <div class="phone-menu">
                    <div v-for="(top, i) in menuTree" :key="i" class="phone-menu-item">{{ top.name || '菜单' }}</div>
                  </div>
                </div>
              </div>
              <p class="muted preview-hint">发布失败会保留旧菜单，不会出现空菜单状态</p>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="自动回复" name="reply">
          <div class="card">
            <div class="toolbar">
              <el-radio-group v-model="replyType" size="small" @change="loadReplies">
                <el-radio-button label="关注时">关注时</el-radio-button>
                <el-radio-button label="消息">消息</el-radio-button>
                <el-radio-button label="关键词">关键词</el-radio-button>
              </el-radio-group>
              <span class="spacer" />
              <el-button size="small" type="primary" :disabled="!ready" @click="openReply">新增回复</el-button>
            </div>

            <el-table :data="replies" size="small" stripe style="margin-top: 14px">
              <el-table-column v-if="replyType === '关键词'" prop="keyword" label="关键词" width="150" />
              <el-table-column v-if="replyType === '关键词'" prop="matchType" label="匹配" width="100" />
              <el-table-column prop="msgType" label="回复类型" width="110" />
              <el-table-column prop="content" label="内容" min-width="260" />
              <el-table-column prop="createdTime" label="创建时间" width="170" />
              <el-table-column label="操作" width="90">
                <template #default="{ row }">
                  <el-button link type="danger" @click="removeReply(row)">删除</el-button>
                </template>
              </el-table-column>
              <template #empty><div class="empty">该类型还没有回复配置</div></template>
            </el-table>
          </div>
        </el-tab-pane>

        <el-tab-pane label="素材与图文" name="material">
          <div class="material-layout">
            <div class="card">
              <p class="card-title">素材上传<small>图片 ≤2MB、语音 ≤60 秒、视频需缩略图</small></p>
              <div class="toolbar">
                <el-select v-model="materialType" size="small" style="width: 120px">
                  <el-option label="图片" value="图片" />
                  <el-option label="语音" value="语音" />
                  <el-option label="视频" value="视频" />
                </el-select>
                <input ref="materialInput" type="file" hidden @change="uploadMaterial" />
                <el-button size="small" :disabled="!ready" @click="materialInput?.click()">选择文件</el-button>
                <span class="muted">文件先入对象存储，再登记到公众号素材库</span>
              </div>
              <ul class="material-list">
                <li v-for="m in materials" :key="m.id">
                  <span class="badge info">{{ m.materialType }}</span>
                  <span class="name">{{ m.name }}</span>
                  <em class="muted">{{ sizeText(m.sizeBytes) }}</em>
                  <span v-if="m.mediaId" class="mono media-id">{{ m.mediaId }}</span>
                </li>
                <li v-if="!materials.length" class="muted">暂无素材</li>
              </ul>
            </div>

            <div class="card">
              <p class="card-title">图文草稿与发表<small>多篇合成多图文，发表记录保留状态与失败原因</small></p>
              <div class="toolbar">
                <el-button size="small" :disabled="!ready" @click="openDraft">新建草稿</el-button>
              </div>
              <el-table :data="drafts" size="small" stripe style="margin-top: 12px">
                <el-table-column prop="title" label="标题" min-width="160" />
                <el-table-column prop="articleIndex" label="序号" width="80" align="center" />
                <el-table-column prop="status" label="状态" width="110">
                  <template #default="{ row }">
                    <span class="badge" :class="row.status === '已发表' ? 'ok' : row.status === '发表失败' ? 'danger' : 'muted'">{{ row.status }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="publishTime" label="发表时间" width="160" />
                <el-table-column prop="failReason" label="失败原因" min-width="140" />
                <el-table-column label="操作" width="100">
                  <template #default="{ row }">
                    <el-button v-if="row.status !== '已发表'" link type="primary" :disabled="!ready" @click="publishDraft(row)">发表</el-button>
                    <span v-else class="muted">已发表</span>
                  </template>
                </el-table-column>
                <template #empty><div class="empty">暂无图文草稿</div></template>
              </el-table>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </template>

    <!-- 账号表单 -->
    <el-drawer v-model="accountDrawer" :title="form.id ? '编辑公众号' : '接入公众号'" size="640px">
      <el-form :model="form" label-position="top" class="form-grid">
        <p class="section-label full">基础信息</p>
        <el-form-item label="公众号名称" required><el-input v-model="form.accountName" /></el-form-item>
        <el-form-item label="微信号"><el-input v-model="form.wechatId" /></el-form-item>
        <el-form-item label="AppID" required><el-input v-model="form.appId" placeholder="wx 开头" /></el-form-item>
        <el-form-item label="AppSecret" required><el-input v-model="form.appSecret" type="password" show-password placeholder="留空表示不修改" /></el-form-item>
        <el-form-item label="公众号类型">
          <el-select v-model="form.accountType" style="width: 100%">
            <el-option label="订阅号" value="订阅号" />
            <el-option label="服务号" value="服务号" />
            <el-option label="测试号" value="测试号" />
          </el-select>
        </el-form-item>
        <el-form-item label="微信认证"><el-switch v-model="form.verified" :active-value="1" :inactive-value="0" /></el-form-item>

        <p class="section-label full">服务器与安全</p>
        <el-form-item label="服务器地址（自动生成）" class="full">
          <el-input :model-value="serverUrlPreview" readonly>
            <template #append><el-button @click="copy(serverUrlPreview)">复制</el-button></template>
          </el-input>
        </el-form-item>
        <el-form-item label="消息令牌 Token" required><el-input v-model="form.token" type="password" show-password /></el-form-item>
        <el-form-item label="消息加解密密钥"><el-input v-model="form.aesKey" type="password" show-password /></el-form-item>
        <el-form-item label="网页授权域名"><el-input v-model="form.oauthDomain" placeholder="如 h5.yuezizhongxin.com" /></el-form-item>
        <el-form-item label="JS 安全域名"><el-input v-model="form.jsSafeDomain" /></el-form-item>
        <el-form-item label="备注" class="full"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <el-alert type="warning" :closable="false" show-icon title="IP 白名单提示：请把服务器出口 IP 加入微信公众平台「IP 白名单」，否则接口会返回 40164" style="margin-top: 10px" />
      <template #footer>
        <div class="drawer-footer">
          <el-button @click="accountDrawer = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="saveAccount">保存</el-button>
        </div>
      </template>
    </el-drawer>

    <!-- 自动回复 -->
    <el-dialog v-model="replyDialog" title="新增自动回复" width="520px">
      <el-form :model="replyForm" label-position="top">
        <el-form-item v-if="replyType === '关键词'" label="关键词" required><el-input v-model="replyForm.keyword" /></el-form-item>
        <el-form-item v-if="replyType === '关键词'" label="匹配方式">
          <el-radio-group v-model="replyForm.matchType">
            <el-radio-button label="全匹配" />
            <el-radio-button label="半匹配" />
          </el-radio-group>
        </el-form-item>
        <el-form-item label="回复类型">
          <el-select v-model="replyForm.msgType" style="width: 100%">
            <el-option label="文本" value="文本" />
            <el-option label="图片" value="图片" />
            <el-option label="图文" value="图文" />
            <el-option label="语音" value="语音" />
            <el-option label="视频" value="视频" />
            <el-option label="小程序卡片" value="小程序" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容" required><el-input v-model="replyForm.content" type="textarea" :rows="4" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="replyDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveReply">保存</el-button>
      </template>
    </el-dialog>

    <!-- 图文草稿 -->
    <el-dialog v-model="draftDialog" title="新建图文草稿" width="640px">
      <el-form :model="draftForm" label-position="top" class="form-grid">
        <el-form-item label="标题" class="full" required><el-input v-model="draftForm.title" /></el-form-item>
        <el-form-item label="作者"><el-input v-model="draftForm.author" /></el-form-item>
        <el-form-item label="原文链接"><el-input v-model="draftForm.sourceUrl" /></el-form-item>
        <el-form-item label="摘要" class="full"><el-input v-model="draftForm.digest" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="封面" class="full">
          <ImageUploader v-model="draftForm.coverKey" dir="mp" :limit="1" />
        </el-form-item>
        <el-form-item label="正文" class="full">
          <RichEditor v-model="draftForm.bodyHtml" :min-height="220" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="draftDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveDraft">保存草稿</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import Icon from '../../components/Icon.vue'
import ImageUploader from '../../components/ImageUploader.vue'
import RichEditor from '../../components/RichEditor.vue'
import { tenantHttp } from '../../api/client'
import { uploadFile } from '../../api/resource'

const loading = ref(false)
const saving = ref(false)
const tab = ref('account')
const accounts = ref<any[]>([])
const accountId = ref<number>()
const ready = ref(false)

const currentAccount = computed(() => accounts.value.find((a) => a.id === accountId.value))

const tags = ref<any[]>([])
const fans = ref<any[]>([])
const messages = ref<any[]>([])
const templates = ref<any[]>([])
const menus = ref<any[]>([])
const replies = ref<any[]>([])
const materials = ref<any[]>([])
const drafts = ref<any[]>([])
const stats = ref<any>({})

const msgTypes = ['文本', '图片', '语音', '视频', '图文', '事件', '客服消息']

function mask(value: string) {
  if (!value) return '未设置'
  return value.length <= 8 ? '••••••' : `${value.slice(0, 4)}••••${value.slice(-4)}`
}

function copy(text: string) {
  navigator.clipboard?.writeText(text).then(
    () => ElMessage.success('已复制'),
    () => ElMessage.info(text)
  )
}

function sizeText(bytes: number) {
  if (!bytes) return '0 KB'
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

async function loadAccounts() {
  const res: any = await tenantHttp.get('/api/tenant/mp/accounts')
  accounts.value = res.data || []
  if (!accountId.value && accounts.value.length) {
    accountId.value = accounts.value[0].id
  }
  await checkReady()
}

async function checkReady() {
  if (!accountId.value) {
    ready.value = false
    return
  }
  try {
    const res: any = await tenantHttp.get(`/api/tenant/mp/accounts/${accountId.value}/ready`)
    ready.value = Boolean(res.data)
  } catch {
    ready.value = false
  }
}

async function onAccountChange() {
  await checkReady()
  await loadTab()
}

/* ---------------- 账号 ---------------- */
const accountDrawer = ref(false)
const form = reactive<any>({})

const serverUrlPreview = computed(() => {
  if (!form.appId) return '保存后自动生成'
  return form.serverUrl || `${location.origin}/api/open/mp/callback/${form.appId}`
})

function openAccount(row?: any) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, { accountType: '订阅号', verified: 0 })
  if (row) Object.assign(form, { ...row, appSecret: '', token: '', aesKey: '' })
  accountDrawer.value = true
}

async function saveAccount() {
  if (!form.accountName || !form.appId) {
    ElMessage.warning('请填写公众号名称与 AppID')
    return
  }
  saving.value = true
  try {
    await tenantHttp.post('/api/tenant/mp/accounts', { ...form })
    ElMessage.success('已保存，凭证加密存储且列表仅显示掩码')
    accountDrawer.value = false
    await loadAccounts()
  } finally {
    saving.value = false
  }
}

async function qrcode(row: any) {
  const res: any = await tenantHttp.get(`/api/tenant/mp/accounts/${row.id}/qrcode`)
  ElMessageBox.alert(res.data?.url || res.data?.qrcode || '未返回二维码', '公众号二维码', { type: 'info' })
}

async function clearQuota(row: any) {
  await ElMessageBox.confirm('清空接口配额会重置微信侧调用计数，确认？', '清空配额', { type: 'warning' })
  await tenantHttp.post(`/api/tenant/mp/accounts/${row.id}/clear-quota`)
  ElMessage.success('已清空')
}

async function removeAccount(row: any) {
  await ElMessageBox.confirm(`删除公众号「${row.accountName}」将同时清理粉丝与消息关联，确认？`, '删除公众号', { type: 'warning' })
  await tenantHttp.delete(`/api/tenant/mp/accounts/${row.id}`)
  ElMessage.success('已删除')
  accountId.value = undefined
  await loadAccounts()
}

/* ---------------- 统计 ---------------- */
const statRange = ref<string[] | null>(null)

const statBlocks = computed(() => {
  const toRows = (list: any[], valueKeys: string[]) => {
    if (!Array.isArray(list)) return []
    return list.map((row: any) => {
      const date = row.ref_date || row.date || row.stat_date || '-'
      const value = valueKeys.reduce((sum, k) => sum + Number(row[k] || 0), 0)
      return { date, value }
    })
  }
  return [
    { key: 'userSummary', label: '用户增减', hint: '新关注 - 取消关注', rows: toRows(stats.value.userSummary, ['new_user', 'cancel_user']) },
    { key: 'userCumulative', label: '累计用户', hint: '截至当日总用户', rows: toRows(stats.value.userCumulative, ['cumulate_user']) },
    { key: 'messageSummary', label: '消息概况', hint: '上行消息量', rows: toRows(stats.value.messageSummary, ['msg_count', 'count']) },
    { key: 'interfaceSummary', label: '接口分析', hint: '调用次数与失败', rows: toRows(stats.value.interfaceSummary, ['total_count', 'call_count']) }
  ]
})

function barWidth(value: number, rows: any[]) {
  const max = Math.max(1, ...rows.map((r) => Number(r.value || 0)))
  return `${Math.max(2, (Number(value) / max) * 100)}%`
}

function last7() {
  const end = new Date()
  const start = new Date()
  start.setDate(start.getDate() - 6)
  statRange.value = [start.toISOString().slice(0, 10), end.toISOString().slice(0, 10)]
  loadStats()
}

async function loadStats() {
  if (!accountId.value) return
  const res: any = await tenantHttp.get(`/api/tenant/mp/accounts/${accountId.value}/stats`, {
    params: { from: statRange.value?.[0], to: statRange.value?.[1] }
  })
  stats.value = res.data || {}
}

/* ---------------- 标签 ---------------- */
const tagName = ref('')

async function loadTags() {
  if (!accountId.value) return
  const res: any = await tenantHttp.get(`/api/tenant/mp/accounts/${accountId.value}/tags`)
  tags.value = res.data || []
}

async function addTag() {
  if (!tagName.value) {
    ElMessage.warning('请输入标签名称')
    return
  }
  await tenantHttp.post(`/api/tenant/mp/accounts/${accountId.value}/tags`, { tagName: tagName.value })
  ElMessage.success('标签已创建并推送')
  tagName.value = ''
  loadTags()
}

async function syncTags() {
  await tenantHttp.post(`/api/tenant/mp/accounts/${accountId.value}/tags/sync`)
  ElMessage.success('已同步微信标签')
  loadTags()
}

async function removeTag(row: any) {
  await ElMessageBox.confirm(`删除标签「${row.tagName}」会同时从微信移除，确认？`, '删除标签', { type: 'warning' })
  await tenantHttp.delete(`/api/tenant/mp/tags/${row.id}`)
  ElMessage.success('已删除')
  loadTags()
}

/* ---------------- 粉丝 ---------------- */
const fanKeyword = ref('')
const filteredFans = computed(() => {
  if (!fanKeyword.value) return fans.value
  return fans.value.filter((f) => String(f.nickname || '').includes(fanKeyword.value))
})

async function loadFans() {
  if (!accountId.value) return
  const res: any = await tenantHttp.get(`/api/tenant/mp/accounts/${accountId.value}/fans`, {
    params: { pageNum: 1, pageSize: 200 }
  })
  fans.value = res.data?.records || res.data || []
}

async function syncFans() {
  await tenantHttp.post(`/api/tenant/mp/accounts/${accountId.value}/fans/sync`)
  ElMessage.success('粉丝已同步')
  loadFans()
}

function copyOpenId(row: any) {
  copy(row.openId)
}

async function editRemark(row: any) {
  const { value } = await ElMessageBox.prompt('粉丝备注（仅本地可见）', '修改备注', {
    inputValue: row.remark || ''
  })
  await tenantHttp.put(`/api/tenant/mp/fans/${row.id}/remark`, { remark: value })
  ElMessage.success('备注已更新')
  loadFans()
}

async function bindFanTag(row: any) {
  if (!tags.value.length) {
    ElMessage.warning('请先在标签管理中创建标签')
    return
  }
  const { value } = await ElMessageBox.prompt(
    `输入标签名称：${tags.value.map((t) => t.tagName).join('、')}`,
    '给粉丝打标签'
  )
  const tag = tags.value.find((t) => t.tagName === value)
  if (!tag) {
    ElMessage.warning('未找到该标签')
    return
  }
  await tenantHttp.post(`/api/tenant/mp/fans/${row.id}/tags/${tag.id}`)
  ElMessage.success('已打标签')
}

/* ---------------- 消息 ---------------- */
const msgType = ref('')

async function loadMessages() {
  if (!accountId.value) return
  const res: any = await tenantHttp.get(`/api/tenant/mp/accounts/${accountId.value}/messages`, {
    params: { msgType: msgType.value || undefined, pageNum: 1, pageSize: 100 }
  })
  messages.value = res.data?.records || res.data || []
}

function showRaw(row: any) {
  ElMessageBox.alert(
    `<pre style="max-height:340px;overflow:auto;font-size:12px;white-space:pre-wrap">${(row.rawPayload || '无原始报文').replace(/</g, '&lt;')}</pre>`,
    '原始报文',
    { dangerouslyUseHTMLString: true }
  )
}

/* ---------------- 模板 ---------------- */
async function loadTemplates() {
  if (!accountId.value) return
  const res: any = await tenantHttp.get(`/api/tenant/mp/accounts/${accountId.value}/templates`)
  templates.value = res.data || []
}

/* ---------------- 菜单 ---------------- */
const menuConfigId = ref<number>()
const menuName = ref('默认菜单')
const menuTree = ref<any[]>([])

async function loadMenus() {
  if (!accountId.value) return
  const res: any = await tenantHttp.get(`/api/tenant/mp/accounts/${accountId.value}/menus`)
  menus.value = res.data || []
  if (menus.value.length) {
    menuConfigId.value = menus.value[0].id
    pickMenu(menuConfigId.value)
  } else {
    menuTree.value = []
    menuName.value = '默认菜单'
  }
}

function pickMenu(id: number) {
  const config = menus.value.find((m) => m.id === id)
  if (!config) return
  menuName.value = config.configName
  try {
    const parsed = JSON.parse(config.treeJson || '[]')
    menuTree.value = Array.isArray(parsed) ? parsed : []
  } catch {
    menuTree.value = []
  }
}

function addTop() {
  if (menuTree.value.length >= 3) {
    ElMessage.warning('一级菜单最多 3 个')
    return
  }
  menuTree.value.push({ name: '', children: [] })
}

function addChild(index: number) {
  const top = menuTree.value[index]
  if ((top.children?.length || 0) >= 5) {
    ElMessage.warning('二级菜单最多 5 个')
    return
  }
  top.children = top.children || []
  top.children.push({ name: '', type: 'view', value: '' })
}

/** 删除一级菜单（含其下全部子菜单，需二次确认） */
async function removeTop(index: number) {
  const top = menuTree.value[index]
  const childCount = top.children?.length || 0
  try {
    await ElMessageBox.confirm(
      childCount ? `该菜单下还有 ${childCount} 个子菜单，删除后一并移除，确认？` : '确认删除该一级菜单？',
      '删除菜单',
      { type: 'warning' }
    )
  } catch {
    return
  }
  menuTree.value.splice(index, 1)
}

async function saveMenu() {
  if (!menuTree.value.length) {
    ElMessage.warning('至少配置一个一级菜单')
    return
  }
  saving.value = true
  try {
    const res: any = await tenantHttp.post(`/api/tenant/mp/accounts/${accountId.value}/menus`, {
      id: menuConfigId.value,
      configName: menuName.value,
      treeJson: JSON.stringify(menuTree.value)
    })
    menuConfigId.value = res.data
    const pub: any = await tenantHttp.post(`/api/tenant/mp/menus/${res.data}/publish`)
    ElMessage.success(pub.data?.published === false ? '已保存，但发布失败已保留旧菜单' : '菜单已发布生效')
    loadMenus()
  } finally {
    saving.value = false
  }
}

async function clearMenu() {
  await ElMessageBox.confirm('清空后微信端菜单会立即消失，确认？', '清空菜单', { type: 'warning' })
  await tenantHttp.post(`/api/tenant/mp/accounts/${accountId.value}/menus/clear`)
  ElMessage.success('已清空')
  menuTree.value = []
  loadMenus()
}

/* ---------------- 自动回复 ---------------- */
const replyType = ref('关注时')
const replyDialog = ref(false)
const replyForm = reactive<any>({})

async function loadReplies() {
  if (!accountId.value) return
  const res: any = await tenantHttp.get(`/api/tenant/mp/accounts/${accountId.value}/replies`, {
    params: { replyType: replyType.value }
  })
  replies.value = res.data || []
}

function openReply() {
  Object.keys(replyForm).forEach((k) => delete replyForm[k])
  Object.assign(replyForm, { replyType: replyType.value, msgType: '文本', matchType: '全匹配' })
  replyDialog.value = true
}

async function saveReply() {
  if (!replyForm.content) {
    ElMessage.warning('请填写回复内容')
    return
  }
  saving.value = true
  try {
    await tenantHttp.post(`/api/tenant/mp/accounts/${accountId.value}/replies`, { ...replyForm })
    ElMessage.success('已保存')
    replyDialog.value = false
    loadReplies()
  } finally {
    saving.value = false
  }
}

async function removeReply(row: any) {
  await ElMessageBox.confirm('确认删除该自动回复？', '删除', { type: 'warning' })
  await tenantHttp.delete(`/api/tenant/mp/replies/${row.id}`)
  ElMessage.success('已删除')
  loadReplies()
}

/* ---------------- 素材与图文 ---------------- */
const materialType = ref('图片')
const materialInput = ref<HTMLInputElement>()

async function loadMaterials() {
  if (!accountId.value) return
  const res: any = await tenantHttp.get(`/api/tenant/mp/accounts/${accountId.value}/materials`, {
    params: { type: materialType.value }
  })
  materials.value = res.data || []
}

async function uploadMaterial(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  const uploaded = await uploadFile(file, 'mp')
  await tenantHttp.post(`/api/tenant/mp/accounts/${accountId.value}/materials`, {
    name: file.name,
    type: materialType.value,
    sizeBytes: file.size,
    fileKey: uploaded.objectKey
  })
  ElMessage.success('素材已登记（超过微信大小限制会被拦截）')
  ;(event.target as HTMLInputElement).value = ''
  loadMaterials()
}

async function loadDrafts() {
  if (!accountId.value) return
  const res: any = await tenantHttp.get(`/api/tenant/mp/accounts/${accountId.value}/drafts`)
  drafts.value = res.data || []
}

const draftDialog = ref(false)
const draftForm = reactive<any>({})

function openDraft() {
  Object.keys(draftForm).forEach((k) => delete draftForm[k])
  Object.assign(draftForm, { articleIndex: drafts.value.length + 1 })
  draftDialog.value = true
}

async function saveDraft() {
  if (!draftForm.title) {
    ElMessage.warning('请填写标题')
    return
  }
  saving.value = true
  try {
    await tenantHttp.post(`/api/tenant/mp/accounts/${accountId.value}/drafts`, { ...draftForm })
    ElMessage.success('草稿已保存')
    draftDialog.value = false
    loadDrafts()
  } finally {
    saving.value = false
  }
}

async function publishDraft(row: any) {
  await ElMessageBox.confirm('发表后将推送至公众号粉丝，确认？', '发表图文', { type: 'warning' })
  const res: any = await tenantHttp.post(`/api/tenant/mp/drafts/${row.id}/publish`)
  ElMessage.success(res.data?.published === false ? `发表失败：${res.data?.failReason || ''}` : '已发表')
  loadDrafts()
}

/* ---------------- tab 调度 ---------------- */
async function loadTab() {
  if (!accountId.value) return
  loading.value = true
  try {
    if (tab.value === 'stats') await loadStats()
    if (tab.value === 'tag') await loadTags()
    if (tab.value === 'fan') await loadFans()
    if (tab.value === 'message') await loadMessages()
    if (tab.value === 'template') await loadTemplates()
    if (tab.value === 'menu') await loadMenus()
    if (tab.value === 'reply') await loadReplies()
    if (tab.value === 'material') {
      await loadMaterials()
      await loadDrafts()
    }
  } finally {
    loading.value = false
  }
}

function onTab() {
  loadTab()
}

onMounted(async () => {
  await loadAccounts()
  if (!statRange.value) last7()
  await loadStats()
})
</script>

<style scoped>
.biz-tabs :deep(.el-tabs__header) { margin-bottom: 0; padding: 0 4px; border-bottom: 1px solid var(--line); }
.biz-tabs :deep(.el-tabs__nav-wrap::after) { display: none; }
.biz-tabs :deep(.el-tabs__item) { height: 42px; font-size: 13.5px; color: var(--ink-500); }
.biz-tabs :deep(.el-tabs__item.is-active) { color: var(--brand-700); font-weight: 600; }
.biz-tabs :deep(.el-tabs__active-bar) { background: var(--brand-600); }
.biz-tabs :deep(.el-tabs__content) { padding-top: var(--space-3); }

.empty-card { display: flex; flex-direction: column; align-items: center; gap: 10px; padding: 46px 0; color: var(--ink-400); }
.empty-card strong { color: var(--ink-800); font-size: 15px; }
.empty-card p { margin: 0; font-size: 12.5px; }

.ready-bar { display: flex; align-items: center; gap: 9px; padding: 10px 14px; border-radius: var(--radius-md); background: #eef7f1; color: #2f7d55; font-size: 12.5px; border: 1px solid #d6ebdd; }
.ready-bar.warn { background: #fdf5e8; color: #9a6a18; border-color: #f3e3c6; }
.ready-bar .spacer { flex: 1; }

.filter-bar { padding: 14px var(--space-5); border-bottom: 1px solid var(--line); background: var(--surface-sunken); }

.stat-charts { display: grid; grid-template-columns: 1fr 1fr; gap: var(--space-3); }
.chart-box { background: var(--surface); border: 1px solid var(--line); border-radius: var(--radius-lg); padding: 16px 18px; box-shadow: var(--shadow-xs); }
.card-title { margin: 0 0 12px; font-size: 14px; font-weight: 600; display: flex; align-items: baseline; justify-content: space-between; }
.card-title small { font-size: 11.5px; color: var(--ink-400); font-weight: 400; }
.mini-bars { display: flex; flex-direction: column; gap: 7px; max-height: 200px; overflow-y: auto; }
.mini-bar-row { display: flex; align-items: center; gap: 10px; font-size: 11.5px; }
.mini-bar-row .date { flex: 0 0 78px; color: var(--ink-400); }
.mini-bar { flex: 1; height: 7px; background: var(--line); border-radius: 999px; overflow: hidden; }
.mini-bar span { display: block; height: 100%; background: linear-gradient(90deg, #6cc3a5, #2f8b70); border-radius: 999px; }
.mini-bar-row .num { flex: 0 0 56px; text-align: right; color: var(--ink-700); font-variant-numeric: tabular-nums; }
.empty-mini { padding: 30px 0; text-align: center; font-size: 12px; color: var(--ink-400); }

.fan-cell { display: flex; align-items: center; gap: 9px; }
.fan-cell img, .fan-cell .avatar { width: 30px; height: 30px; border-radius: 50%; object-fit: cover; flex: none; }
.fan-cell .avatar { background: var(--brand-50); color: var(--brand-700); display: grid; place-items: center; font-size: 12px; font-weight: 600; }
.fan-cell div { display: flex; flex-direction: column; line-height: 1.3; }
.fan-cell strong { font-size: 13px; }
.fan-cell em { font-style: normal; font-size: 10.5px; color: var(--ink-400); }

.menu-layout { display: grid; grid-template-columns: 1fr 300px; gap: var(--space-4); align-items: start; }
.menu-editor { display: flex; flex-direction: column; gap: 10px; }
.menu-top { border: 1px solid var(--line); border-radius: var(--radius-md); padding: 10px 12px; background: var(--surface-sunken); }
.menu-top-head { display: flex; align-items: center; gap: 8px; }
.menu-child { display: flex; align-items: center; gap: 8px; margin-top: 8px; }
.menu-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 14px; }
.preview-card { position: sticky; top: 12px; }
.phone { width: 100%; display: flex; justify-content: center; }
.phone-body { width: 220px; height: 380px; border: 8px solid #2b2f2d; border-radius: 26px; background: #f7f7f5; display: flex; flex-direction: column; overflow: hidden; }
.phone-content { flex: 1; padding: 12px; display: flex; flex-direction: column; }
.phone-content p { margin: 0; font-size: 11px; }
.phone-menu { display: flex; border-top: 1px solid #ddd; background: #fff; }
.phone-menu-item { flex: 1; text-align: center; padding: 11px 4px; font-size: 11px; color: #333; border-right: 1px solid #eee; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.phone-menu-item:last-child { border-right: 0; }
.preview-hint { margin-top: 10px; font-size: 11.5px; text-align: center; }

.material-layout { display: grid; grid-template-columns: 1fr 1fr; gap: var(--space-4); align-items: start; }
.material-list { list-style: none; margin: 12px 0 0; padding: 0; display: flex; flex-direction: column; gap: 8px; }
.material-list li { display: flex; align-items: center; gap: 9px; font-size: 12.5px; }
.material-list .name { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.media-id { font-size: 10.5px; color: var(--ink-400); }

.badge { display: inline-flex; align-items: center; height: 22px; padding: 0 8px; border-radius: 999px; font-size: 12px; background: #eef2f7; color: #4a5f7a; }
.badge.ok { background: #eef7f1; color: #2f7d55; }
.badge.warn { background: #fdf5e8; color: #9a6a18; }
.badge.danger { background: #fbeeed; color: #a83c37; }
.badge.muted { background: #f4f3ef; color: var(--ink-500); }
.badge.info { background: #eef2f7; color: #4a5f7a; }
.empty { padding: 30px 0; text-align: center; color: var(--ink-400); font-size: 12px; }

.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0 16px; }
.form-grid :deep(.full) { grid-column: 1 / -1; }
.section-label { margin: 4px 0 8px; font-size: 11.5px; letter-spacing: 0.08em; color: var(--ink-400); font-weight: 600; }
.drawer-footer { display: flex; justify-content: flex-end; gap: 10px; }
</style>
