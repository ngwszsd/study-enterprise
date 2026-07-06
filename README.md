# study-enterprise

Java / Kotlin 双语言企业级全链路学习项目:登陆(JWT)· 文章 CRUD · MySQL(MyBatis-Plus)· Redis(缓存/浏览量)· MinIO 上传 · Yjs/Hocuspocus 多人协作笔记。两套后端功能一致,共用一个 React 前端。

## 快速开始(Make)

前端已从 Vite 切到 **Rsbuild**。Rsbuild/Rspack 要求 Node.js `20.19+` 或 `22.12+`;如果本机 `node -v`
低于这个版本,先切到 Node 22,例如:

```bash
export PATH="$HOME/.nvm/versions/node/v22.18.0/bin:$PATH"
```

```bash
make          # 列出所有命令
make up       # 起 MySQL + MinIO + Redis
make java     # Java 后端 :18080     make kotlin  # Kotlin 后端 :18081
make collab   # 协作服务 :19082/collab/notes      make web     # 前端 :15173
make test     # 两套后端测试
```

📖 **前端转全栈的同学先读**:[前端转全栈导读](./docs/frontend-to-fullstack.md)(把后端每个概念翻译成你熟悉的前端东西 + 读代码顺序)。

## 启动教程

### Java 后端链路

开 4 个终端:

```bash
# 1. 基础设施
make up

# 2. Java 后端(:18080)
make java

# 3. 协作服务(:19082,默认连 Java 后端)
make collab

# 4. 前端(:15173,Rsbuild)
make web
```

浏览器打开 http://localhost:15173 。

### Kotlin 后端链路

```bash
# 1. 基础设施
make up

# 2. Kotlin 后端(:18081)
make kotlin

# 3. 协作服务改连 Kotlin 后端
cd collab-server
COLLAB_BACKEND_URL=http://localhost:18081 pnpm dev

# 4. 前端改连 Kotlin 后端
cd ../frontend
VITE_API_BASE_URL=http://localhost:18081 pnpm dev
```

## 整体功能链路

这个项目有 4 类服务:

| 服务 | 职责 | 默认端口 |
|---|---|---|
| React + Rsbuild 前端 | 页面、登录态、REST 调用、WS/SSE/Hocuspocus 客户端 | 15173 |
| Java Spring Boot 后端 | 业务 API、JWT、文章、文件、Redis、WS/SSE、协作笔记元数据 | 18080 |
| Kotlin Spring Boot 后端 | 和 Java 后端功能完全一致,用于语言对照学习 | 18081 |
| NestJS + Hocuspocus 协作服务 | Yjs 实时同步、协作鉴权、文档快照加载/保存 | 19082(`/collab/notes`) |

### 1. 登录鉴权链路

```text
前端 /login
  -> POST /api/auth/login
  -> Java/Kotlin AuthController
  -> AuthService 校验 BCrypt 密码
  -> JwtService 签发普通登录 JWT
  -> 前端 localStorage 保存 study_token
  -> 后续 axios 请求自动带 Authorization: Bearer <token>
  -> JwtAuthenticationFilter 解析 token 并写入 SecurityContext
  -> Controller 通过 @AuthenticationPrincipal 拿当前用户
```

学习重点:
- 前端不传 `userId`,服务端只信 JWT 解析出来的用户。
- Spring Security 是无状态的,不使用服务端 session。
- WS/SSE 因浏览器限制不能自定义 header,所以握手 token 走查询参数。

### 2. 文章 CRUD + Redis + MinIO 链路

```text
前端文章页
  -> /api/articles REST API
  -> ArticleController
  -> ArticleService
  -> MyBatis-Plus Mapper
  -> MySQL articles/users 表
```

读详情:

```text
GET /api/articles/{id}
  -> RedisArticleCache 先查 java-backend:article:{id}(Kotlin 链路为 kotlin-backend:article:{id})
  -> 未命中再查 MySQL 并写入 Redis,TTL 10 分钟
  -> Redis INCR java-backend:article:views:{id}(Kotlin 链路同样使用 kotlin-backend: 前缀)
  -> 返回 ArticleResponse
```

