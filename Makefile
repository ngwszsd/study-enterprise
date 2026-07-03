# study-enterprise 启动/管理入口
# 用法:make <目标>   例:make up · make java · make kotlin · make web · make test
# 端口:Java 8080 · Kotlin 8081 · MySQL 3306 · MinIO 9000/9001 · Redis 6379 · 前端 5173
# 端口被占用时用环境变量前缀覆盖(会自动传给应用),例:
#   SERVER_PORT=8090 REDIS_PORT=6380 make java
#   SERVER_PORT=8091 REDIS_PORT=6380 make kotlin

.DEFAULT_GOAL := help
SHELL := /bin/bash

.PHONY: help up down ps logs java kotlin install web test test-java test-kotlin build clean reset-db

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

# ---- 后端 ----
java: ## 起 Java 后端(:8080,Maven)
	cd java-backend && ./mvnw spring-boot:run

kotlin: ## 起 Kotlin 后端(:8081,Gradle)
	cd kotlin-backend && ./gradlew bootRun

# ---- 前端 ----
install: ## 安装前端依赖
	cd frontend && pnpm install

web: install ## 起前端 dev 服务(:5173)
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
