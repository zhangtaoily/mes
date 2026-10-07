package com.xinao.mes.engine.code.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 编码取号日志（sys_code_log，缺号即断号，可追溯）
 */
@Data
@TableName("sys_code_log")
public class SysCodeLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long ruleId;

    private String codeName;

    /** 流水期间 */
    private String period;

    private Long serial;

    /** 完整单号 */
    private String fullNo;

    /** 业务关联单据 */
    private String bizRef;

    private String createBy;
    private LocalDateTime createTime;
}
