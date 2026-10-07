package com.xinao.mes.plan.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/** 生产任务-工序路线 */
@Data
@TableName("prod_task_route")
public class ProdTaskRoute {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务ID */
    private Long taskId;
    /** 序号（工序顺序） */
    private Integer seq;
    /** 类型 */
    private String typeName;
    /** 工序ID */
    private Long processId;
    /** 工序名称 */
    private String processName;
    /** 投料（红绿图标，1是 0否） */
    private Integer isFeed;
    /** 报产（1是 0否） */
    private Integer isReport;
    /** 上下批（是否翻批，1是 0否） */
    private Integer isBatchChange;
    /** 物料单位 */
    private String materialUnit;
    /** 生产单位 */
    private String prodUnit;
    /** 计划产量 */
    private BigDecimal planQty;
    /** 投料量 */
    private BigDecimal feedQty;
    /** 产出量 */
    private BigDecimal outputQty;
    /** 车间ID */
    private Long workshopId;
    /** 了批（1是 0否） */
    private Integer isFinishBatch;
    /** 工序标识 */
    private String processFlag;
}
