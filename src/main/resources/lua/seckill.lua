-- KEYS[1]  seckill:stock:{goodsId}
-- KEYS[2]  seckill:user:{goodsId}
-- ARGV[1]  userId

local exists = redis.call('sismember', KEYS[2], ARGV[1])
if exists == 1 then
    return -1
end

local stock = tonumber(redis.call('get', KEYS[1]) or '0')
if stock <= 0 then
    return 0
end

redis.call('decr', KEYS[1])
redis.call('sadd', KEYS[2], ARGV[1])
return 1
