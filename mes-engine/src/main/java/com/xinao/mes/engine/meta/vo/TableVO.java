package com.xinao.mes.engine.meta.vo;

import lombok.Data;

import java.util.List;

/**
 * 表节点元数据（前端列表页/查询项动态渲染契约）
 */
@Data
public class TableVO {

    private String nodeCode;

    private String nodeName;

    private String tableName;

    private List<ColumnVO> columns;
}
