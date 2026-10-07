package com.xinao.mes.engine.meta.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xinao.mes.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 元数据-列节点（meta_column_node，列表列/查询项动态渲染）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("meta_column_node")
public class MetaColumnNode extends BaseEntity {

    private Long nodeId;

    private String columnCode;

    private String columnLabel;

    /** string/number/date */
    private String dataType;

    /** LIST=列表列 QUERY=查询项 BOTH=两者 */
    private String columnUsage;

    /** eq/like/between/ge/le */
    private String queryType;

    /** 关联字典类型 */
    private String dictType;

    private Integer sortable;

    private Integer width;

    private Integer sortNo;

    /** 1显示 0隐藏 */
    private Integer visible;
}
