package com.xinao.mes.produce.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 上批汇报-工艺项（工艺项/工艺值/检验值） */
@Data
@TableName("rep_batch_on_craft")
public class RepBatchOnCraft {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 上批汇报ID（关联 rep_batch_on.id） */
    private Long batchOnId;
    /** 工艺项 */
    private String paramName;
    /** 工艺值 */
    private String paramValue;
    /** 检验值 */
    private String checkValue;
}
