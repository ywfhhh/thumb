package com.ywf.thumb.model.enums;

import lombok.Getter;
import lombok.Setter;

@Getter
public enum ThumbTypeEnum {
    // 点赞
    INCR(1),
    DECR(-1),
    NON(0);
    private final int value;

    ThumbTypeEnum(int value) {
        this.value = value;
    }
}
