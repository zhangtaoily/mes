package com.xinao.mes.common.result;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页返回体
 */
@Data
public class PageResult<T> implements Serializable {

    /** 总条数 */
    private long total;
    /** 当前页数据 */
    private List<T> rows;

    public static <T> PageResult<T> of(long total, List<T> rows) {
        PageResult<T> p = new PageResult<>();
        p.setTotal(total);
        p.setRows(rows);
        return p;
    }
}
