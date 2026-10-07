package com.xinao.mes.pack.domain.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 组托记录（托号 TP... + 箱号绑定；组托完成后箱贴含托盘码） */
@Data
@TableName("pk_pallet")
public class PkPallet {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 托号 */
    private String palletCode;
    /** 箱号 */
    private String boxCode;
    /** 包装车间 */
    private Long packWorkshopId;
    /** 生产批号 */
    private String batchNo;
    /** 缸号 */
    private String dyeVatNo;
    /** 标重 */
    private BigDecimal standardWeight;
    /** 毛重 */
    private BigDecimal grossWeight;
    /** 净重 */
    private BigDecimal netWeight;
    /** 标准筒数 */
    private Integer standardTube;
    /** 组托人 */
    private String palletUser;
    /** 组托时间 */
    private LocalDateTime palletTime;

    /** 创建者 */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
