# Mall 商城项目

基于 **Spring Cloud Alibaba + Vue3** 的前后端分离微服务商城。

## 技术栈

| 层次 | 技术 |
|------|------|
| 后端框架 | JDK8 / Spring Boot 2.7.x / Spring Cloud Alibaba 2021.x |
| 注册/配置中心 | Nacos 2.x |
| API 网关 | Spring Cloud Gateway |
| ORM | MyBatis-Plus 3.5.x |
| 数据库 | MySQL 8 |
| 缓存 | Redis 6 |
| 消息队列 | Kafka（日志）+ RocketMQ（事务消息） |
| 认证 | Spring Security + JWT (jjwt) |
| 前端 | Vue3 + Vite + Element Plus + Pinia |

## 项目结构

```
mall-project/
├── mall-common/      公共模块（Result、异常、JWT工具、Redis工具）
├── mall-gateway/     API 网关（JWT 鉴权、路由转发）
├── mall-auth/        认证服务（注册、登录、JWT 签发）
├── mall-product/     商品服务（商品/分类 CRUD、Redis 缓存、Kafka 日志）
├── mall-cart/        购物车服务（Redis Hash 存储）
├── mall-order/       订单服务（RocketMQ 事务消息、超时取消）
├── mall-logistics/   物流服务（RocketMQ 消费、Kafka 日志）
├── mall-frontend/    Vue3 前端
├── sql/init.sql      数据库初始化脚本
└── pom.xml           父 POM
```

## 端口规划

| 服务 | 端口 |
|------|------|
| mall-gateway | 8080 |
| mall-auth | 8081 |
| mall-product | 8082 |
| mall-cart | 8083 |
| mall-order | 8084 |
| mall-logistics | 8085 |
| Vue3 前端 | 5173 |
| Nacos | 8848 |
| RocketMQ NameServer | 9876 |
| Kafka | 9092 |
| Redis | 6379 |
| MySQL | 3306 |

## 快速启动

### 1. 启动基础设施

```bash
# 启动 Nacos（单机模式）
sh startup.sh -m standalone

# 启动 Redis
redis-server

# 启动 RocketMQ
sh mqnamesrv
sh mqbroker -n localhost:9876

# 启动 Kafka
bin/zookeeper-server-start.sh config/zookeeper.properties
bin/kafka-server-start.sh config/server.properties
```

### 2. 初始化数据库

```bash
mysql -u root -p < sql/init.sql
```

### 3. 启动后端服务（按顺序）

```bash
# 1. 公共模块先 install
cd mall-common && mvn install

# 2. 依次启动各服务
java -jar mall-gateway/target/mall-gateway-1.0.0.jar
java -jar mall-auth/target/mall-auth-1.0.0.jar
java -jar mall-product/target/mall-product-1.0.0.jar
java -jar mall-cart/target/mall-cart-1.0.0.jar
java -jar mall-order/target/mall-order-1.0.0.jar
java -jar mall-logistics/target/mall-logistics-1.0.0.jar
```

### 4. 启动前端

```bash
cd mall-frontend
npm install
npm run dev
```

访问 http://localhost:5173

## 核心设计说明

### 购物车（Redis Hash）
- key = `cart:{userId}`，field = `{productId}`，value = CartItem JSON
- 无 MySQL 持久化，30 天过期

### 订单（RocketMQ 事务消息）
- 发送半消息 → 执行本地事务（写 DB）→ 提交/回滚消息
- 保证订单创建与物流通知的最终一致性

### 库存扣减（双重保障）
- Redis 分布式锁：防止并发请求同时进入 DB
- SQL 乐观锁：`WHERE stock >= quantity`，最终兜底防超卖

### 订单超时取消（Redis ZSet 延迟队列）
- 创建订单时写入 ZSet，score = 超时时间戳
- 定时任务每分钟扫描 score <= now 的订单并取消
