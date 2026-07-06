# study-enterprise — 项目知识库(AGENTS.md)

本文件是仓库**唯一事实源**。读它就能理解目标、跑起来、看懂两套后端差异。

> 🧭 **前端转全栈的同学**:先读 [`docs/frontend-to-fullstack.md`](docs/frontend-to-fullstack.md) —— 把后端每个概念翻译成你熟悉的前端东西(概念对照表、请求流动、读代码顺序、注解小抄)。

## 1. 目标

用 Java 与 Kotlin 各写一套**业务完全相同**的企业级 Spring Boot 后端(以"语言"为唯一变量对照学习),
覆盖:注册/登陆(JWT)、文章 CRUD + 分页搜索、Redis 缓存与浏览量、MinIO 文件上传、Yjs/Hocuspocus 实时多人协作笔记。
共用一个 React + Rsbuild + Tailwind 前端串起全链路。暂不用 monorepo。

## 2. 技术栈

| 层 | 选型 |
|---|---|
| 语言 | Java 21 / Kotlin(JVM 21) |
| 框架 | Spring Boot 3.5.16 |
| 构建 | Java 用 **Maven** / Kotlin 用 **Gradle**(Kotlin DSL,刻意不同以对比) |
| 数据库 | MySQL 8(每套后端独立 schema) |
| 持久层 | **MyBatis-Plus 3.5.12**(+ 分页插件)+ Flyway 迁移 |
| 缓存 | **Redis 7**(文章详情缓存 + 浏览量计数) |
| 认证 | JWT(HS,jjwt 0.12)+ Spring Security 无状态 + BCrypt |
| 对象存储 | MinIO(官方 Java SDK) |
| 实时 | WebSocket(聊天室)+ SSE(通知流),Spring 事件驱动 |
| 协作编辑 | NestJS + Hocuspocus + Yjs(短期协作 token + MySQL binary snapshot) |
| 前端 | React + Rsbuild + TypeScript + Tailwind + HeroUI |
| 基础设施 | Docker Compose(MySQL + MinIO + Redis) |

> 注:Java 侧未用 Lombok(JDK 25 上其注解处理器有兼容风险,故手写 getter/setter);企业中常用 Lombok,可自行加回。Kotlin 侧用 data class 天然免样板。
>
> **JDK 统一**:两套后端目标字节码均 **Java 21**。Java(Maven)用 `--release 21` 在本机 JDK 上编译;Kotlin(Gradle)用 `jvmToolchain(21)` 锁定 JDK 21(本机无则经 foojay 自动下载),**不受机器 `JAVA_HOME` 影响**(本机 `JAVA_HOME` 恰好指向 JDK 17,直接跑会踩坑,工具链规避了它)。
>
> **Node 版本**:前端使用 **Rsbuild**。Rsbuild/Rspack 要求 Node.js `20.19+` 或 `22.12+`。如果 `node -v`
> 显示 `20.12.x`,先 `export PATH="$HOME/.nvm/versions/node/v22.18.0/bin:$PATH"` 再跑 `make web`。

## 3. 目录结构

```
study-enterprise/
├── AGENTS.md
├── docker-compose.yml         # MySQL + MinIO + Redis
├── .env.example               # 复制为 .env 使用
├── infra/mysql/init/          # 建两套 schema
├── java-backend/              # Spring Boot(Java):18080
├── kotlin-backend/            # Spring Boot(Kotlin):18081
├── collab-server/             # NestJS + Hocuspocus 协作服务:19082/collab/notes
└── frontend/                  # React + Rsbuild + Tailwind + HeroUI:15173
```

Java 后端包结构:`domain / mapper / service / storage / cache / security / config / web(controller+dto) / exception`。

## 4. 端口

| 服务 | 端口 |
|---|---|
| Java 后端 | 18080 |
| Kotlin 后端 | 18081 |
| MySQL | 13306 |
| MinIO API / Console | 19100 / 19101 |
| Redis | 16379 |
| 协作服务(Hocuspocus) | 19082(`/collab/notes`) |
| 前端 Rsbuild | 15173 |

## 5. 如何运行

**推荐用 `make`**(根目录 Makefile;`make` 列出全部命令):

```bash
make up        # 起 MySQL + MinIO + Redis
make java      # Java 后端 :18080
make kotlin    # Kotlin 后端 :18081
make collab    # 协作服务 :19082/collab/notes(默认连 Java 后端)
make web       # 前端 :15173
make test      # 两套后端测试
# 端口被占用时:SERVER_PORT=18090 REDIS_PORT=16380 make java
```

等价的手工命令:

```bash
cp .env.example .env
docker compose up -d           # 起 mysql + minio + redis(首次自动建两库)

# Java 后端(:18080)
cd java-backend && ./mvnw spring-boot:run
#   跑测试(Testcontainers 起 MySQL+Redis,需 Docker):./mvnw test

# Kotlin 后端(:18081)—— Gradle
cd kotlin-backend && ./gradlew bootRun
#   跑测试:./gradlew test

# 协作服务(:19082/collab/notes)—— NestJS + Hocuspocus,默认连 Java 后端
cd collab-server && pnpm install && pnpm dev
#   切 Kotlin 后端:COLLAB_BACKEND_URL=http://localhost:18081 pnpm dev

# 前端(:15173,Rsbuild)
cd frontend && pnpm install && pnpm dev
```

