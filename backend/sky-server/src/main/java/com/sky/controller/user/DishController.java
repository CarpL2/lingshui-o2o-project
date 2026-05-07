package com.sky.controller.user;

import com.alibaba.fastjson.JSON;
import com.sky.constant.StatusConstant;
import com.sky.entity.Dish;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController("userDishController")
@RequestMapping("/user/dish")
@Slf4j
@Api(tags = "C端-菜品浏览接口")
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 根据分类id查询菜品
     *
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<DishVO>> list(Long categoryId) {

        // 1. 构造 Redis key
        String key = "dish_" + categoryId;

        // 2. 查询 Redis (拿到的是 JSON 字符串)
        String json = stringRedisTemplate.opsForValue().get(key);

        // 防穿透：如果查到的是空对象标记（我们约定 "" 代表空）
        if ("".equals(json)) {
            // 直接返回空列表，不查库
            return Result.success(new ArrayList<>());
        }

        // 3. 如果存在正常数据
        if (StringUtils.hasLength(json)) {
            // 将 JSON 转回 List 对象
            List<DishVO> list = JSON.parseArray(json, DishVO.class);
            return Result.success(list);
        }

        // 4. Redis 不存在，查询数据库
        Dish dish = new Dish();
        dish.setCategoryId(categoryId);
        dish.setStatus(StatusConstant.ENABLE);
        List<DishVO> list = dishService.listWithFlavor(dish);

        // 5. 判断数据库查询结果
        if (list != null && !list.isEmpty()) {
            // 防雪崩设置过期时间 = 固定值 + 随机值
            // 比如：基础 30 分钟 + 随机 0~5 分钟
            int randomTime = (int) (Math.random() * 5);

            stringRedisTemplate.opsForValue().set(
                    key,
                    JSON.toJSONString(list), // 转成 JSON 存
                    30 + randomTime,         // 时间
                    TimeUnit.MINUTES         // 单位
            );
        } else {
            // 防穿透数据库也没查到
            // 存一个空字符串，过期时间设置短一点（比如 2 分钟）
            stringRedisTemplate.opsForValue().set(key, "", 2, TimeUnit.MINUTES);
        }

        return Result.success(list);
    }

}
