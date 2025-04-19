package com.ywf.thumb.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ywf.thumb.domain.Thumb;
import com.ywf.thumb.model.dto.DoThumbRequest;
import jakarta.servlet.http.HttpServletRequest;

/**
* @author yiwenfeng
* @description 针对表【thumb】的数据库操作Service
* @createDate 2025-04-19 17:16:55
*/
public interface ThumbService extends IService<Thumb> {
    /**
     * 点赞
     * @param doThumbRequest
     * @param request
     * @return {@link Boolean }
     */
    Boolean doThumb(DoThumbRequest doThumbRequest, HttpServletRequest request);
    /**
     * 取消点赞
     * @param doThumbRequest
     * @param request
     * @return {@link Boolean }
     */
    Boolean undoThumb(DoThumbRequest doThumbRequest, HttpServletRequest request);

}