MinIO 控制台:http://localhost:19101(`minioadmin` / `minioadmin123`)。

### 5.1 协作笔记启动教程

**Java 后端链路**(默认):

```bash
make up       # 终端 1:MySQL + MinIO + Redis
make java     # 终端 2:Java 后端 :18080
make collab   # 终端 3:NestJS + Hocuspocus :19082/collab/notes,默认连 Java 后端
make web      # 终端 4:React 前端 :15173
```

打开 http://localhost:15173 。

**Kotlin 后端链路**:

```bash
make up
make kotlin
cd collab-server && COLLAB_BACKEND_URL=http://localhost:18081 pnpm dev
cd frontend && VITE_API_BASE_URL=http://localhost:18081 pnpm dev
```

**学习账号**:项目不内置种子账号。启动后在 `/register` 注册:

| 用户名 | 密码 |
|---|---|
| `alice_demo` | `secret123` |
| `bob_demo` | `secret123` |

多人协作测试:用 `alice_demo` 新建笔记,在成员面板搜索 `bob_demo` 并添加为编辑者,再用两个浏览器窗口分别登陆两人并打开同一篇笔记。

## 6. 环境变量

见 `.env.example`。关键:`DB_*`、`MINIO_*`、`REDIS_*`、`JWT_*`、`COLLAB_*`。Spring 用 application.yml 的 `${VAR:default}` 兜底,本地不设也能跑。

## 7. API 契约(两套后端一致)

前缀 `/api`。除注册/登陆外均需 `Authorization: Bearer <token>`。

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/auth/register` | `{username,password}` → 201 `{id,username}` |
| POST | `/api/auth/login` | → 200 `{token,tokenType,expiresIn}` |
| GET | `/api/auth/me` | → 200 `{id,username}` |
| GET | `/api/users?keyword=` | 搜索用户,用于协作成员选择 → `[{id,username}]` |
| GET | `/api/articles?page=0&size=10&keyword=` | 分页+标题搜索 → `PageResponse` |
| GET | `/api/articles/stats` | 按分类统计(手写 SQL @Select GROUP BY)→ `[{category,count}]` |
| GET | `/api/articles/{id}` | 详情(浏览量 +1)→ `Article` |
| POST | `/api/articles` | `{title,content,category?,coverImageKey?}` → 201 |
| PUT | `/api/articles/{id}` | 更新(需作者)→ 200 |
| DELETE | `/api/articles/{id}` | 删除(需作者)→ 204 |
| POST | `/api/files` | multipart `file`(仅图片≤5MB)→ 201 `{key,url}` |
| GET | `/api/files/url?key=` | 换预签名 URL |
| GET | `/api/notes` | 当前用户可访问的协作笔记列表 |
| POST | `/api/notes` | `{title}` → 201 `Note`(自动创建 OWNER 成员和空 Yjs 文档) |
| GET | `/api/notes/{id}` | 笔记元数据(需成员权限) |
| PUT | `/api/notes/{id}` | 改标题(需 OWNER) |
| DELETE | `/api/notes/{id}` | 删除笔记(需 OWNER) |
| GET | `/api/notes/{id}/members` | 成员列表 |
| POST | `/api/notes/{id}/members` | `{userId,role}` 添加/更新 EDITOR/VIEWER(需 OWNER) |
| DELETE | `/api/notes/{id}/members/{userId}` | 移除成员(需 OWNER) |
| POST | `/api/notes/{id}/collab-token` | 换 5 分钟短期协作 token,供 Hocuspocus 鉴权 |
| WS | `ws://localhost:19082/collab/notes` | Hocuspocus 协作连接,文档名 `note:{id}`,token 用 `/collab-token` 返回值 |
| WS | `/ws/chat?token=` | **WebSocket** 聊天室(握手 token 鉴权,双向广播;新文章推系统消息) |
| GET | `/api/sse/notifications?token=` | **SSE** 通知流(EventSource;新文章实时推送 `article-created`) |

> 实时:文章创建时后端发布 `ArticleCreatedEvent`(Spring 事件),WS 与 SSE 各自 `@EventListener` 推送。浏览器 WS/EventSource 不能带 header,故 token 走查询参数握手鉴权。
>
> 协作编辑:普通业务权限仍在 Java/Kotlin 后端。前端先用登录 JWT 请求 `/api/notes/{id}/collab-token`,再把短期 token 交给 Hocuspocus Provider。NestJS 协作服务只校验 `typ=collab` 的短期 token,并通过内部接口 `/api/notes/internal/{id}/document` 读写 Yjs binary snapshot(用 `X-Collab-Secret` 保护)。

