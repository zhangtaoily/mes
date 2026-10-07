package com.xinao.mes.engine.code.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 自动编码规则（sys_auto_code，前缀+日期+流水，如 PSC260927000030）
 */
@Data
@TableName("sys_auto_code")
public class SysAutoCode implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 编号名（业务语义，唯一） */
    private String codeName;

    /** 前缀：PI/PSC/TPC/SR/QC/PS/PC/MF/TP 等 */
    private String prefix;

    /** 日期格式，默认 yyyyMMdd */
    private String dateFormat;

    /** 流水位数 */
    private Integer serialLen;

    /** 重置类型：1不重置 2按日 */
    private Integer resetType;

    /** 当前流水（DB 回写，展示/核对用，发号以 Redis 为准） */
    private Long currentSerial;

    /** 当前流水期间 */
    private String currentPeriod;

    private String createBy;
    private java.time.LocalDateTime createTime;
}
