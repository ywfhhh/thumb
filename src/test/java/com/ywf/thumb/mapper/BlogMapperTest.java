package com.ywf.thumb.mapper;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BlogMapperTest {
    @Resource
    BlogMapper blogMapper;

    @Test
    void test() {
        HashMap<Long, Long> map = new HashMap<>();
        map.put(1L, 1L);
        blogMapper.batchUpdateThumbCount(map);
    }
}