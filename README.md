# 月子中心数字化多端运营平台（多租户 SaaS）

一个后端 + 八个前端：平台运营端、租户后台、PC 品牌官网、微信公众号、小程序客户版、小程序护理师版、H5 分享页、Flutter App。

技术栈：Java 17 · Spring Boot 3.2 · MyBatis-Plus（租户行级插件）· MySQL 8 · Redis · RabbitMQ · Vue 3 + Vite + TypeScript + Element Plus · 微信原生小程序双工程 · Flutter 3 · Docker Compose。

## 一、能力总览

| 端 | 目录 | 说明 |
|---|---|---|
| 平台运营端 | `admin-web/`（`/platform/*`） | 租户开通/停用/续期、套餐与五类配额、按席位账单、平台账号与审计 |
| 租户后台 | `admin-web/` | 房态日历、CRM 线索、订单与收款、在住与护理、康复、月子餐、员工排班、会员营销、内容与 SEO、公众号、报表、系统设置 |
| PC 官网 | `web-site/` | 首页、房型套餐、康复、月子餐、团队、案例、门店、资讯、留资预约；SEO / hreflang / JSON-LD |
| 公众号 | 后端 `yzz-mp` + `views/mp` | 台账、菜单、自动回复、粉丝标签、素材图文、统计（未配凭证时本地降级并明示） |
| 客户小程序 | `miniprogram/` | 首页、房型、预约参观、下单支付、结果页、订单、护理、康复与月子餐、会员中心 |
| 护理师小程序 | `miniprogram-staff/` | 登录、今日工作台、护理执行、交接班、异常上报、出餐核对、考勤、消息 |
| H5 | `h5/` | 落地页、活动、案例、预约、支付回跳、订单查询、会员登录 |
| App | `app/` | Flutter 双端：登录、首页、订单（离线缓存）、护理日志、康复与月子餐、会员、消息 |

## 二、工程结构

```
yuezizhongxin/
├── pom.xml                     聚合工程（18 模块，dependencyManagement 锁定版本）
├── yzz-common/                 返回结构、错误码、租户上下文、幂等/限流、审计、存储、脱敏、敏感词
├── yzz-tenant/                 租户域、五态生命周期、初始化模板
├── yzz-platform/               平台运营端（租户/套餐/账单/审计/菜单）
├── yzz-system/                 租户账号、登录、RBAC、字典、参数、多语言、导航、入口、SSO、消息配置、审计
├── yzz-room|product|crm|order|stay|care|rehab|meal|staff|member|content|report|mp
│                               各业务域（Service + Controller + Entity + Mapper）
├── yzz-api/                    启动模块 + 开放接口聚合（站点/客户自助/员工端）+ 文件上传预览
├── admin-web/                  租户后台 + 平台端（Vue3）
├── web-site/                   PC 官网（Vue3 + SEO）
├── h5/                         H5 分享页（Vue3 + hash 路由，base=/h5/）
├── miniprogram/                客户小程序（原生）
├── miniprogram-staff/          护理师小程序（原生，与客户版物理隔离）
├── app/                        Flutter 客户 App
├── sql/                        00 建库、01 平台、02 系统、03 业务、04 运营、05 公众号配置、06 种子、07 扩展
├── deploy/                     compose（dev/prod）、Dockerfile、nginx、备份、监控、冒烟与验收脚本
└── docs/                       requirements / proposal / manual / architecture / adr
```

## 三、快速启动

### 3.1 依赖环境

JDK 17、Maven 3.9+、Node 18+、Docker、微信开发者工具、Flutter 3（可选）。

### 3.2 启动依赖

```bash
docker compose -f deploy/docker-compose.dev.yml up -d mysql redis rabbitmq minio
# 首次启动自动执行 sql/01~07 建表与种子数据；如已存在数据卷不会重复执行
```

一键脚本（MySQL 自检 → 后端 18090 → 前端 5173 → 登录自检）：

```bash
python scripts/dev-start.py
python scripts/dev-stop.py          # 加 --all 同时停 MySQL 容器
```

> 本机 18080 常被其他项目占用，脚本默认后端端口 18090；可用 `YZZ_PORT=18091 python scripts/dev-start.py` 覆盖。
> `application.yml` 默认端口 8080、数据库 3306，容器外直连时按需覆盖。

### 3.3 启动后端

```bash
mvn -DskipTests package
java -jar yzz-api/target/yzz-api.jar --server.port=18090
# 接口 /api ，文档 /swagger-ui.html ，健康检查 /actuator/health ，指标 /actuator/prometheus
```

### 3.4 启动前端

```bash
cd admin-web && npm i && npm run dev      # 后台 5173（/api 代理到 18090，可用 VITE_API_TARGET 覆盖）
cd web-site  && npm i && npm run dev      # 官网 5180
cd h5        && npm i && npm run dev      # H5 5190（构建产物带 /h5/ base）
```

### 3.5 小程序

用微信开发者工具分别打开 `miniprogram/`（客户版）与 `miniprogram-staff/`（护理师版）。
联调时把 `app.js` 中 `baseUrl` 改为后端地址，并在小程序后台配置 request 合法域名。

### 3.6 App

```bash
cd app && flutter pub get
flutter run --dart-define=API_BASE=http://10.0.2.2:8080 --dart-define=TENANT=demo
```

## 四、演示账号

