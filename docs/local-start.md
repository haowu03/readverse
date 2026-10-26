# 无 Docker 时的本地备用启动

> 首选方式见 README 的「Docker Desktop 启动」。以下仅用于没有 Docker Desktop、但本机已有 JDK/MySQL/Redis/Node 的情况。

## 1. 前置条件

- JDK 17
- Maven 3.9+（本机也可用 `tools/apache-maven-3.9.16`）
- MySQL 8（`localhost:3306`，默认 `root/123456`）
- Redis（`localhost:6379`，密码 `readverse`）
- Node 22 + npm 10

## 2. 一键脚本

```powershell
cd C:\Users\16098\Desktop\ReadVerse
powershell -ExecutionPolicy Bypass -File .\scripts\start.ps1
```

停止：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\stop.ps1
# 同时停 Redis：-IncludeRedis
```

## 3. 手动启动

### 3.1 建库

```sql
CREATE DATABASE IF NOT EXISTS readverse
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
```

首次启动会自动执行 `schema.sql` 并写入 30 本书、150 章和演示账号。

### 3.2 启动 Redis

```powershell
cd C:\Users\16098\Desktop\ReadVerse\tools\redis
.\redis-server.exe redis.conf
```

### 3.3 启动后端

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

cd C:\Users\16098\Desktop\ReadVerse
& .\tools\apache-maven-3.9.16\bin\mvn.cmd -B -DskipTests package
java -jar rv-server\target\rv-server-1.0.0.jar
```

验证：`http://localhost:8080/actuator/health` 返回 `UP`。

### 3.4 启动前端

```powershell
cd web-admin
npm.cmd install
npm.cmd run dev      # 5173

cd ..\web-portal
npm.cmd install
npm.cmd run dev      # 5174
```

## 4. 演示账号

| 账号 | 密码 | 角色 |
|---|---|---|
| `admin` | `Admin@123` | 管理员 |
| `operator` | `Operator@123` | 运营 |
| `reader` | `Reader@123` | 读者 |

## 5. 没有 Redis 时

后端加参数 `--rv.redis.enabled=false`，Token、缓存、布隆和限流会降级到本地实现，仅用于快速看页面。
