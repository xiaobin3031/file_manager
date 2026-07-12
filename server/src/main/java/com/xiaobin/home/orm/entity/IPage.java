package com.xiaobin.home.orm.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

public class IPage<T> {

    @Getter
    @Setter
    private List<T> records = new ArrayList<>();

    private final int total;

    private final int page;

    private final int limit;

    public IPage(int total, int page, int limit) {
        this.total = total;
        this.page = page;
        this.limit = limit;
    }

    public boolean hasMore() {
        return this.limit > 0 && this.total > 0
                && (int) (Math.ceil(this.total * 1f / this.limit)) > this.page;
    }

}
