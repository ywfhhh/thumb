package com.ywf.thumb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ywf.thumb.domain.Blog;
import org.apache.ibatis.annotations.Param;
import java.util.Map;

/**
 * @author yiwenfeng
 * @description 针对表【blog】的数据库操作Mapper
 * @createDate 2025-04-19 17:16:46
 * @Entity generator.domain.Blog
 */
public interface BlogMapper extends BaseMapper<Blog> {
    void batchUpdateThumbCount(@Param("countMap") Map<Long, Long> countMap);

}




