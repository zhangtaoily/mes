package com.xinao.mes.produce.domain.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 周转车移动明细（操作名称：绑定/称重/入库/移库/车辆拆分/出库） */
@Data
@TableName("rep_truck_move")
public class RepTruckMove {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 周转车ID（关联 rep_truck.id） */
    private Long truckId;
    /** 移出库位 */
    private String fromLocation;
    /** 移入库位 */
    private String toLocation;
    /** 扫描车间 */
    private String scanWorkshop;
    /** 复精梳批号 */
    private String recombedBatchNo;
    /** 纺纱批号 */
    private String spinBatchNo;
    /** 袋子颜色 */
    private String bagColor;
    /** 毛球数 */
    private Integer ballCount;
    /** 毛重 */
    private BigDecimal grossWeight;
    /** 净重 */
    private BigDecimal netWeight;
    /** 标重 */
    private BigDecimal standardWeight;
    /** 公定回潮 */
    private BigDecimal moistureStandard;
    /** 实测回潮 */
    private BigDecimal moistureActual;
    /** 操作名称:绑定/称重/入库/移库/车辆拆分/出库 */
    private String opName;
    /** 移出净重 */
    private BigDecimal moveNetWeight;
    /** 移出毛球数 */
    private Integer moveBallCount;
    /** 先排（1=是） */
    private Integer isFirst;
    /** 了批（1=是） */
    private Integer isFinished;

    /** 创建人 */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
