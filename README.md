# ⚡ 秒杀系统

> 一个练手项目，从零搭建高并发秒杀系统 —— Redis 原子扣库存 + Lua 脚本 + 异步订单 + 三级缓存

刚学 Spring Boot 时做的第一个实战项目，踩了不少坑，记录下完整思路 🚀

---

## 🎯 它做了什么

模拟电商秒杀场景：**10 万人同时抢 100 件商品**，如何保证不超卖、系统不崩？

```
👤 用户点击秒杀
  → 🌸 布隆过滤器（已售罄？直接拒绝）
    → ⚡ Redis Lua 脚本（原子扣库存 + 防重复购买）
      → 📬 订单推入 Redis 队列（异步解耦）
        → 💾 后台消费者慢慢写 MySQL
```

---

## 🧱 技术栈

| 分类 | 用了什么 | 干嘛用 |
|:---:|------|------|
| 🏗️ 框架 | Spring Boot 2.7 + Java 17 | 项目骨架 |
| 🗄️ 数据库 | MySQL 8.0 + MyBatis Plus | 商品和订单持久化 |
| ⚡ 缓存 | Redis | 库存扣减、商品缓存、订单队列 |
| 🔒 分布式锁 | Redisson | 防止订单重复落库 |
| 🌸 布隆过滤器 | Redisson BloomFilter | 拦截已售罄商品请求 |
| 📜 原子操作 | Lua 脚本 | 查库存 + 扣减 + 去重三合一 |
| 📦 序列化 | Fastjson2 | 订单对象序列化 |

---

## 📁 项目结构

```
seckill/
├── controller/     🌐 SeckillController     三个接口：秒杀·商品详情·库存预热
├── service/        ⚙️ SeckillService         秒杀核心逻辑
├── consumer/       📬 OrderConsumer          后台订单消费者（Redis List 队列）
├── config/         🔧 RedisConfig            Redis 序列化配置
│                   🔒 RedissonConfig          Redisson 客户端
│                   🛡️ GlobalExceptionHandler 统一异常处理
├── entity/         📦 SeckillGoods           商品实体
│                   📋 SeckillOrder           订单实体
├── mapper/         🗺️ MyBatis Plus Mapper
├── dto/            📤 Result                 统一返回体
├── resources/
│   ├── lua/seckill.lua    📜 秒杀核心 Lua 脚本
│   └── sql/init.sql       🗃️ 建表 + 测试数据
└── pom.xml
```

---

## ⚡ 核心设计

### 为什么用 Lua 脚本而不是 Java 操作 Redis？

```
❌ 错误做法（Java 三次调用）：
  查库存 → 判断 → 扣减    ← 三个独立操作，并发时会超卖

✅ 正确做法（Lua 一次完成）：
  redis.call('get', 库存)  →  redis.call('decr', 库存)redis.call('get', 库存)  →  redis.call('decr', 库存)
  在 Redis 服务端单线程执行，天然原子性
```

Lua 脚本 `seckill.lua` 一次性完成：**去重检查 → 库存判断 → 扣库存 → 记录用户**，返回 -1/0/1 告诉 Java 端结果。

### 为什么异步下单？

秒杀瞬间 10 万请求，每个都等 MySQL 写入 → 数据库连接池瞬间打满 → 系统雪崩 💥

```
同步：秒杀请求 → 等待MySQL写入（慢）→ 返回
异步：秒杀请求 → 推入Redis队列（极快）→ 返回
              ↓
         后台线程慢慢消费 → 写入MySQL
```

订单消费者 `OrderConsumer` 在后台从 Redis List 里 `leftPop` 阻塞读取，一条一条写入数据库。

### 三级缓存防击穿

```
查商品详情：
  ① 先查 Redis → 命中返回 ✅
  ② 未命中 → setIfAbsent 抢互斥锁 → 只有一个线程查 MySQL② 未命中 → setIfAbsent 抢互斥锁 → 只有一个线程查 MySQL
  ③ 查 MySQL → 回填 Redis（随机TTL防雪崩）③ 查 MySQL → 回填 Redis（随机TTL防雪崩）
```

---

## 🚀 跑起来

### 环境

- ☕ JDK 17
- 🐘 MySQL 8.0
- 🔴 Redis（默认 localhost:6379）

### 步骤

```bash
# 1. 建库建表
mysql -u root -p < src/main/resources/sql/init.sql-u root < src/main/resources/sql/init.sql

# 2. 配 Redis（如果 Redis 不在本地或有密码，复制 application.example.yml → application.yml 改配置）# 2. 配 Redis（如果 Redis 不在本地或有密码，复制 application.example.yml → application.yml 改配置）

# 3. 启动应用
./mvnw spring-boot:run   ．/ mvnw spring-boot:运行

# 4. 预热库存（管理员操作，把商品库存从 MySQL 加载到 Redis）
curl -X POST http://localhost:8080/api/seckill/admin/load/1

# 5. 秒杀！（用不同 userId 模拟多人并发）
curl -X POST http://localhost:8080/api/seckill/1 \
  -H "Content-Type: application/json" \-H "Content-Type: application/json"
  -H "X-User-Id: 1001"   - X-User-Id: 1001"；

# 6. 看商品详情
curl http://localhost:8080/api/seckill/goods/1
```

---

## 🔑 设计要点速查

| 问题 | 怎么解决的 |
|------|-----------|
| 🔴 超卖 | Lua 脚本原子操作：查库存 + 扣减在 Redis 单线程内完成 |
| 🔴 重复购买 | Lua 脚本 `sismember` 检查用户 Set + 数据库联合唯一索引兜底 |
| 🔴 缓存穿透 | Redisson 布隆过滤器，已售罄/不存在 ID 直接拒绝 |
| 🔴 缓存击穿 | `setIfAbsent` 互斥锁，只有一个线程能查库 |
| 🔴 缓存雪崩 | 随机 TTL（30min + random 0~10min），避免集中过期 || 🔴 缓存雪崩 | 随机 TTL（30min   random 0~10min），避免集中过期 || 🔴 缓存雪崩 | 随机 TTL（30min   random 0~10min），避免集中过期 || 🔴 缓存雪崩 | 随机 TTL（30min   random 0~10min），避免集中过期 |
| 🔴 数据库压力 | Redis List 异步队列，削峰填谷 |
| 🔴 订单重复落库 | Redisson 分布式锁（user_id + goods_id 粒度） || 🔴 订单重复落库 | Redisson 分布式锁（user_id   goods_id 粒度） || 🔴 订单重复落库 | Redisson 分布式锁（user_id   goods_id 粒度） || 🔴 订单重复落库 | Redisson 分布式锁（user_id   goods_id 粒度） |

---

## 📝 学到了什么

这个项目让我理解了：

- Redis 不仅是缓存，还能做**原子计数器、消息队列、分布式锁**
- **Lua 脚本**是 Redis 最强大的功能之一，多步操作原子化的利器
- 高并发下的核心思路：**能放 Redis 的绝不打 MySQL，能异步的绝不同步**
- 缓存不是"查不到就查库"那么简单，要防穿透、击穿、雪崩
- 分布式锁要考虑**粒度**（锁什么）、**超时**（防死锁）、**释放**（finally 里释放）

---

、   & lt;
