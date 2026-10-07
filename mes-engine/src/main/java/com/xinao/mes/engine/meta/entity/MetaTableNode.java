package com.xinao.mes.engine.meta.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xinao.mes.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 元数据-表节点（meta_table_node）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("meta_table_node")
public class MetaTableNode extends BaseEntity {

    /** 节点编码，如 qc_report_item */
    private String nodeCode;

    private String nodeName;

    /** 关联物理表 */
    private String tableName;

    /** 动态列来源：QC_INDICATOR=质检指标纵表展开；NULL=仅固定列 */
    private String dynamicSource;

    /** 1启用 0停用 */
    private Integer status;
}
