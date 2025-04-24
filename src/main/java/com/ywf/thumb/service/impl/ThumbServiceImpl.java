package com.ywf.thumb.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ywf.thumb.common.ErrorCode;
import com.ywf.thumb.constant.ThumbConstant;
import com.ywf.thumb.controller.UserController;
import com.ywf.thumb.domain.Blog;
import com.ywf.thumb.domain.Thumb;
import com.ywf.thumb.domain.User;
import com.ywf.thumb.exception.ThumbException;
import com.ywf.thumb.mapper.ThumbMapper;
import com.ywf.thumb.model.dto.DoThumbRequest;
import com.ywf.thumb.service.BlogService;
import com.ywf.thumb.service.ThumbService;
import com.ywf.thumb.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.TimeUnit;

@Service("thumbServiceDB")
@Slf4j
@RequiredArgsConstructor // 自动生成构造函数进行依赖注入
public class ThumbServiceImpl extends ServiceImpl<ThumbMapper, Thumb> implements ThumbService {

    private final UserService userService;

    private final BlogService blogService;

    private final TransactionTemplate transactionTemplate;

    private final RedissonClient redissonClient;

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Boolean doThumb(DoThumbRequest doThumbRequest, HttpServletRequest request) {
        if (doThumbRequest == null || doThumbRequest.getBlogId() == null) {
            throw new RuntimeException("参数错误");
        }
        User loginUser = userService.getLoginUser(request);
        // 加锁
        // 必须让锁的作用域完全包裹事务操作，保证其他线程获取锁时，前序事务必然已完成提交。
        // 即：先获取锁 → 开启事务 → 执行业务逻辑 → 提交事务 → 释放锁，保证点赞操作的原子性和一致性及事务的完整性不被其他线程干扰。
        String key = String.format("thumb:blog:%d:%d", loginUser.getId(), doThumbRequest.getBlogId());
        RLock lock = redissonClient.getLock(key);
        boolean locked = false;
        try {
            // 不使用无参函数 看门狗续期机制， 等待最多 5 秒去拿锁，拿到锁后 30 秒自动解锁（防止死锁）
            locked = lock.tryLock(5, 30, TimeUnit.SECONDS);
            if (!locked) {
                throw new ThumbException(ErrorCode.OPERATION_ERROR, "操作频繁!");
            }
            return transactionTemplate.execute(status -> {
                Long blogId = doThumbRequest.getBlogId();
                Boolean exists = this.hasThumb(loginUser.getId(), blogId);
                if (exists)
                    throw new ThumbException(ErrorCode.OPERATION_ERROR, "已点过赞!");
                boolean update = blogService.lambdaUpdate().eq(Blog::getId, blogId).setSql("thumbCount = thumbCount + 1").update();
                if (!update) {
                    throw new ThumbException(ErrorCode.OPERATION_ERROR, "更新点赞数失败!");
                }
                Thumb thumb = new Thumb();
                thumb.setUserId(loginUser.getId());
                thumb.setBlogId(blogId);
                boolean success = this.save(thumb);
                if (success)
                    redisTemplate.opsForHash().put(ThumbConstant.USER_THUMB_KEY_PREFIX + loginUser.getId().toString(), blogId.toString(), thumb.getId());
                return success;
            });
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public Boolean undoThumb(DoThumbRequest doThumbRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        String key = String.format("thumb:blog:%d:%d", loginUser.getId(), doThumbRequest.getBlogId());
        RLock lock = redissonClient.getLock(key);
        boolean locked = false;
        try {
            locked = lock.tryLock(5, TimeUnit.SECONDS);
            if (!locked) {
                throw new ThumbException(ErrorCode.OPERATION_ERROR, "获取点赞分布式锁失败!");
            }
            return transactionTemplate.execute(status -> {
                Long blogId = doThumbRequest.getBlogId();
                Long thumbId = ((Long) redisTemplate.opsForHash().get(ThumbConstant.USER_THUMB_KEY_PREFIX + loginUser.getId().toString(), blogId.toString()));
                if (thumbId == null) {
                    throw new ThumbException(ErrorCode.OPERATION_ERROR, "用户未点赞");
                }
                // 更新数据库
                boolean update = blogService.lambdaUpdate().eq(Blog::getId, blogId).setSql("thumbCount = thumbCount - 1").update();
                if (!update) {
                    throw new ThumbException(ErrorCode.OPERATION_ERROR, "更新点赞数失败!");
                }
                // 更新数据库
                boolean success = this.removeById(thumbId);
                // 更新缓存
                if (success)
                    redisTemplate.opsForHash().delete(ThumbConstant.USER_THUMB_KEY_PREFIX + loginUser.getId().toString(), blogId.toString());
                return success;
            });
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }


    @Override
    public Boolean hasThumb(Long userId, Long blogId) {
        return redisTemplate.opsForHash().hasKey(ThumbConstant.USER_THUMB_KEY_PREFIX + userId.toString(), blogId.toString());
    }

}
