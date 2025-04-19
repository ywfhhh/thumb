package com.ywf.thumb.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ywf.thumb.common.ErrorCode;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Slf4j
@RequiredArgsConstructor
public class ThumbServiceImpl extends ServiceImpl<ThumbMapper, Thumb> implements ThumbService {

    private final UserService userService;

    private final BlogService blogService;

    private final TransactionTemplate transactionTemplate;

    @Override
    public Boolean doThumb(DoThumbRequest doThumbRequest, HttpServletRequest request) {
        if (doThumbRequest == null || doThumbRequest.getBlogId() == null) {
            throw new RuntimeException("参数错误");
        }
        User loginUser = userService.getLoginUser(request);
        // 加锁
        //必须让锁的作用域完全包裹事务操作，保证其他线程获取锁时，前序事务必然已完成提交。
        // 即：先获取锁 → 开启事务 → 执行业务逻辑 → 提交事务 → 释放锁，保证点赞操作的原子性和一致性及事务的完整性不被其他线程干扰。
        synchronized (loginUser.getId().toString().intern()) {

            // 编程式事务
            return transactionTemplate.execute(status -> {
                Long blogId = doThumbRequest.getBlogId();
                boolean exists = this.lambdaQuery()
                        .eq(Thumb::getUserId, loginUser.getId())
                        .eq(Thumb::getBlogId, blogId)
                        .exists();
                if (exists) {
                    throw new ThumbException(ErrorCode.OPERATION_ERROR, "用户已点赞");
                }

                boolean update = blogService.lambdaUpdate()
                        .eq(Blog::getId, blogId)
                        .setSql("thumbCount = thumbCount + 1")
                        .update();

                Thumb thumb = new Thumb();
                thumb.setUserId(loginUser.getId());
                thumb.setBlogId(blogId);
                // 更新成功才执行
                return update && this.save(thumb);
            });
        }
    }

    @Override
    public Boolean undoThumb(DoThumbRequest doThumbRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        // 编程式事务
        return transactionTemplate.execute(status -> {
            Long blogId = doThumbRequest.getBlogId();

            Thumb thumb = this.lambdaQuery().eq(Thumb::getUserId, loginUser.getId()).eq(Thumb::getBlogId, blogId).one();
            if (ObjectUtil.isNull(thumb)) {
                throw new ThumbException(ErrorCode.OPERATION_ERROR, "用户未点赞");
            }

            boolean update = blogService.lambdaUpdate()
                    .eq(Blog::getId, blogId)
                    .setSql("thumbCount = thumbCount - 1")
                    .update();

            // 更新成功才执行
            return update && this.removeById(thumb.getId());
        });
    }
}
