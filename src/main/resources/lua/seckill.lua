-- ============================================================
-- 秒杀 Lua 脚本 —— 为什么用 Lua？
-- Redis 的每个操作都是原子的，但"查库存 → 判断 → 扣减"是 3 个
-- 独立操作。如果放在 Java 里分三次调用 Redis，并发时会出现：
--   用户A 读到库存=1，用户B 也读到库存=1
--   两人都判断有库存，都执行扣减 → 超卖
-- Lua 脚本在 Redis 服务端单线程执行，整个脚本是一个原子操作，
-- Java 里一次 execute() 即可，不会被其他命令插队
-- ============================================================

-- KEYS[1]  seckill:stock:{goodsId}  库存值（String类型）
-- KEYS[2]  seckill:user:{goodsId}   已购用户集合（Set类型，防重复购买）
-- ARGV[1]  userId                   当前用户ID

-- 第一步：检查用户是否已经买过（Set 去重）
-- sismember 返回 1=已存在  0=不存在
local exists = redis.call('sismember', KEYS[2], ARGV[1])
if exists == 1 then
    -- 返回值 -1：Java 端提示"您已参与过该秒杀"
    return -1
end

-- 第二步：检查库存是否充足
-- tonumber 把字符串转成数字，key 不存在时返回 0
local stock = tonumber(redis.call('get', KEYS[1]) or '0')
if stock <= 0 then
    -- 返回值 0：Java 端提示"库存不足"并更新布隆过滤器
    return 0
end

-- 第三步：扣减库存 + 记录用户
-- decr 是原子操作，把库存值减 1
redis.call('decr', KEYS[1])
-- sadd 把 userId 加入已购集合，防止重复购买
redis.call('sadd', KEYS[2], ARGV[1])

-- 返回值 1：Java 端判断成功，推入订单队列
return 1
