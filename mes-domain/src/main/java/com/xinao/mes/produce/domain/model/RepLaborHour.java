package com.xinao.mes.produce.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 人员工时（rep_labor_hour，无逻辑删除列，物理删除） */
@Data
@TableName("rep_labor_hour")
public class RepLaborHour implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 年月，如 2026-10 */
    private String periodMonth;
    /** 工时产线 */
    private String prodLine;
    /** 人员 */
    private Long userId;
    /** 工时 */
    private BigDecimal workHour;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
}
