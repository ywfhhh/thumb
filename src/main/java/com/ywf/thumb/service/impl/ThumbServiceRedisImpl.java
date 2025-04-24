package com.ywf.thumb.service.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ywf.thumb.common.ErrorCode;
import com.ywf.thumb.constant.RedisLuaScriptConstant;
import com.ywf.thumb.constant.ThumbConstant;
import com.ywf.thumb.controller.UserController;
import com.ywf.thumb.domain.Blog;
import com.ywf.thumb.domain.Thumb;
import com.ywf.thumb.domain.User;
import com.ywf.thumb.exception.ThumbException;
import com.ywf.thumb.localcache.MultiLevelCacheManager;
import com.ywf.thumb.mapper.ThumbMapper;
import com.ywf.thumb.model.dto.DoThumbRequest;
import com.ywf.thumb.model.enums.LuaStatusEnum;
import com.ywf.thumb.service.BlogService;
import com.ywf.thumb.service.ThumbService;
import com.ywf.thumb.service.UserService;
import com.ywf.thumb.utils.RedisKeyUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service("thumbService")
@Slf4j
@RequiredArgsConstructor
public class ThumbServiceRedisImpl extends ServiceImpl<ThumbMapper, Thumb> implements ThumbService {

    private final UserService userService;

    private final RedisTemplate<String, Object> redisTemplate;

    // 引入缓存管理
    private final MultiLevelCacheManager multiLevelCacheManager;

    @Override
    public Boolean hasThumb(Long blogId, Long userId) {
        Object object = multiLevelCacheManager.get(ThumbConstant.USER_THUMB_KEY_PREFIX + userId, blogId.toString());
        if (object == null) {
            return false;
        }
        Long thumbId = (long) object;
        return !thumbId.equals(ThumbConstant.UN_THUMB_CONSTANT);
    }


    @Override
    public Boolean doThumb(DoThumbRequest doThumbRequest, HttpServletRequest request) {
        if (doThumbRequest == null || doThumbRequest.getBlogId() == null) {
            throw new RuntimeException("参数错误");
        }
        User loginUser = userService.getLoginUser(request);
        Long blogId = doThumbRequest.getBlogId();

        String timeSlice = getTimeSlice();
        // Redis Key
        String tempThumbKey = RedisKeyUtil.getTempThumbKey(timeSlice);
        String userThumbKey = RedisKeyUtil.getUserThumbKey(loginUser.getId());

        // 执行 Lua 脚本
        long result = redisTemplate.execute(
                RedisLuaScriptConstant.THUMB_SCRIPT,
                Arrays.asList(tempThumbKey, userThumbKey),
                loginUser.getId(),
                blogId
        );

        if (LuaStatusEnum.FAIL.getValue() == result) {
            throw new ThumbException(ErrorCode.OPERATION_ERROR,"用户已点过赞!");
        }
        boolean success = LuaStatusEnum.SUCCESS.getValue() == result;
        if (success) {
            String hashKey = ThumbConstant.USER_THUMB_KEY_PREFIX + loginUser.getId();
            String fieldKey = blogId.toString();
            Long tempThumbId = RandomUtil.randomLong(1, Long.MAX_VALUE);// 提前生成非0的thumbId用于占位,是否点赞于该字段无关
            multiLevelCacheManager.putIfPresent(hashKey, fieldKey, tempThumbId);
        }
        // 更新成功才执行
        return success;
    }

    @Override
    public Boolean undoThumb(DoThumbRequest doThumbRequest, HttpServletRequest request) {
        if (doThumbRequest == null || doThumbRequest.getBlogId() == null) {
            throw new RuntimeException("参数错误");
        }
        User loginUser = userService.getLoginUser(request);

        Long blogId = doThumbRequest.getBlogId();
        // 计算时间片
        String timeSlice = getTimeSlice();
        // Redis Key
        String tempThumbKey = RedisKeyUtil.getTempThumbKey(timeSlice);
        String userThumbKey = RedisKeyUtil.getUserThumbKey(loginUser.getId());

        // 执行 Lua 脚本
        long result = redisTemplate.execute(
                RedisLuaScriptConstant.UNTHUMB_SCRIPT,
                Arrays.asList(tempThumbKey, userThumbKey),
                loginUser.getId(),
                blogId
        );
        // 根据返回值处理结果
        if (result == LuaStatusEnum.FAIL.getValue()) {
            throw new RuntimeException("用户未点赞");
        }
        boolean success = LuaStatusEnum.SUCCESS.getValue() == result;
        // 点赞记录存入 Redis
        if (success) {
            String hashKey = ThumbConstant.USER_THUMB_KEY_PREFIX + loginUser.getId();
            String fieldKey = blogId.toString();
            multiLevelCacheManager.putIfPresent(hashKey, fieldKey, ThumbConstant.UN_THUMB_CONSTANT);// 置为0表示未点赞
        }
        return success;
    }

    private String getTimeSlice() {
        DateTime nowDate = DateUtil.date();
        // 获取到当前时间前最近的整数秒，比如当前 11:20:23 ，获取到 11:20:20
        return DateUtil.format(nowDate, "HH:mm:") + (DateUtil.second(nowDate) / 10) * 10;
    }
}


