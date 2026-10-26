# ReadVerse 验收记录

> 验收日期：2026-10-26  
> 环境：Windows · JDK 17.0.12 · MySQL 8.0.46 · Redis 5.0.14（本地回退模式）· Node 22.21.1 · Docker Desktop 29.9.2  
> 说明：**主验收方式为 Docker Desktop**（MySQL 8 / Redis 7 / 后端 / 两个 Nginx 前端）。本地直跑模式作为备用，接口结果与 Docker 模式一致。

## 0. Docker Desktop 部署验收

| 检查 | 结果 |
|---|---|
| `docker compose -f deploy/docker-compose.yml ps` | 5 个容器全部 **healthy** |
| 容器 | `rv-mysql` / `rv-redis-docker` / `rv-backend` / `rv-web-admin` / `rv-web-portal` |
| 端口映射 | MySQL `13306→3306`、Redis `6380→6379`、Backend `18080→8080`、Admin `8081→80`、Portal `8082→80` |
| 后端健康检查 | `http://localhost:18080/actuator/health` → **UP** |
| Nginx 健康检查 | `http://localhost:8081/health` → `ok`；`http://localhost:8082/health` → `ok` |
| 首次启动种子 | `Demo data seeded: roles=3, permissions=57, users=3, books=30`，布隆预热 150 章 |
| 管理端登录（经 Nginx） | `POST http://localhost:8081/api/auth/login` → `code=0`，57 个权限点 |
| 门户首页（经 Nginx） | `GET http://localhost:8082/api/portal/home` → 6 分类 / 8 新书 |
| Docker E2E | 以 `8081 / 8082 / 18080` 跑 Playwright：11 个管理端路由 + 5 个门户页面，**0 console error / 0 pageerror** |
| 镜像 | `readverse/backend:1.0.0`（518MB）、`readverse/web-admin:1.0.0`（77.3MB）、`readverse/web-portal:1.0.0`（75.8MB） |

> 以下第 1–9 节记录接口与业务链路的实测结果（本地回退模式执行，Docker 模式复用同一套代码和数据库结构）。

## 1. 构建与测试

| 检查 | 命令 | 结果 |
|---|---|---|
| 后端单测 | `mvn -B test` | 4 个测试通过（JWT 3 + BCrypt 1） |
| 后端打包 | `mvn -B -DskipTests package` | 6 个模块全部 SUCCESS，生成 `rv-server-1.0.0.jar` |
| 管理端构建 | `cd web-admin && npm run build` | 2253 modules transformed，dist 生成 |
| 门户构建 | `cd web-portal && npm run build` | 1687 modules transformed，dist 生成 |
| 健康检查 | `GET /actuator/health` | `{"status":"UP"}`（db/rvRedis/diskSpace UP） |

测试报告：

```text
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 -- JwtServiceTest
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 -- PasswordEncodingTest
```

## 2. 认证与 RBAC

| 检查 | 结果 |
|---|---|
| `admin / Admin@123` 登录 | 成功，角色 `ADMIN`，权限点 **57** 个 |
| `operator / Operator@123` 登录 | 成功，角色 `OPERATOR`，权限点 **45** 个 |
| `reader / Reader@123` 登录 | 成功，角色 `READER`，无后台权限 |
| 管理员动态菜单 | 5 个根菜单（看板/内容/采集/积分/系统） |
| 运营动态菜单 | 4 个根菜单（无系统管理） |
| 运营访问 `/api/admin/users` | HTTP **403** |
| 读者访问 `/api/admin/books` | HTTP **403** |
| 登出后旧 token | 调用 `/api/auth/logout` 后访问 `/api/auth/me` 返回 **401** |
| 无效 token | 返回 **401** |
| OpenAPI | `GET /v3/api-docs` 返回 **200**，约 60KB JSON |

## 3. 内容与阅读

| 检查 | 结果 |
|---|---|
| 书库总数 | **30** 本 |
| 章节总数 | **150** 章 |
| 分类 | 6 个（玄幻/都市/历史/科幻/悬疑/文学） |
| 首页聚合 | 6 分类 · 8 新书 · 8 热门 · 排行数据 |
| 书详情 | 标题/作者/分类/评分/字数/目录完整 |
| 章节阅读 | 正文、上一章/下一章、字数正常 |
| 书架 | 读者预置 **6** 本，含读至章节 |
| 评分 | 写入/更新成功，均分与人数更新 |
| 评论 | 列表/发表/删除/点赞可用 |
| 封面 | 动态 SVG 封面 `/api/portal/books/{id}/cover`，无外链图片依赖 |

## 4. 缓存与限流

