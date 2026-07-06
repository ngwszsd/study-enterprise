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
make collab   # 协作服务 :19082      make web     # 前端 :15173
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
