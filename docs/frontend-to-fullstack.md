# 前端转全栈导读(读这一篇就能看懂这个后端)

> 写给已经会 React / TypeScript / Vite / axios、正在学后端的你。
> 这份文档把后端每个概念**翻译成你已经熟悉的前端东西**,并指到本仓库里的**真实文件**。
> 建议对照 `java-backend/`(Java)或 `kotlin-backend/`(Kotlin)边读边看,前端在 `frontend/`。

---

## 1. 一句话心智模型

你在前端写 `axios.get('/api/articles')`,请求跨过网络,打到的"另一端"就是这个后端。
后端做的事:**接住请求 → 校验身份 → 找数据库要数据 → 组装成 JSON → 还给你**。

你在 `frontend/src/api/` 里写的每个函数,后端都有一个"对应的处理入口"。比如:

| 前端(你写的) | 后端(接住它的) |
|---|---|
| `frontend/src/api/articles.ts` 里的 `listArticles()` → `GET /api/articles` | `java-backend/.../web/ArticleController.java` 的 `list()` 方法 |
| `login()` → `POST /api/auth/login` | `AuthController.login()` |
| `uploadFile()` → `POST /api/files` | `FileController.upload()` |

**记住这条对应关系,后端就不神秘了:它就是你前端 `api/*.ts` 的服务端实现。**

---

## 2. 概念对照表(前端 ↔ 后端)

| 你在前端熟悉的 | 后端对应 | 本仓库文件 | 说明 |
|---|---|---|---|
| `src/api/*.ts`(用 axios 定义"调哪个 URL、什么方法、传什么") | **Controller** | `web/AuthController`、`web/ArticleController` | 定义 URL、HTTP 方法、入参/出参。只管"收发",不写业务 |
| 组件里的业务逻辑 / 自定义 Hook | **Service** | `service/AuthService`、`service/ArticleService` | 真正干活的地方:校验、组合、算结果、开事务 |
| (前端没有)你调后端 API,后端调**数据库**的"api 层" | **Mapper**(MyBatis-Plus) | `mapper/UserMapper`、`mapper/ArticleMapper` | 打数据库的接口。`BaseMapper` 自带增删改查,不用写 SQL |
| TS `interface` / `type`(描述数据形状) | **DTO** + **Entity** | `web/dto/*`、`domain/User`、`domain/Article` | DTO=对外的请求/响应形状;Entity=数据库表的映射 |
| `.env` / `vite.config.ts` | **application.yml** | `src/main/resources/application.yml` | 端口、数据库地址、密钥等配置。`${VAR:默认值}` 就是 env 兜底 |
| `package.json` + `pnpm install` | **pom.xml**(Maven)/ **build.gradle.kts**(Gradle) | 各后端根目录 | 声明依赖 + 构建脚本 |
| `npx`(不全局装也能跑) | `./mvnw` / `./gradlew`(wrapper) | 各后端根目录 | 自带的构建工具启动器,首次自动下载 |
| axios **请求拦截器**(自动加 `Authorization`) | **JWT 过滤器**(服务端反过来做:自动校验 token) | `security/JwtAuthenticationFilter` | 每个请求进来先过它,验 token、认出"你是谁" |
| axios **响应拦截器**(401 跳登陆) | **全局异常处理** | `web/GlobalExceptionHandler` | 统一把异常转成 `{code,message}`,不用每个接口 try/catch |
| React **Context / Provider** 把东西注入下去 | **依赖注入(DI)** | 到处的构造器注入(`AuthController(authService, ...)`) | 你不 `new` 依赖,框架从"上面"传进来。`@Service`/`@Component` 就是"可被注入的东西" |
| `react-query` / `SWR` 的缓存 | **Redis 缓存** | `cache/RedisArticleCache` | 把读结果缓存起来,下次不查库;写时失效 |
| 前端**路由守卫**(没登陆跳 login) | **SecurityConfig** 的 `authorizeHttpRequests` | `config/SecurityConfig` | 声明"哪些 URL 要登陆、哪些放行" |
| `new WebSocket(...)`(前端连) | **WebSocket 服务端** | `ws/ChatWebSocketHandler` | 你前端 `WebSocket` 连的那一端 |
| `new EventSource(...)`(前端收推送) | **SSE 服务端** | `sse/NotificationController`(`SseEmitter`) | 你前端 `EventSource` 连的那一端 |
| (前端少见)数据库表结构版本管理 | **Flyway 迁移** | `src/main/resources/db/migration/V1__init.sql` | 像给数据库结构做"git 版本",启动时自动建表 |
| ORM(用过 Prisma / TypeORM 的话) | **MyBatis-Plus** | `mapper/*` + `domain/*` | 对象 ↔ 数据库表 的自动映射 |
| 全局事件总线 / `mitt` | **Spring 事件**(`ApplicationEventPublisher`) | `event/ArticleCreatedEvent` + `@EventListener` | 发一个事件,多处监听(本项目:发文章 → WS + SSE 各自推送) |

