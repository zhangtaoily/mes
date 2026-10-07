package com.xinao.mes.engine.meta.vo;

import lombok.Data;

/**
 * 动态渲染列（列表列 + 查询项）
 */
@Data
public class ColumnVO {

    private String columnCode;

    private String columnLabel;

    /** string/number/date */
    private String dataType;

    /** LIST/QUERY/BOTH */
    private String columnUsage;

    /** eq/like/between/ge/le */
    private String queryType;

    private String dictType;

    private Boolean sortable;

    private Integer width;

    /** true=动态列（如质检指标纵表展开） */
    private Boolean dynamic;

    public ColumnVO() {
    }

    public ColumnVO(String columnCode, String columnLabel, String dataType, String columnUsage,
                    String queryType, String dictType, Boolean sortable, Integer width, Boolean dynamic) {
        this.columnCode = columnCode;
        this.columnLabel = columnLabel;
        this.dataType = dataType;
        this.columnUsage = columnUsage;
        this.queryType = queryType;
        this.dictType = dictType;
        this.sortable = sortable;
        this.width = width;
        this.dynamic = dynamic;
    }
}
