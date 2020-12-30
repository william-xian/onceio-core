package top.onceio.core.db.dao;

import java.util.List;

import top.onceio.core.util.OUtils;

/**
 * 当page非正数时，会返回total; 当page为null或者0时，返回第一页数据 ，当page为负数时，返回第page的绝对值也页。
 **/
public class Page<T> {
    Integer page;
    Integer pageSize;
    Long total;
    List<T> data;

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return OUtils.toJson(this);
    }

}