---

## 3. 请求在后端怎么流动(分层架构)

前端也讲分层(组件 / hooks / api 层)。后端是这样一条链:

```
浏览器  ──HTTP──▶  [过滤器链:验 JWT]  ──▶  Controller  ──▶  Service  ──▶  Mapper  ──▶  MySQL
(你的 axios)        JwtAuthenticationFilter    web/*        service/*     mapper/*
                                                  │
                                                  └── 出参用 DTO 组装 ──▶ 自动转 JSON ──▶ 还给前端
```

每一层的职责(和前端类比):

- **过滤器(Filter)**:请求还没到业务就先过一遍。这里验 token、认出用户。≈ axios 拦截器,只不过在服务端、方向相反。
- **Controller**(`web/`):只管 HTTP —— 路径、方法、参数校验、返回。**不写业务逻辑**。≈ 你的 `api/*.ts`(定义调用)+ 一点参数校验。
- **Service**(`service/`):真正的业务。校验规则(用户名重复?)、组合数据、开数据库事务。≈ 组件里的业务函数 / 自定义 hook。
- **Mapper**(`mapper/`):只管跟数据库打交道。≈ "调 DB 的 api 层"。
- **DTO**(`web/dto/`):跨层传递的数据形状。≈ TS `interface`。

**为什么要分层?** 和前端"组件别把 fetch 和 UI 混在一起"一个道理:各司其职,好测、好改。

---

## 4. 跟着一个请求走一遍:发一篇文章

前端点"保存" → `createArticle(body)`(`frontend/src/api/articles.ts`)→ `POST /api/articles`。后端这一路:

1. **`JwtAuthenticationFilter`** 拦下请求,从 `Authorization: Bearer xxx` 解出你是谁,放进"安全上下文"。(没带 token?后面直接 401)
2. **`ArticleController.create()`** 接住,`@Valid` 自动校验入参(标题非空等),用 `@AuthenticationPrincipal` 拿到当前用户 id。
3. 调 **`ArticleService.create()`**:`@Transactional` 开事务 → 组装 `Article` 实体 → 调 `ArticleMapper.insert()` 落库 → 发一个 `ArticleCreatedEvent` 事件。
4. **`ArticleMapper.insert()`**(MyBatis-Plus 自带)生成 SQL 写进 MySQL 的 `articles` 表。
5. 事件被 **`ChatWebSocketHandler`** 和 **`NotificationController`(SSE)** 监听到 → 向所有在线前端**实时推送**"有新文章"。
6. Service 把实体转成 **`ArticleResponse`**(DTO)返回 → Spring 自动序列化成 JSON → 前端 `.then()` 收到。

打开前端 `/realtime` 页,另一个标签页发文章,你能亲眼看到第 5 步的实时推送到达。

---

## 5. 新手读代码顺序(Java 侧;Kotlin 结构一模一样)

从"配置"到"一个完整功能",按这个顺序读最顺:

