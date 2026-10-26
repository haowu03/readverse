# ReadVerse · 阅界

> 一个从 0 到 1 构建的全栈阅读社区：**Spring Boot 3 + Vue 3 + MySQL + Redis**，重点展示缓存、限流、分布式锁、异步统计、示例书源同步与积分一致性。

![管理后台](docs/screenshots/admin-dashboard.png)

**在线演示（本地）**

- 管理后台：<http://localhost:5173>（`admin / Admin@123`，`operator / Operator@123`）
- 读者门户：<http://localhost:5174>（`reader / Reader@123`）
- 后端 Swagger：<http://localhost:8080/swagger-ui.html>
- 健康检查：<http://localhost:8080/actuator/health>

---

## 1. 已实现功能

| 模块 | 能力 | 状态 |
|---|---|---|
| 认证与 RBAC | 注册、登录、Refresh Token、登出白名单、角色/权限、动态菜单、按钮级权限 | ✅ |
| 内容域 | 作者/分类/书籍/章节 CRUD、上下架、封面上传、分页搜索 | ✅ |
| 阅读体验 | 首页聚合、书详情、目录、阅读器、阅读进度、书架 | ✅ |
| 互动 | 评分（一人一条可改）、评论、点赞、删除自评 | ✅ |
| 排行榜 | Redis Zset 点击量，7 日 / 30 日 / 总榜 | ✅ |
| 缓存 | Caffeine L1 + Redis L2、空值缓存、Cache-Aside、Banner 缓存 | ✅ |
| 防穿透 | Redis Bitmap 布隆过滤器，启动预热 + 每日重建 + 失效兜底 | ✅ |
| 限流 | `@RateLimit` + Redis Lua 令牌桶，章节/书详情接入，返回 429 | ✅ |
| 分布式锁 | Redisson 锁，评分聚合/榜单规则使用 | ✅ |
| 异步事件 | 统一事件体 + 本地事件总线；`rv.mq.enabled=true` 切换 RabbitMQ 监听器 | ✅（本地默认） |
| 示例书源 | Mock 书源、全量/增量/章节同步、幂等 upsert、失败重试/重放、作业日志 | ✅ |
| 积分 | 签到、积分账户/流水、商品兑换、权益、幂等键、对账 | ✅ |
| 运营看板 | 书籍/章节/用户/今日阅读、采集成功率、趋势图 | ✅ |
| 测试 | JWT 单测 3 个 + BCrypt 单测 1 个；Maven 构建通过 | ✅（基础覆盖） |

详细计划与取舍见 **[docs/ReadVerse-项目实施计划.md](docs/ReadVerse-项目实施计划.md)**，验证记录见 **[docs/verification.md](docs/verification.md)**。

---

## 2. 技术栈

| 层 | 选型 |
|---|---|
| 后端 | Spring Boot 3.5.16、JDK 17、Spring Security 6、MyBatis-Plus 3.5.18 |
| 认证 | JJWT 0.12.6，Access 30min + Refresh 7d，Redis Token 白名单 |
| 缓存 | Caffeine 3.2 + Redis 7（本地演示使用 Redis 5.0.14 Windows 版） |
| 锁 / 布隆 | Redisson 3.52 + Redis Bitmap 自实现布隆 |
| 数据库 | MySQL 8.0，`schema.sql` 自动建表 + Java 初始化种子数据 |
| 消息 | Spring AMQP / RabbitMQ（可选），默认本地事件总线 |
| 调度 | Spring Scheduling 兜底；XXL-Job 接入点预留在 `rv-crawl/job` |
| 前端 | Vue 3.5、Vite 5.4、TypeScript、Element Plus 2.9、Pinia 2.3、Vue Router 4.5、Axios 1.7、ECharts 5.5 |
| 文档 | SpringDoc OpenAPI 2.8（Swagger UI）、Markdown 文档 |
| 质量 | JUnit 5、AssertJ、Playwright（系统 Chrome 截图验收） |

