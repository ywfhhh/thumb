package com.ywf.thumb.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ywf.thumb.constant.UserConstant;
import com.ywf.thumb.controller.UserController;
import com.ywf.thumb.domain.User;
import com.ywf.thumb.mapper.UserMapper;
import com.ywf.thumb.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

/**
 * @author yiwenfeng
 * @description 针对表【user】的数据库操作Service实现
 * @createDate 2025-04-19 17:17:03
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {
    @Override
    public User getLoginUser(HttpServletRequest request) {
        User loginUser = (User) request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        return loginUser;
    }
}




