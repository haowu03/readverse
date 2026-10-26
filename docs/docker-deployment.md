# ReadVerse Docker Desktop 部署说明

> 目标：不依赖本机 JDK/Maven/MySQL/Redis/Node，只用 Docker Desktop 一条命令启动完整环境。

## 1. 部署拓扑

```text
浏览器
  │
  ├── http://localhost:8081  ──  rv-web-admin  (Nginx + Vue 管理后台)
  │                                  │  /api 反向代理
  └── http://localhost:8082  ──  rv-web-portal (Nginx + Vue 读者门户)
                                     │
                                     ▼
                              rv-backend :8080
                               │        │
                         rv-mysql     rv-redis-docker
                         :3306        :6379
                         (宿主 13306)  (宿主 6380)
```

后端容器内部仍然访问 `mysql:3306` 和 `redis:6379`，宿主机端口只是为了方便排查，避免和本机已有服务冲突。

## 2. 文件清单

| 文件 | 作用 |
|---|---|
| `deploy/docker-compose.yml` | 5 个服务：mysql / redis / backend / web-admin / web-portal（rabbitmq 为可选 profile） |
| `deploy/backend.Dockerfile` | Maven 多阶段构建后端 fat jar，运行在 JRE 17 |
| `deploy/admin.Dockerfile` | Node 构建管理端，Nginx 托管 |
| `deploy/portal.Dockerfile` | Node 构建门户，Nginx 托管 |
| `deploy/nginx/default.conf` | `/api` 与 `/uploads` 反代到 `backend:8080`，SPA history fallback |
| `rv-server/src/main/resources/application-docker.yml` | Docker profile 数据源/Redis/JWT/上传目录 |
| `deploy/.env.example` | 密码与宿主机端口模板 |
| `scripts/docker-start.ps1` | 一键构建 + 启动 + 等健康检查 |
| `scripts/docker-stop.ps1` | 停止 compose |
| `.dockerignore` | 排除 target/node_modules/dist/tools 等构建上下文 |

## 3. 启动

### 3.1 前置

1. 打开 **Docker Desktop**，等待鲸鱼图标稳定。
2. 确认引擎可用：

```powershell
docker info --format '{{.ServerVersion}} {{.OSType}}'
# 29.9.2 linux
```

3. 默认端口未被占用：`8081`、`8082`、`18080`、`13306`、`6380`。

### 3.2 一键启动

```powershell
cd C:\Users\16098\Desktop\ReadVerse
powershell -ExecutionPolicy Bypass -File .\scripts\docker-start.ps1
```

首次启动时镜像不存在会自动构建（后端 Maven 构建约 1–2 分钟，前端 npm 构建约 1 分钟），然后启动。  
后续日常启动直接运行同一命令，不会重复构建：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\docker-start.ps1
```

修改代码后需要重建：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\docker-start.ps1 -Build
```

### 3.3 等价的 compose 命令

```powershell
cd C:\Users\16098\Desktop\ReadVerse

docker compose -f deploy/docker-compose.yml up -d --build
docker compose -f deploy/docker-compose.yml ps
```

正常状态：

```text
rv-mysql          healthy   13306->3306
rv-redis-docker   healthy   6380->6379
rv-backend        healthy   18080->8080
rv-web-admin      healthy   8081->80
rv-web-portal     healthy   8082->80
```

## 4. 访问

| 端 | 地址 | 账号 |
|---|---|---|
| 管理后台 | http://localhost:8081 | `admin / Admin@123`、`operator / Operator@123` |
| 读者门户 | http://localhost:8082 | `reader / Reader@123` |
| API / Swagger | http://localhost:18080/swagger-ui.html | 无 |
| 健康检查 | http://localhost:18080/actuator/health | 无 |
| MySQL（宿主工具连接） | `localhost:13306` | `root / 123456`，库 `readverse` |
| Redis（宿主工具连接） | `localhost:6380` | 密码 `readverse` |

管理端和门户的 `/api` 都由 Nginx 反代到 `backend:8080`，所以浏览器只访问 8081/8082 即可，不需要单独处理 CORS。

## 5. 停止与重置

```powershell
# 停止，保留 MySQL 数据
powershell -ExecutionPolicy Bypass -File .\scripts\docker-stop.ps1

# 停止并删除数据卷（下次启动重新建表 + 种子数据）
powershell -ExecutionPolicy Bypass -File .\scripts\docker-stop.ps1 -RemoveVolumes
```

