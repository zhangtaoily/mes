package com.xinao.mes.produce.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/** 排班人员（st_shift_person，无审计列，物理删除） */
@Data
@TableName("st_shift_person")
public class StShiftPerson implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 排班ID */
    private Long shiftId;
    /** 人员 */
    private Long userId;
    private String userName;
    private String empNo;
    /** 机台 */
    private Long equipmentId;
}
