package com.sky.controller.user;

import com.alibaba.fastjson.JSON;
import com.sky.constant.StatusConstant;
import com.sky.entity.Setmeal;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController("userSetmealController")
@RequestMapping("/user/setmeal")
@Api(tags = "C端-套餐浏览接口")
public class SetmealController {
    @Autowired
    private SetmealService setmealService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 条件查询
     *
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询套餐")
    public Result<List<Setmeal>> list(Long categoryId) {
        // 1. 构造 Redis Key (保持和 Admin 端清理逻辑一致：setmeal_*)
        String key = "setmeal_" + categoryId;

        // 2. 查询 Redis
        String json = stringRedisTemplate.opsForValue().get(key);

        // 防穿透如果是空对象标记
        if ("".equals(json)) {
            return Result.success(new ArrayList<>());
        }

        // 3. 命中缓存，直接返回
        if (StringUtils.hasLength(json)) {
            List<Setmeal> list = JSON.parseArray(json, Setmeal.class);
            return Result.success(list);
        }

        // 4. 未命中，查询数据库
        Setmeal setmeal = new Setmeal();
        setmeal.setCategoryId(categoryId);
        setmeal.setStatus(StatusConstant.ENABLE);
        List<Setmeal> list = setmealService.list(setmeal);

        // 5. 回写 Redis
        if (list != null && !list.isEmpty()) {
            // 防雪崩设置随机过期时间 (30~35分钟)
            int randomTime = (int) (Math.random() * 5);
            stringRedisTemplate.opsForValue().set(
                    key,
                    JSON.toJSONString(list),
                    30 + randomTime,
                    TimeUnit.MINUTES
            );
        } else {
            // 防穿透：存入空字符串，短时间过期
            stringRedisTemplate.opsForValue().set(key, "", 2, TimeUnit.MINUTES);
        }

        return Result.success(list);
    }

    /**
     * 根据套餐id查询包含的菜品列表
     *
     * @param id
     * @return
     */
    @GetMapping("/dish/{id}")
    @ApiOperation("根据套餐id查询包含的菜品列表")
    public Result<List<DishItemVO>> dishList(@PathVariable("id") Long id) {
        List<DishItemVO> list = setmealService.getDishItemById(id);
        return Result.success(list);
    }

}
