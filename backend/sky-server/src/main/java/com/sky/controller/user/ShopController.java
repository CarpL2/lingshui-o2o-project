package com.sky.controller.user;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController("userShopController")
@RequestMapping("/user/shop")
@Slf4j
@Api(tags = "店铺相关接口")
public class ShopController {

    @Autowired
    private RedisTemplate redisTemplate;

    private static final String SHOP_STATUS = "SHOP_STATUS";

    @GetMapping("/status")
    @ApiOperation("获取店铺营业状态")
    public Result<Integer> getStatus() {
        Integer status = (Integer) redisTemplate.opsForValue().get(SHOP_STATUS);
        log.info("获取到的店铺营业状态：{}", status == 1 ? "营业中" : "打烊中");
        return Result.success(status);
    }

    /**
     * 获取店铺详情 (补丁接口)
     * 前端请求: GET /user/shop/getMerchantInfo
     */
    @GetMapping("/getMerchantInfo")
    @ApiOperation("获取店铺详情")
    public Result<Map<String, Object>> getMerchantInfo() {
        log.info("C端请求获取店铺详情...");
        Map<String, Object> map = new HashMap<>();

        // 1. 对应前端 {{ shopInfo().shopAddress }}
        map.put("shopAddress", "辽宁省大连市甘井子区凌工路2号");

        // 2. 对应前端 {{ deliveryFee() }}
        // 我们把可能的变量名都传回去，不管前端取哪个都能拿到值
        map.put("deliveryPrice", 5);  // 最常用
        map.put("deliveryFee", 5);    // 备用
        map.put("packAmount", 0);     // 备用（打包费）

        // 3. 其他基础信息
        map.put("minMoney", 0);       // 起送费
        map.put("open", true);        // 营业状态

        return Result.success(map);
    }

}
