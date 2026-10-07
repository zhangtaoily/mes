package com.xinao.mes.pack.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 箱子删除记录（审计：删箱时自动留痕，与 pk_box 逻辑删同步） */
@Data
@TableName("pk_box_delete")
public class PkBoxDelete {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 箱子编码 */
    private String boxCode;
    /** 托盘码 */
    private String palletCode;
    /** 正序/箱序号 */
    private Integer seqNo;
    /** 去皮重量 */
    private BigDecimal tareWeight;
    /** 毛重 */
    private BigDecimal grossWeight;
    /** 筒子数 */
    private Integer tubeCount;
    /** 纸管重量 */
    private BigDecimal paperTubeWeight;
    /** 删除人 */
    private String deleteUser;
    /** 删除时间 */
    private LocalDateTime deleteTime;
}
