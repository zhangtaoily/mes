package com.xinao.mes.plugin.pda;

import lombok.Data;

/** PDA 标准上报结构（适配后） */
@Data
public class PdaReport {

    /** 扫码内容（批次条码 / 箱码 / 单号） */
    private String scanCode;
    /** 上报类型：上批 / 投料 / 产量 / 组托 */
    private String reportType;
    /** 操作员工号 */
    private String userCode;
    /** PDA 型号标识 */
    private String deviceModel;
    /** 原始报文（留痕） */
    private String raw;
}
