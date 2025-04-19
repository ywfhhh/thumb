package com.ywf.thumb.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ywf.thumb.domain.Blog;
import com.ywf.thumb.model.vo.BlogVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
* @author yiwenfeng
* @description 针对表【blog】的数据库操作Service
* @createDate 2025-04-19 17:16:46
*/
public interface BlogService extends IService<Blog> {
    BlogVO getBlogVOById(long blogId, HttpServletRequest request);
    List<BlogVO> getBlogVOList(List<Blog> blogList, HttpServletRequest request);

}