---

## 3. 项目结构

```text
readverse/
├── rv-common        # R<T> / PageResult / ErrorCode / BizException
├── rv-framework     # JWT / RBAC / 缓存 / 布隆 / 限流 / 锁 / MyBatis / 异常 / MQ
├── rv-system        # 用户、角色、权限、通知、审核通知监听
├── rv-content       # 作者/分类/书籍/章节/阅读/评分/评论/排行/积分/统计
├── rv-crawl         # 示例书源、任务状态机、同步、重试、作业日志、调度
├── rv-server        # 启动模块、schema.sql、种子数据、上传、OpenAPI
├── web-admin        # 管理后台（Vue3）
├── web-portal       # 读者门户（Vue3）
├── deploy           # docker-compose + MySQL 初始化脚本
├── docs             # 计划、验证记录、截图
└── tools/e2e        # Playwright 验收脚本（本地工具，不参与构建）
```

---

## 4. 本地启动

### 4.0 一键启动（Windows，推荐）

```powershell
cd C:\Users\16098\Desktop\ReadVerse
powershell -ExecutionPolicy Bypass -File .\scripts\start.ps1
```

脚本会依次检查 MySQL/Redis、启动后端（必要时先构建）、启动两个 Vite 前端，最后打印访问地址。  
停止：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\stop.ps1
# 同时停 Redis：-IncludeRedis
```

启动日志在 `logs\backend.log`、`logs\admin-dev.log`、`logs\portal-dev.log`。如果某个端口已被占用，脚本会跳过对应服务并提示。

### 4.1 前置条件

- JDK 17（不要用 JDK 8；本项目以 17 编译）
- Maven 3.9+
- MySQL 8（默认 `localhost:3306`，`root/123456`，见 `application.yml`）
- Redis（默认 `localhost:6379`，密码 `readverse`）
- Node 22 + npm 10

> 本项目默认不需要 RabbitMQ：`rv.mq.enabled=false` 时使用本地 `ApplicationEventPublisher`，审核通知与阅读统计照常工作。开启 RabbitMQ 只需启动 `deploy/docker-compose.yml` 中的 rabbitmq，并把 `rv.mq.enabled` 改为 `true`。

### 4.2 建库

```sql
CREATE DATABASE IF NOT EXISTS readverse
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
```

应用首次启动会执行 `rv-server/src/main/resources/schema.sql` 建表，并自动写入 30 本书、150 章、3 个角色、57 个权限点、3 个演示账号。

### 4.3 启动 Redis

已有 Redis 时：

```bash
redis-server --port 6379 --requirepass readverse
```

或使用 Docker：

```bash
cd deploy
docker compose up -d mysql redis rabbitmq
```

> 如果暂时没有 Redis：启动时加 `--rv.redis.enabled=false`，Token、缓存、布隆与限流会自动降级到本地实现，便于快速看页面；正式演示仍建议使用 Redis。
>
> 如果启用 RabbitMQ：把 `rv.mq.enabled` 改为 `true`，并把 `management.health.rabbit.enabled` 改为 `true`（默认关闭 Rabbit 健康检查，避免无 broker 时健康检查 503）。

### 4.4 启动后端

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

mvn -B -DskipTests package
java -jar rv-server/target/rv-server-1.0.0.jar
```

验证：

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

### 4.5 启动前端

```bash
cd web-admin
npm install
npm run dev        # http://localhost:5173

cd ../web-portal
npm install
npm run dev        # http://localhost:5174
```

两个 Vite 服务通过 `/api` 代理到 `http://localhost:8080`。

### 4.6 演示账号

| 账号 | 密码 | 角色 | 用途 |
|---|---|---|---|
| `admin` | `Admin@123` | 管理员 | 全部菜单、权限、系统管理 |
| `operator` | `Operator@123` | 运营 | 内容、采集、积分，无系统管理 |
| `reader` | `Reader@123` | 读者 | 门户阅读、书架、评分、签到、兑换 |

