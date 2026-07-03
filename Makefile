# study-enterprise 启动/管理入口
# 用法:make <目标>   例:make up · make java · make kotlin · make web · make test
# 端口(均 10000+,减少与其它服务冲突):
#   Java 18080 · Kotlin 18081 · MySQL 13306 · MinIO 19100/19101 · Redis 16379 · 前端 15173
# 端口被占用时用环境变量前缀覆盖(会自动传给应用),例:
#   SERVER_PORT=18090 REDIS_PORT=16380 make java

.DEFAULT_GOAL := help
SHELL := /bin/bash

.PHONY: help up down ps logs status java kotlin install web test test-java test-kotlin build clean reset-db

help: ## 显示所有目标
	@grep -hE '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN{FS=":.*?## "}{printf "  \033[36m%-13s\033[0m %s\n", $$1, $$2}'

# ---- 基础设施(Docker)----
up: ## 起 MySQL + MinIO + Redis(首次自动复制 .env)
	@test -f .env || cp .env.example .env
	docker compose up -d
	docker compose ps

down: ## 停止并移除基础设施容器
	docker compose down

ps: ## 查看容器状态
	docker compose ps

logs: ## 跟踪基础设施日志
	docker compose logs -f

status: ## 查看基础设施 + 各服务运行状态
	@echo "== 基础设施(Docker)=="
	@docker compose ps 2>/dev/null || true
	@echo "== 应用(默认端口)=="
	@c=$$(curl -s -o /dev/null -w "%{http_code}" --max-time 2 http://localhost:18080/actuator/health); [ "$$c" = 000 ] && echo "  ⛔ Java 后端    :18080  未运行" || echo "  ✅ Java 后端    :18080  运行中 (HTTP $$c)"
	@c=$$(curl -s -o /dev/null -w "%{http_code}" --max-time 2 http://localhost:18081/actuator/health); [ "$$c" = 000 ] && echo "  ⛔ Kotlin 后端  :18081  未运行" || echo "  ✅ Kotlin 后端  :18081  运行中 (HTTP $$c)"
	@c=$$(curl -s -o /dev/null -w "%{http_code}" --max-time 2 http://localhost:15173/); [ "$$c" = 000 ] && echo "  ⛔ 前端         :15173  未运行" || echo "  ✅ 前端         :15173  运行中 (HTTP $$c)"
	@c=$$(curl -s -o /dev/null -w "%{http_code}" --max-time 2 http://localhost:19101/); [ "$$c" = 000 ] && echo "  ⛔ MinIO 控制台 :19101  未运行" || echo "  ✅ MinIO 控制台 :19101  运行中 (HTTP $$c)"

# ---- 后端 ----
java: ## 起 Java 后端(:18080,Maven)
	cd java-backend && ./mvnw spring-boot:run

kotlin: ## 起 Kotlin 后端(:18081,Gradle)
	cd kotlin-backend && ./gradlew bootRun

# ---- 前端 ----
install: ## 安装前端依赖
	cd frontend && pnpm install

web: install ## 起前端 dev 服务(:15173)
	cd frontend && pnpm dev

# ---- 测试 / 构建 ----
test: test-java test-kotlin ## 跑两套后端测试

test-java: ## Java 后端测试(Testcontainers)
	cd java-backend && ./mvnw test

test-kotlin: ## Kotlin 后端测试(Testcontainers)
	cd kotlin-backend && ./gradlew test

build: ## 前端类型检查 + 打包
	cd frontend && pnpm install && pnpm build

# ---- 清理 / 维护 ----
clean: ## 清理各端构建产物
	-cd java-backend && ./mvnw -q clean
	-cd kotlin-backend && ./gradlew -q clean
	rm -rf frontend/dist

reset-db: ## 清空两套 schema(危险:会删数据)
	docker compose exec mysql mysql -uroot -p$${MYSQL_ROOT_PASSWORD:-rootpass} -e "\
DROP DATABASE IF EXISTS study_java;   CREATE DATABASE study_java   CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; \
DROP DATABASE IF EXISTS study_kotlin; CREATE DATABASE study_kotlin CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
