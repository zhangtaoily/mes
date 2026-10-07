package com.xinao.mes.common.log;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志（sys_oper_log）
 */
@Data
@TableName("sys_oper_log")
public class SysOperLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作说明 */
    private String title;
    /** 请求方法全名 */
    private String method;
    /** 请求 URL */
    private String url;
    /** 操作人 */
    private String operName;
    /** 操作 IP */
    private String operIp;
    /** 请求参数 */
    private String operParam;
    /** 返回结果 */
    private String jsonResult;
    /** 1成功 0失败 */
    private Integer status;
    /** 异常信息 */
    private String errorMsg;
    /** 操作时间 */
    private LocalDateTime operTime;
}