等价 compose：

```powershell
docker compose -f deploy/docker-compose.yml down
docker compose -f deploy/docker-compose.yml down -v
```

## 6. 修改端口/密码

```powershell
Copy-Item deploy\.env.example deploy\.env
notepad deploy\.env
```

可修改：

```dotenv
MYSQL_PORT=13306
REDIS_PORT=6380
BACKEND_PORT=18080
ADMIN_PORT=8081
PORTAL_PORT=8082
MYSQL_ROOT_PASSWORD=123456
REDIS_PASSWORD=readverse
RV_JWT_SECRET=change-me
```

修改后重建：

```powershell
docker compose -f deploy/docker-compose.yml up -d
```

> `docker-start.ps1` 的健康检查默认访问 `18080`；如果改了 `BACKEND_PORT`，请手动执行 compose 命令，或同步修改脚本中的健康检查地址。

## 7. 代码更新后的重建

| 改动 | 命令 |
|---|---|
| 后端 Java/SQL | `docker compose -f deploy/docker-compose.yml up -d --build backend` |
| 管理端 | `docker compose -f deploy/docker-compose.yml up -d --build web-admin` |
| 门户 | `docker compose -f deploy/docker-compose.yml up -d --build web-portal` |
| 全部 | `docker compose -f deploy/docker-compose.yml up -d --build` |

只改配置/环境变量不需要重建镜像，`up -d --force-recreate backend` 即可。

## 8. 日志与排障

```powershell
# 全部日志
docker compose -f deploy/docker-compose.yml logs -f

# 单个服务
docker compose -f deploy/docker-compose.yml logs -f backend
docker compose -f deploy/docker-compose.yml logs -f mysql
```

常见问题：

| 现象 | 原因/处理 |
|---|---|
| `Bind for 0.0.0.0:xxxx failed: port is already allocated` | 宿主机端口被占用；改 `deploy/.env` 的端口，或停掉占用进程 |
| `backend` 一直 unhealthy | 看 `docker compose logs backend`；通常是 MySQL/Redis 未 healthy 或数据库密码不匹配 |
| 管理端/门户打不开 | 看 `docker compose ps`；Nginx 健康检查应返回 `ok` |
| 首次启动没有数据 | 首次启动需要 1–3 秒执行 schema + 种子；看日志 `Demo data seeded` |
| 想清空数据 | `docker compose down -v` 后重新 `up -d` |
| Docker 引擎不可用 | 先打开 Docker Desktop，等 `docker info` 正常 |

## 9. 可选 RabbitMQ

默认 `RV_MQ_ENABLED=false`，使用本地事件总线，不需要 RabbitMQ。启用真实 broker：

```powershell
# 1. 启动 mq profile
docker compose -f deploy/docker-compose.yml --profile mq up -d rabbitmq

# 2. deploy\.env 增加 RV_MQ_ENABLED=true
# 3. 重建 backend
docker compose -f deploy/docker-compose.yml up -d --force-recreate backend
```

RabbitMQ 管理台：<http://localhost:15673>（`rv / rv123456`）。

## 10. 本次 Docker 验收

- 5 个容器均 `healthy`：MySQL、Redis、backend、web-admin、web-portal。
- `GET http://localhost:18080/actuator/health` → `UP`。
- 后端日志：`Demo data seeded: roles=3, permissions=57, users=3, books=30`，布隆预热 150 章。
- 通过 Nginx 访问管理端登录成功，权限点 57。
- 门户经 `8082` 代理读取首页、书详情、阅读器、个人中心正常。
- Playwright 对 Docker 端口执行 11 个管理端路由 + 5 个门户页面，控制台错误 0。
- 镜像：`readverse/backend:1.0.0`、`readverse/web-admin:1.0.0`、`readverse/web-portal:1.0.0`。

## 11. 安全提示

- `deploy/.env.example` 中的密码/JWT secret 仅用于本地演示；部署到公网前必须更换。
- 不要把 `deploy/.env` 提交到 Git（已在 `.gitignore` 中忽略）。
- 生产环境建议在 Nginx 层加 HTTPS、限制管理端入口 IP，并使用独立数据库账号而不是 root。

