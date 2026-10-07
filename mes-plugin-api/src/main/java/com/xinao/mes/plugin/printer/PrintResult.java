package com.xinao.mes.plugin.printer;

import lombok.Data;

/** 打印回执 */
@Data
public class PrintResult {

    /** 打印任务号 */
    private String jobId;
    /** 状态：QUEUED / PRINTED / FAILED */
    private String status;
    /** 目标打印机 */
    private String printer;
    /** 生成的打印指令（ZPL 等，演示直读） */
    private String spool;
    /** 说明 */
    private String message;
}