新建/更新/删除:
- 新建文章会发布 `ArticleCreatedEvent`,同时驱动 WebSocket 聊天室系统消息和 SSE 通知。
- 更新文章会失效 Redis 详情缓存。
- 删除文章会删除 MySQL 记录、清 Redis 缓存,如果有封面图也删除 MinIO 对象。

封面上传:

```text
前端选择图片
  -> POST /api/files multipart
  -> FileController
  -> MinioStorageService 校验 image/*
  -> MinIO 保存 object key
  -> 文章表只存 coverImageKey
  -> 详情接口按 key 生成 1 小时预签名 URL
```

### 3. WebSocket 与 SSE 实时链路

聊天室 WebSocket:

```text
前端 new WebSocket('/ws/chat?token=...')
  -> WebSocketAuthInterceptor 从 query token 校验 JWT
  -> ChatWebSocketHandler 保存 WebSocketSession
  -> 用户发送消息时广播给所有在线 session
```

文章创建通知:

```text
ArticleService.create()
  -> publishEvent(new ArticleCreatedEvent(...))
  -> ChatWebSocketHandler @EventListener 推 WS 系统消息
  -> NotificationController @EventListener 推 SSE article-created
```

学习重点:
- WebSocket 是双向通信,适合聊天室。
- SSE 是服务端到客户端的单向推送,适合通知流。
- Spring 事件把“文章创建”与“实时推送”解耦。

### 4. 多人协作笔记链路

协作笔记分两层:
- Java/Kotlin 后端:管业务数据、成员权限、短期 collab token、Yjs 快照落库。
- NestJS/Hocuspocus:管实时 Yjs 同步、协作光标、断线重连、debounce 后保存快照。

打开协作笔记:

```text
前端 /notes/{id}
  -> GET /api/notes/{id} 校验当前用户是成员
  -> GET /api/notes/{id}/members 获取成员列表
  -> POST /api/notes/{id}/collab-token 换 5 分钟短期协作 token
  -> HocuspocusProvider 连接 ws://localhost:19082/collab/notes,documentName = note:{id}
```

Hocuspocus 握手与文档加载:

```text
Hocuspocus onAuthenticate
  -> JwtVerifier 校验 typ=collab、docName、过期时间、签名
  -> 写入 userId/username/noteId/role 到连接 context

Hocuspocus onLoadDocument
  -> BackendClient GET /api/notes/internal/{id}/document
  -> Java/Kotlin NoteController 校验 X-Collab-Secret
  -> NoteService 从 note_documents.ydoc_state 读取 BLOB
  -> Base64 -> Uint8Array -> Y.applyUpdate(document, state)
```

编辑与保存:

```text
两个浏览器打开同一篇 note
  -> TipTap Collaboration 绑定同一个 Y.Doc 字段 rich-content
  -> CollaborationCaret 通过 awareness 显示对方光标/用户名
  -> Hocuspocus 在服务端同步 Yjs update
  -> onStoreDocument debounce 后 Y.encodeStateAsUpdate(document)
  -> BackendClient PUT /api/notes/internal/{id}/document
  -> NoteService 保存到 note_documents.ydoc_state
```

成员搜索:

```text
成员面板输入关键字
  -> GET /api/users?keyword=
  -> UserService 只查询 id/username
  -> OWNER 调 POST /api/notes/{id}/members 添加 EDITOR
```

学习重点:
- 普通 JWT 只用于业务 API。
- 协作 WS 不直接用普通 JWT,而是用短期 `typ=collab` token,降低泄漏风险。
- NestJS 不直接连数据库,所有业务权限仍由 Java/Kotlin 后端统一处理。
- Yjs 内容不是文章 HTML,而是 CRDT 二进制快照,所以用 BLOB 保存。

### 两个学习账号

项目不内置种子账号。启动后在 `/register` 注册:

| 用户名 | 密码 |
|---|---|
| `alice_demo` | `secret123` |
| `bob_demo` | `secret123` |

也可以用接口注册(Java 后端示例):

```bash
curl -X POST http://localhost:18080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"alice_demo","password":"secret123"}'

curl -X POST http://localhost:18080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"bob_demo","password":"secret123"}'
```

多人协作测试:用 `alice_demo` 新建笔记,在成员面板搜索 `bob_demo` 并添加为编辑者,再用两个浏览器窗口分别登陆两人并打开同一篇笔记。

完整说明见 [`AGENTS.md`](./AGENTS.md)。