`Article`:`{id,title,content,category,coverImageKey,coverImageUrl,authorId,authorUsername,createdAt,updatedAt,viewCount}`。
`Note`:`{id,title,ownerId,ownerUsername,role,createdAt,updatedAt}`。
错误:`{code,message}`(校验附 `errors`)。code:`VALIDATION_ERROR/UNAUTHORIZED/FORBIDDEN/NOT_FOUND/CONFLICT/INVALID_FILE/INTERNAL_ERROR`。

## 8. Redis 用法(企业常见两种)

- **详情缓存**:`GET /articles/{id}` 先查 Redis(`{app}:article:{id}`,JSON,10 分钟 TTL),命中免查库;更新/删除时失效(`RedisArticleCache`)。
- **浏览量计数**:每次看详情 `INCR {app}:article:views:{id}`,响应带 `viewCount`。
- **key 命名空间**:key 以 `spring.application.name` 为前缀(`java-backend:` / `kotlin-backend:`),避免两套后端共用一个 Redis 时按 id 撞键脏读。

## 9. Java vs Kotlin 写法对照

同一业务两套实现,差异集中在语言层(架构/契约完全一致):

| 维度 | Java(`java-backend`) | Kotlin(`kotlin-backend`) |
|---|---|---|
| 实体 | 手写 getter/setter(避 Lombok 的 JDK25 风险) | `class` + `var` + 默认值(免样板,MP 反射用无参构造) |
| DTO | `record`(不可变) | `data class`(不可变 `val`) |
| 空处理 | 显式判空 / `Optional` | 空安全类型 `?` + `?.` / `?:` / `!!` |
| MP 条件构造 | `LambdaQueryWrapper`(方法引用) | `QueryWrapper` 字符串列(避 Kotlin lambda 序列化坑) |
| Security DSL | `http.csrf(c -> c.disable())` | `http.csrf { it.disable() }`(尾随 lambda) |
| 配置绑定 | `@ConfigurationProperties` + setter | `@ConfigurationProperties data class`(构造器绑定) |
| 单元测试 mock | Mockito(`@Mock` / `when`) | MockK(`mockk` / `every`) |
| Mock 依赖坐标 | `io.mockk:mockk-jvm`(Maven 不读 Gradle 元数据) | `io.mockk:mockk`(Gradle 读元数据自动转 -jvm) |
| 构建工具 | Maven(`pom.xml` + `mvnw`) | Gradle(`build.gradle.kts` + `gradlew`,JDK 21 工具链) |
| 入口 | `SpringApplication.run(...)` | `runApplication<...>(*args)` |
| 分页回填 | `new PageResponse<>(...)` | `PageResponse(...)`(具名/默认参数) |

**全链路工具都用到了**:MySQL(Flyway 建表 + MyBatis-Plus CRUD/分页 + **手写 @Select SQL** 统计)、Redis(详情缓存 + 浏览量,key 按应用前缀隔离)、MinIO(上传预签名 URL + **删文章时删对象**)、**WebSocket**(聊天室,双向)、**SSE**(通知流,服务端推送;均由 Spring 事件驱动)。

构建都用 Maven;Kotlin 需 `kotlin-maven-plugin` + `kotlin-spring`(all-open,让 `@Service`/`@Configuration` 可被 CGLIB 代理)。
⚠️ Kotlin 编译器需 **2.2+** 才支持在 JDK 25 上运行(本项目用 `2.3.21`;`2.1.0` 会在 JRT 初始化时崩)。

## 10. 里程碑(全部完成 ✅)

- [x] 基础设施(MySQL + MinIO + Redis)
- [x] Java 后端全链路(MyBatis-Plus + Redis,11 项测试全绿 + 真实 smoke)
- [x] Kotlin 后端全链路(镜像契约,MockK + Testcontainers,11 项全绿 + smoke)
- [x] React 前端(登陆/注册/列表搜索分页/详情/编辑 + 封面上传)
- [x] 浏览器 E2E(Playwright):前端 → Java 后端 → MySQL/Redis/MinIO 全链路跑通(注册→建文章→详情,Redis 浏览量、作者权限均正确)

## 11. 环境备注(重要)

- 本项目最初在 `~/Downloads/backend-study`(含 JPA 版 P1/P2,已提交在那个 git 仓库)。开发中该目录被本机 OS 层锁死(所有已存在文件不可读写),故迁到 `study-enterprise` 按升级后的目标(MyBatis-Plus + Redis)重建。
- 本机 **8080 被你另一个 Java 进程占用**:Java 后端用 `SERVER_PORT=8090 ./mvnw spring-boot:run` 换端口。
- 本机 **6379 被你另一个 redis 占用**:本会话 study-redis 跑在 **6380**;交付默认仍 6379。跑应用时可 `REDIS_PORT=6380` 覆盖。

## 12. 未来扩展(暂不做)

Lombok、RBAC 权限、刷新 token、标签/评论、Redis 分布式锁/限流、CI/CD、monorepo。
