package com.xiaobin.home.service;

import lombok.Setter;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * 分页查询服务
 * @param <T> 数据类型
 */
@Setter
public class PageQueryService<T> {

    private BiFunction<Long, Integer, List<T>> mapper;
    private BiFunction<T, Long, Long> idMapper;
    private Consumer<T> queryEnd;
    private int size = 1000;

    public PageQueryService() {}

    public PageQueryService(int size) {
        this.size = size;
    }

    public void query() {
        assert mapper != null;
        assert idMapper != null;

        long minId = 0;
        while(true) {
            List<T> list = mapper.apply(minId, size);
            if(list.isEmpty()) {
                break;
            }

            for (T t : list) {
                minId = idMapper.apply(t, minId);
            }

            if(list.size() < size) {
                break;
            }
        }
        if(queryEnd != null) {
            this.queryEnd.accept(null);
        }
    }
}
