# study-enterprise

Java / Kotlin 双语言企业级全链路学习项目:登陆(JWT)· 文章 CRUD · MySQL(MyBatis-Plus)· Redis(缓存/浏览量)· MinIO 上传。两套后端功能一致,共用一个 React 前端。

## 快速开始(Make)

```bash
make          # 列出所有命令
make up       # 起 MySQL + MinIO + Redis
make java     # Java 后端 :18080     make kotlin  # Kotlin 后端 :18081
make web      # 前端 :15173          make test    # 两套后端测试
```

完整说明见 [`AGENTS.md`](./AGENTS.md)。