---

## 5. 关键设计

### 5.1 JWT + RBAC

- Access Token 30 分钟，Refresh Token 7 天；JJWT 0.12 API。
- JWT 只放 `userId`、`username`、`jti`；登录用户信息（角色 + 权限）存 Redis `rv:auth:token:{jti}`，登出即失效。
- `@PreAuthorize("hasAuthority('content:book:edit')")` 做按钮/接口级鉴权；前端 `v-permission` 风格由 `hasPerm()` 控制按钮显隐。
- 动态菜单：`/api/admin/menus` 按当前用户权限过滤 `MENU` 节点，前端侧边栏由后端菜单驱动。

### 5.2 二级缓存 + 布隆 + 限流

- 章节详情：Caffeine L1（60s）→ Redis L2（30min ± 随机）→ MySQL；更新后删 L2、删 L1，并延迟双删。
- 布隆：Redis Bitmap + MD5 四路哈希，启动预热 150 个章节；Redis 被清空时 fail-open，避免误杀。
- `@RateLimit`：Redis Lua 令牌桶，按 IP / USER / GLOBAL 维度；章节 120 req/s、书详情 200 req/s；超限返回 42901。
- 实测 400 个并发章节请求：200 × 144、429 × 256；冷读 `cached=false`，二次读 `cached=true`（见 `docs/verification.md`）。

### 5.3 异步统计与审核通知

- 统一事件体 `DomainEvent(eventId, type, bizId, occurredAt, payload)`。
- 本地模式：`ApplicationEventPublisher`；RabbitMQ 模式：`RabbitTemplate` + `@RabbitListener`（同一套 Handler）。
- 已接入两条链路：
  1. 书籍审核结果 → 通知管理员（`BOOK_AUDIT_PASSED/REJECTED → sys_notify`）。
  2. 章节阅读行为 → 日统计（`CHAPTER_READ → stat_daily_book/stat_daily_platform`）。
- 配置 `rv.mq.enabled` 一键切换；默认本地模式无需中间件即可演示。

### 5.4 示例书源同步

- 内置 `MockSourceAdapter`：10 本示例书、50 章，**不访问真实站点**。
- 唯一键幂等：`crawl_book_map(source_id, external_book_id)`、`content_chapter(book_id, chapter_no)`。
- 任务状态：`PENDING → RUNNING → SUCCESS/FAILED`；管理端可重放。
- 全量任务重复执行 3 次不新增书籍/章节；每次执行写 `sys_job_log`。

### 5.5 积分一致性

- `reader_points_account` + `reader_points_log`；流水是事实来源。
- 变更走“条件更新 + version 乐观锁 + 重试”，扣减时校验 `balance >= amount`。
- 签到：`reader_sign_in(user_id, sign_date)` 唯一键防重，Redis Bitmap 仅作展示加速。
- 兑换：库存原子扣减、积分扣减、订单、权益同一事务；支持 `Idempotency-Key`，重复请求返回同一订单。
- 对账接口：`POST /api/admin/points/reconcile`，当前演示数据差异为 0。

---

## 6. 主要接口

