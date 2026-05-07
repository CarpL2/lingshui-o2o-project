-- seckill.lua

-- 1. 参数列表
--  优惠券(秒杀商品)库存的 key
local stockKey = KEYS[1]
--  订单(用户购买记录)的 key
local orderKey = KEYS[2]

--  脚本参数：优惠券/商品 ID
local voucherId = ARGV[1]
--  脚本参数：用户 ID
local userId = ARGV[2]

-- 2. 数据校验
--  判断库存是否充足 (get stockKey)
-- tonumber将字符串转为数字
if (tonumber(redis.call('get', stockKey) or 0) <= 0) then
    --  库存不足，返回 1
    return 1
end

--  判断用户是否下单 (sismember orderKey userId)
if (redis.call('sismember', orderKey, userId) == 1) then
    --  存在重复下单，返回 2
    return 2
end

-- 3. 扣减库存 (incrby stockKey -1)
redis.call('incrby', stockKey, -1)
-- 4. 下单（保存用户） (sadd orderKey userId)
redis.call('sadd', orderKey, userId)

-- 5. 抢购成功，返回 0
return 0