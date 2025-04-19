package com.ywf.thumb.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ywf.thumb.domain.User;
import jakarta.servlet.http.HttpServletRequest;

/**
* @author yiwenfeng
* @description 针对表【user】的数据库操作Service
* @createDate 2025-04-19 17:17:03
*/
public interface UserService extends IService<User> {

    User getLoginUser(HttpServletRequest request);
}
