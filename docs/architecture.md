# 架构说明与核心数据流（T17）

> 对应 PRD 第二章。本文只保留落地所需的图与链路说明，技术选型对比与风险清单以 PRD 2.5/2.6 为准。

## 1. 一后端多前端壳

```mermaid
graph TD
    subgraph 客户端
      PADMIN["平台运营端<br/>/platform"]
      ADMIN["PC 租户后台<br/>/tenant"]
      PCWEB["PC 品牌官网"]
      UMP["小程序·用户端"]
      NMP["小程序·护理师端"]
      H5["H5 分享页"]
      OA["微信公众号（服务号）"]
      APP["App（Flutter）"]
    end

    PADMIN --> NG["Nginx + HTTPS + 租户子域名解析"]
    ADMIN --> NG
    PCWEB --> NG
    UMP --> NG
    NMP --> NG
    H5 --> NG
    OA -->|"消息与事件回调"| NG
    APP --> NG

    NG --> API["yzz-api（Spring Boot 3 模块化单体）"]
    API --> AUTH["认证鉴权 · 租户上下文 · 审计"]
    API --> BIZ["业务模块：房态/套餐/CRM/订单/入馆出馆/护理/康复/餐食/人力/会员/内容/报表/公众号"]
    AUTH --> DB[("MySQL 8")]
    BIZ --> DB
    BIZ --> REDIS[("Redis 7")]
    BIZ --> MQ["RabbitMQ（延迟队列）"]
    BIZ --> OSS["对象存储（本地/S3/OSS/COS/MinIO）"]
    API --> WX["微信开放平台/公众号/小程序"]
    API --> SMS["短信 6 渠道"]
    API --> PAY["微信支付 v3"]
```

## 2. 多租户两条请求链路

```mermaid
graph TD
    subgraph 平台侧
      PA["平台运营端"] --> PAPI["/api/platform/**<br/>platform_* 表"]
      PAPI --> PDB[("平台表")]
    end

    subgraph 租户侧
      T1["租户甲 全端"] --> NG["Nginx + 租户解析"]
      T2["租户乙 全端"] --> NG
      NG --> F["TenantContextFilter<br/>解析 X-Tenant-Code 或子域名"]
      F --> S{"租户五态校验"}
      S -->|"停用/到期"| D403["403 拒绝（含登录）"]
      S -->|"正常/试用/欠费"| AI["AuthInterceptor<br/>令牌解析 + 入口隔离 + 租户一致性"]
      AI --> TC["TenantContext（ThreadLocal）"]
      TC --> MP["MyBatis 租户插件<br/>强制追加 tenant_id"]
      MP --> BDB[("业务表（共享库 + tenant_id）")]
      TC --> RC[("Redis key: tenant:{code}:*")]
      TC --> OB["对象存储路径: tenants/{code}/"]
      TC --> MQ["队列消息体带 tenant_id"]
    end
```

**落地要点（与代码一一对应）**

| 环节 | 实现位置 |
|---|---|
| 租户解析（请求头 → 子域名） | `common/web/TenantContextFilter` |
| 五态拦截（停用/到期 403，欠费限写） | `TenantContextFilter` + `TenantLifecycleInterceptor` |
| 令牌签发（写入 tenant_id） | `system/service/TenantAuthService`、`platform/service/PlatformAuthService` |
| 入口物理隔离（平台端 ↔ 租户端） | `common/web/AuthInterceptor`（40305） |
| 篡改租户标识 403 + 安全审计 | `AuthInterceptor` + `JdbcAuditRecorder#recordSecurity`（40300） |
| DAO 强制租户条件 | `common/web/MybatisPlusConfig`（TenantLineInnerInterceptor） |
| 平台表跳过租户条件 | 表前缀 `platform_` 自动忽略 |

## 3. 核心数据流

### 3.1 正常链路：预约 → 定金 → 入住 → 结算

```mermaid
sequenceDiagram
    participant C as 客户（小程序/官网）
    participant A as 后端
    participant S as 销售/顾问
    participant W as 微信支付
    participant R as 护理师端

    C->>A: 提交参观预约（来源带参码）
    A->>A: 写入 crm_lead（来源归因）
    S->>A: 跟进并一键转订单
    A->>A: 创建 biz_order（待支付）
    C->>W: 支付定金
    W->>A: 回调（验签 + 幂等键）
    A->>A: biz_payment 已支付 + room_schedule 锁定（唯一约束兜底）
    C->>A: 提交健康告知与证件（加密存储）
    A->>A: stay_checkin 登记，房态转「已入住」
    R->>A: 按护理计划逐项打卡（服务端时间戳）
    A->>A: 出馆结算：尾款 + 押金退还 + 42 天复查预约
    A->>A: 房态「待清洁」→「可预订」
```

### 3.2 异常链路一：支付回调失败或丢失

```mermaid
graph LR
    A["支付回调验签失败/超时"] --> B["订单保持待支付"]
    B --> C["进入主动查单队列<br/>每 5 分钟一次，最多 6 次"]
    C -->|"查单成功"| D["补记支付流水并锁档"]
    C -->|"6 次仍失败"| E["标记支付异常 + 财务待办"]
    E --> F["人工核对银行流水"]
    F --> G["手动确认或关闭订单并释放档期"]
```

### 3.3 异常链路二：护理体征超阈值

```mermaid
graph LR
    A["护理师端录入体征"] --> B{"是否超预警阈值"}
    B -->|"否"| C["正常归档，主管端可见"]
    B -->|"是"| D["生成异常单（类型/紧急程度/照片）"]
    D --> E["即时推送护理主管"]
    E --> F["处置记录 + 告知家属"]
    F --> G["提示「建议及时就医」<br/>系统不输出医疗结论"]
    G --> H["闭环并计入护理报表"]
```

## 4. 模块依赖方向

```
yzz-api（启动 + 对外 REST）
  ├── yzz-platform（平台运营端：租户/套餐/账单/审计）
  │      └── yzz-tenant（租户域：平台表实体、状态机、初始化模板）
  ├── yzz-system（租户账号、登录、审计落库）
  │      └── yzz-tenant
  ├── yzz-common（统一返回/异常/租户上下文/幂等限流/审计/存储）
  └── yzz-room / product / crm / order / stay / care / rehab / meal / staff / member / content / report / mp
         └── yzz-common
```

禁止反向依赖：`common` 不依赖任何业务模块；`tenant` 只依赖 `common`；业务模块之间通过服务接口或领域事件交互，不直接引用对方 Mapper。
