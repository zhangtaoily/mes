package com.xinao.mes.pack.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 箱子删除记录（pk_box_delete，删箱审计留痕，只读） */
@Data
@TableName("pk_box_delete")
public class PkBoxDelete implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 箱子编码 */
    private String boxCode;
    /** 托盘码 */
    private String palletCode;
    private Integer seqNo;
    private BigDecimal tareWeight;
    private BigDecimal grossWeight;
    private Integer tubeCount;
    private BigDecimal paperTubeWeight;
    /** 删除人 */
    private String deleteUser;
    /** 删除时间 */
    private LocalDateTime deleteTime;
}