### 门户

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/portal/home` | 首页聚合 |
| GET | `/api/portal/books` | 书库分页/分类/排序 |
| GET | `/api/portal/books/{id}` | 书详情（限流） |
| GET | `/api/portal/books/{id}/chapters` | 目录 |
| GET | `/api/portal/books/{id}/cover` | 动态 SVG 封面 |
| GET | `/api/portal/chapters/{id}` | 章节正文（限流、缓存、VIP 校验） |
| GET | `/api/portal/rankings` | 7d/30d/all |
| GET/POST/DELETE | `/api/portal/bookshelf` | 书架 |
| GET/PUT | `/api/portal/read-record` | 阅读进度 |
| POST | `/api/portal/ratings/{bookId}` | 评分 |
| GET/POST | `/api/portal/comments` | 评论 |
| POST | `/api/portal/sign-in` | 签到 |
| GET | `/api/portal/sign-in/month` | 月签到 |
| GET | `/api/portal/points/*` | 账户/流水/商品/订单 |
| POST | `/api/portal/points/exchange` | 兑换 |

### 管理后台

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/auth/login` | 登录 |
| GET | `/api/admin/menus` | 动态菜单 |
| GET | `/api/admin/dashboard/*` | 看板 |
| GET/POST/PUT/DELETE | `/api/admin/books` | 书籍 |
| PUT | `/api/admin/books/{id}/publish` | 上下架 |
| GET/POST/PUT/DELETE | `/api/admin/chapters` | 章节 |
| GET/POST/PUT/DELETE | `/api/admin/authors` | 作者 |
| GET/POST/PUT/DELETE | `/api/admin/categories` | 分类 |
| GET/POST | `/api/admin/crawl/sources` | 书源 |
| POST | `/api/admin/crawl/tasks/trigger` | 触发同步 |
| POST | `/api/admin/crawl/tasks/{id}/retry` | 重放 |
| GET/POST/PUT/DELETE | `/api/admin/points/goods` | 积分商品 |
| GET | `/api/admin/points/orders` | 兑换订单 |
| POST | `/api/admin/points/reconcile` | 对账 |

完整 OpenAPI 见 `http://localhost:8080/swagger-ui.html`。

---

## 7. 验证

```bash
# 后端单测 + 构建
mvn -B test
mvn -B -DskipTests package

# 前端生产构建
cd web-admin && npm run build
cd ../web-portal && npm run build

# 浏览器验收（需要本机 Chrome）
node tools/e2e/verify.mjs
```

验证结果、接口实测数字与截图见 **[docs/verification.md](docs/verification.md)**。

| 截图 | 说明 |
|---|---|
| ![管理后台](docs/screenshots/admin-dashboard.png) | 数据看板 |
| ![书籍管理](docs/screenshots/admin-books.png) | 书籍 CRUD 与封面 |
| ![门户首页](docs/screenshots/portal-home.png) | 阅读社区首页 |
| ![书详情](docs/screenshots/portal-book-detail.png) | 书籍详情与目录 |
| ![阅读器](docs/screenshots/portal-reader.png) | 章节阅读器 |
| ![个人中心](docs/screenshots/portal-profile.png) | 签到、积分、兑换 |

---

## 8. 已知限制与下一步

1. **RabbitMQ 默认未启动**：当前以本地事件总线运行；`rv.mq.enabled=true` + RabbitMQ 后自动切换监听器。本地未安装 Erlang/RabbitMQ，因此“停消费者/积压/死信”尚未做真实 broker 演示。
2. **XXL-Job 未实际接入**：`CrawlScheduler` 提供 Spring Scheduling 兜底（`rv.scheduler.enabled=true`），XXL-Job Admin 在 compose 中保留注释；下一步把 `@XxlJob` Handler 接到同一 Service。
3. **测试覆盖**：目前 4 个单元测试（JWT/BCrypt），未达到计划中的 60%；下一步补 Auth、缓存、限流、积分并发、Testcontainers 集成测试。
4. **搜索**：当前使用 `LIKE` 兜底；生产环境建议补 MySQL ngram 全文索引或 ES。
5. **演示数据**：书籍评分人数为种子展示值，并非逐条评分聚合；评分逻辑本身已实现并在门户可用。
6. **部署**：未做公网部署；Vite 开发代理即可本地演示，生产需 Nginx 反代 `/api` 与静态资源。

---

## 9. 文档

- [项目实施计划（解析 + 8 周路线）](docs/ReadVerse-项目实施计划.md)
- [验收记录](docs/verification.md)
- [截图目录](docs/screenshots)

---

> 所有书籍/章节内容均为内置示例文本，不抓取真实站点；项目仅用于技术学习与作品展示。
