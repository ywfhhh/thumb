package com.ywf.thumb.localcache;

import com.ywf.thumb.localcache.Item;

import java.util.List;
import java.util.concurrent.BlockingQueue;

import java.util.List;
import java.util.concurrent.BlockingQueue;

public interface TopK {
    AddResult add(String key, int increment);
    List<Item> list();
    BlockingQueue<Item> expelled();
    void fading();
    long total();
}