| 检查 | 命令/方式 | 结果 |
|---|---|---|
| 章节冷读 | 删除 `rv:chapter:150` 后请求 | `cached=false` |
| 章节二次读 | 再次请求同一章节 | `cached=true` |
| 缓存命中 | Redis `rv:chapter:150` 存在 | 命中 |
| 布隆预热 | 启动日志 | `Bloom filter warmed with 150 chapter ids` |
| 限流 | 400 个并发请求 `/api/portal/chapters/2` | **200 × 144，429 × 256** |
| 限流维度 | 同 IP 下章节 key `chapter:2` | 超过 120 req/s 后拒绝 |
| 限流修复 | 令牌桶时间单位 | 初版误用毫秒导致不触发，改为秒后实测 429 生效 |

## 4.1 管理端 CRUD 实测

| 检查 | 结果 |
|---|---|
| 新增书籍 | `code=0`，生成书籍 ID |
| 新增章节 | `code=0`，生成章节 ID |
| 上架 | `code=0`，门户详情可见 |
| 删除书籍 | `code=0`，门户再次访问返回 **404** |
| 级联清理 | 删除书籍后章节级联清理，数据回到 30 本/150 章 |
| 封面实际上传 | `POST /api/admin/upload/cover` 返回 `/uploads/...png`，文件落盘 |

## 5. 采集与幂等

| 检查 | 结果 |
|---|---|
| 触发全量同步 | `SUCCESS success=10, fail=0` |
| 连续触发 3 次 | 3 次均 `SUCCESS`，书籍/章节仍为 **30 / 150**，无重复 |
| 同步后书籍/章节 | 仍为 **30 / 150**，无重复 |
| 任务表 | 有任务记录，状态 `SUCCESS` |
| 作业日志 | `sys_job_log` 记录成功数与耗时 |
| 失败重放接口 | `POST /api/admin/crawl/tasks/{id}/retry` 可用 |

## 6. 积分与交易一致性

| 检查 | 结果 |
|---|---|
| 签到 | 首次 +5，重复签到返回“今天已经签到过了” |
| 积分账户 | 初始 120（`INIT` 流水） |
| 兑换 | 扣积分、生成订单与券码 |
| 幂等键 | 同一 `idempotencyKey` 两次请求返回**同一订单号**，余额只扣一次（110/110） |
| 对账 | `POST /api/admin/points/reconcile` 差异 **0** |
| 并发保护 | 账户更新使用 `version` 乐观锁 + `balance >= amount` 条件更新 |

## 7. 异步事件

| 链路 | 验证 |
|---|---|
| 书籍审核通过 → 管理员通知 | 审核后 `unread-count` 从 0 变为 1，通知标题“书籍审核结果” |
| 章节阅读 → 日统计 | `stat_daily_book` 新增当日记录，`read_count=1`；`stat_daily_platform.read_count=1` |
| 事件总线 | `rv.mq.enabled=false` 时 `ApplicationEventPublisher`；置 true 后使用 RabbitMQ 监听器 |

## 8. 前端 E2E

使用 Playwright Core + 本机 Chrome 执行 `tools/e2e/verify.mjs`：

```powershell
$env:RV_ADMIN_URL='http://localhost:8081'
$env:RV_PORTAL_URL='http://localhost:8082'
$env:RV_API_URL='http://localhost:18080'
node tools/e2e/verify.mjs
```

- 管理端：登录 → 看板 → 书籍/章节/作者/分类/采集/积分/用户/角色共 11 个路由，全部渲染。
- 门户：首页 → 书详情 → 阅读器 → 登录 → 个人中心，共 5 个页面截图。
- 浏览器控制台与网络：**0 个 404 / 0 个 pageerror**（Docker 端口最后一次运行）。
- 截图见 `docs/screenshots/`。

## 9. 验收结论

- **P0（认证/RBAC、内容域、阅读、书架、评分、排行、缓存、布隆、限流、构建与部署骨架）已完成并实测。**
- **P1（本地事件总线、审核通知、阅读统计、示例书源同步、签到、积分、兑换、对账）已完成并实测。**
- **未完成/限制**：真实 RabbitMQ 停消费者/死信演示、XXL-Job 实际接入、60% 测试覆盖、公网部署、中文 ngram 全文检索。详见 README「已知限制」。
- **可作为作品集演示**：管理端 + 门户 + 后端 API + Swagger + 截图 + 验收记录完整。

---

## 附：关键命令

```powershell
# 后端
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn -B test
mvn -B -DskipTests package
java -jar rv-server/target/rv-server-1.0.0.jar

# 前端
cd web-admin; npm install; npm run dev
cd ../web-portal; npm install; npm run dev

# 浏览器验收（Docker 端口）
$env:RV_ADMIN_URL='http://localhost:8081'
$env:RV_PORTAL_URL='http://localhost:8082'
$env:RV_API_URL='http://localhost:18080'
node tools/e2e/verify.mjs
```
