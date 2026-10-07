package com.xinao.mes.pack.domain.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 筒贴打印数量/筒贴详情（包装人员/工号 + 打印量统计与人员追溯） */
@Data
@TableName("pk_tube_print")
public class PkTubePrint {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 包装车间 */
    private Long packWorkshopId;
    /** 生产批号 */
    private String batchNo;
    /** 入库批号 */
    private String warehouseBatchNo;
    /** 缸号 */
    private String dyeVatNo;
    /** 产品规格 */
    private String productSpec;
    /** 包装人员/工号 */
    private String printUser;
    /** 数量 */
    private Integer qty;
    /** 打印日期 */
    private LocalDate printDate;

    /** 创建者 */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
