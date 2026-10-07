package com.xinao.mes.plan.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 排程-上机工艺（项/值/上下限） */
@Data
@TableName("prod_schedule_craft")
public class ProdScheduleCraft {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 排程ID */
    private Long scheduleId;
    /** 工艺项 */
    private String paramName;
    /** 工艺值 */
    private String paramValue;
    /** 上限 */
    private String upperLimit;
    /** 下限 */
    private String lowerLimit;
    /** 排序号 */
    private Integer sortNo;
}