| 入口 | 账号 | 口令 | 说明 |
|---|---|---|---|
| 平台运营端 | `platform` / `oper` / `finance` | `Admin@123456` | 平台超管 / 运营 / 财务 |
| 租户后台 | `admin`（租户 `demo`） | `Admin@123456` | 请求头带 `X-Tenant-Code: demo` |
| 客户小程序 / H5 / App | 任意手机号 + 验证码 | 未接短信时验证码回显（`yzz.sms.mock=true`） | 手机号首次登录自动建会员 |
| 护理师小程序 | 门店员工账号（需在员工档案绑定） | 同上 | 账号未绑定档案会提示联系主管 |

两端令牌分开存放（`yzz.platform.token` / `yzz.token`），平台令牌访问租户接口、租户令牌访问平台接口均返回 403。

## 五、关键接口（节选）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/platform/auth/login` | 平台登录 |
| GET/POST | `/api/platform/tenants` | 租户列表 / 开通（返回一次性口令） |
| POST | `/api/platform/tenants/{id}/disable\|resume\|renew` | 停用 / 恢复 / 续期 |
| GET/PUT | `/api/platform/tenants/{id}/quota` | 配额调整（必填原因，留痕） |
| GET/POST | `/api/platform/bills`、`/generate`、`/{id}/pay` | 账单生成与状态流转 |
| POST | `/api/tenant/auth/login` | 租户登录 |
| GET/POST | `/api/tenant/orders`、`/{id}/payments` | 订单与收款 |
| GET/POST | `/api/tenant/care/**` | 护理计划、执行、交接、异常、看板 |
| GET/POST | `/api/tenant/rehab/**`、`/api/tenant/meals/**` | 康复与月子餐 |
| GET/POST | `/api/tenant/staff/**` | 员工档案、证件、排班、考勤、工作量 |
| GET/POST | `/api/tenant/members/**` | 会员、积分、券、储值、地址、活动、转介绍 |
| GET/POST | `/api/tenant/cms/**`、`/api/tenant/mp/**` | 内容与公众号 |
| GET | `/api/tenant/report/**`、`/workbench` | 报表与工作台 |
| GET | `/api/open/site/**` | 官网/小程序/H5 站点数据（免登录，需租户头） |
| POST | `/api/open/orders`、`/{id}/pay` | 客户端下单与支付 |
| GET | `/api/open/orders/{orderNo}/query` | 订单号 + 手机号查询 |
| POST | `/api/open/member/sms`、`/login` | 会员验证码登录 |
| GET/POST | `/api/open/customer/**` | 客户自助：订单、在住、护理、餐食、康复、转介绍 |
| GET/POST | `/api/tenant/mobile/**` | 护理师端：工作台、任务执行、交接、异常、出餐、考勤 |
| POST | `/api/files/upload`、GET `/files/**` | 文件上传与预览（强制租户前缀） |
| GET | `/actuator/health`、`/actuator/prometheus` | 健康检查与监控指标 |

## 六、验证方式

```bash
mvn -DskipTests compile          # 后端编译
mvn test                         # 单元测试
cd admin-web && npm run build    # 后台构建
cd web-site  && npm run build    # 官网构建
cd h5        && npm run build    # H5 构建
cd app       && flutter analyze  # App 静态检查

bash deploy/smoke-test.sh        # 平台与租户底座（11 项）
bash deploy/smoke-phase4.sh      # 系统配置域（41 项）
bash deploy/smoke-mobile.sh      # 会员/订单/护理/餐食/康复/员工端
bash deploy/test-isolation.sh    # 双租户隔离
bash deploy/test-security.sh     # 越权/幂等/限流/脱敏/审计
bash deploy/test-i18n.sh         # 多语言与 SEO
```

## 七、生产部署

```bash
cp deploy/.env.example deploy/.env   # 填写域名、密码、密钥
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env up -d --build
```

包含 nginx（四域名 + HTTPS + SPA 兜底）、backend、MySQL、Redis、RabbitMQ、MinIO、每日备份、Prometheus + Grafana。
证书放 `deploy/nginx/certs/`；数据库与对象存储不对外暴露端口。

## 八、已知偏差

1. **后台 UI 基座**：任务书建议 vue-pure-admin，实际按设计 token 自建 layout（`admin-web/src/layout`），未引入其多页签与主题体系；功能与权限链路完整，样式与交互统一。
2. **存储驱动**：5 个驱动共用同一 `StorageService` 与 SigV4 签名，本地驱动已用真实文件验证；对象存储只做签名结构与路径规则单测，**上线前须在目标环境执行一次「连接测试」**。
3. **缓存默认内存实现**：`yzz.cache.provider=memory` 便于单机调试；多实例部署必须切 `redis`，否则限流与幂等只在单机生效。
4. **停用会话清理**：依靠「每次请求校验租户五态（缓存 60 秒，停用即主动失效）」，未引入令牌黑名单；需要秒级强制下线时扩展令牌版本号。
5. **短信网关**：`local_log` 渠道可真实测试，其余仅落配置与掩码，待凭证到位接入。
6. **App 推送**：当前为应用内消息列表 + 护理动态，未接入 APNs/FCM 厂商通道。
7. **前端未做浏览器级 E2E**：后台/官网/H5 均通过构建，页面与接口契约由接口冒烟覆盖。
8. **字典归属**：`sys_dict_*` 为平台统一下发（无 `tenant_id`），已加入租户插件白名单，避免被注入 `tenant_id = -1`。

## 九、文档

- `docs/requirements.md` 需求规格说明书（角色矩阵、功能编号、验收标准）
- `docs/proposal.md` 技术方案与报价（WBS 人天、报价明细、付款节点、风险对策）
- `docs/manual.md` 用户使用手册（按角色编号步骤、常见问题）
- `docs/architecture.md` 架构与核心数据流
- `docs/adr.md` 架构决策记录
- `STATUS.md` 交付进度与逐阶段说明
