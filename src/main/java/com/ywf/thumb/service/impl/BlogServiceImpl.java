package com.ywf.thumb.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ywf.thumb.constant.ThumbConstant;
import com.ywf.thumb.domain.Blog;
import com.ywf.thumb.domain.Thumb;
import com.ywf.thumb.domain.User;
import com.ywf.thumb.mapper.BlogMapper;
import com.ywf.thumb.model.vo.BlogVO;
import com.ywf.thumb.service.BlogService;
import com.ywf.thumb.service.ThumbService;
import com.ywf.thumb.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jdk.jfr.Label;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author yiwenfeng
 * @description 针对表【blog】的数据库操作Service实现
 * @createDate 2025-04-19 17:16:46
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog>
        implements BlogService {
    @Resource
    private UserService userService;
    @Lazy// 解决循环引用问题
    @Resource
    private ThumbService thumbService;
    @Resource
    private RedisTemplate redisTemplate;

    @Override
    public BlogVO getBlogVOById(long blogId, HttpServletRequest request) {
        Blog blog = this.getById(blogId);
        User loginUser = userService.getLoginUser(request);
        return this.getBlogVO(blog, loginUser);
    }

    @Override
    public List<BlogVO> getBlogVOList(List<Blog> blogList, HttpServletRequest request) {
        // 批量加载到点赞信息到内存判断，将减少网络IO
        User loginUser = userService.getLoginUser(request);
        HashMap<Long, Boolean> hasThumbed = new HashMap<>();
        if (loginUser != null) {
            List<Object> blogIds = blogList.stream().map(blog -> blog.getId().toString()).collect(Collectors.toList());
            List<Object> thumbIds = redisTemplate.opsForHash().multiGet(ThumbConstant.USER_THUMB_KEY_PREFIX + loginUser.getId().toString(), blogIds);
            for (int i = 0; i < thumbIds.size(); i++) {
                if (thumbIds.get(i) == null) {
                    continue;
                }
                hasThumbed.put(Long.valueOf(blogIds.get(i).toString()), true);
            }
        }
        return blogList.stream()
                .map(blog -> {
                    BlogVO blogVO = BeanUtil.copyProperties(blog, BlogVO.class);
                    blogVO.setHasThumb(hasThumbed.get(blog.getId()));
                    return blogVO;
                })
                .toList();
    }


    private BlogVO getBlogVO(Blog blog, User loginUser) {
        BlogVO blogVO = new BlogVO();
        BeanUtil.copyProperties(blog, blogVO);

        if (loginUser == null) {
            return blogVO;
        }

        blogVO.setHasThumb(thumbService.hasThumb(loginUser.getId(), blog.getId()));
        return blogVO;
    }
}