1. `src/main/resources/application.yml` —— 配置(≈ `.env` + `vite.config`)
2. `src/main/resources/db/migration/V1__init.sql` —— 表结构(≈ 数据模型的"源头")
3. `domain/User.java`、`domain/Article.java` —— 实体(≈ TS model,但映射到表)
4. `mapper/UserMapper.java`、`mapper/ArticleMapper.java` —— 数据库访问(≈ 调 DB 的 api 层)
5. `web/dto/*.java` —— 请求/响应形状(≈ TS interface)
6. `service/AuthService.java` —— 业务:注册/登陆(≈ 业务 hook)
7. `web/AuthController.java` —— 接口(≈ `frontend/src/api/auth.ts` 的服务端)
8. `security/JwtService` + `JwtAuthenticationFilter` + `config/SecurityConfig` —— 鉴权(≈ axios 拦截器 + 路由守卫)
9. `service/ArticleService.java` + `web/ArticleController.java` —— 文章全套(含分页、缓存、事件)
10. `cache/RedisArticleCache.java` —— 缓存(≈ react-query)
11. `storage/MinioStorageService.java` —— 文件上传(≈ 你前端 FormData 上传的落地端)
12. `ws/ChatWebSocketHandler` + `sse/NotificationController` —— 实时(≈ 你前端 `WebSocket`/`EventSource` 的另一端)

---

## 6. 注解速查小抄(看到 `@xxx` 别慌)

| 注解 | 作用 | 前端类比 |
|---|---|---|
| `@RestController` | 这个类是一组 HTTP 接口 | 一个 `api/xxx.ts` 文件 |
| `@GetMapping` / `@PostMapping("/x")` | 绑定 URL + 方法 | `axios.get('/x')` 的服务端声明 |
| `@RequestBody` | 把请求 JSON 转成对象 | `JSON.parse(req.body)` |
| `@RequestParam` | 取查询参数 `?page=1` | `req.query.page` |
| `@PathVariable` | 取路径参数 `/articles/{id}` | 路由参数 `:id` |
| `@Valid` | 自动校验入参 | zod / yup 校验 |
| `@Service` / `@Component` | "可被注入的对象" | 放进 Context 的东西 |
| 构造器注入 | 依赖从外面传进来 | `useContext()` 拿依赖 |
| `@Transactional` | 方法内的数据库操作要么全成功要么全回滚 | (前端没有,数据库特有) |
| `@RestControllerAdvice` | 全局异常处理 | axios 响应拦截器统一处理错误 |
| `@EventListener` | 监听某个事件 | 订阅事件总线 |
| `@Bean` | 手动造一个"可注入对象"放进容器 | 手动 `createContext` 并 provide 一个值 |

---

## 7. 构建工具:Maven / Gradle ≈ npm / pnpm

| 前端 | Java(Maven) | Kotlin(Gradle) |
|---|---|---|
| `package.json` | `pom.xml` | `build.gradle.kts` |
| `dependencies` 里的包 | `<dependency>` | `implementation("...")` |
| `pnpm install` | 首次 `./mvnw` 自动下 | 首次 `./gradlew` 自动下 |
| `npm run dev` | `./mvnw spring-boot:run` | `./gradlew bootRun` |
| `npm test` | `./mvnw test` | `./gradlew test` |
| `npx`(免全局装) | `./mvnw`(wrapper) | `./gradlew`(wrapper) |

> 本项目 Java 用 Maven、Kotlin 用 Gradle,**故意用不同工具方便你对比**。两者都由根目录 `Makefile` 统一入口(`make java` / `make kotlin`)。

---

## 8. Java vs Kotlin

两套后端**业务完全一样**,只是语言不同。逐项写法对照见 [`AGENTS.md` 第 9 节](../AGENTS.md)。一句话:Kotlin 更简洁(`data class`、空安全 `?`、`val`),Java 更显式。先看哪套都行,看完另一套会很快。

---

## 9. 还想深入?

- 完整技术说明、API 契约、运行方式:[`AGENTS.md`](../AGENTS.md)
- 前端代码本身就是最好的对照:`frontend/src/api/`(调用)、`frontend/src/auth/`(登陆态)、`frontend/src/pages/`(页面)。后端每个接口都能在这里找到调用方。
