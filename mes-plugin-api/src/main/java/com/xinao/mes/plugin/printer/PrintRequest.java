package com.xinao.mes.plugin.printer;

import lombok.Data;

/** 打印请求 */
@Data
public class PrintRequest {

    /** 模板类型：制造卡 / 箱贴 / 筒贴 / 吊牌 / 复称标签 */
    private String templateType;
    /** 模板编码（base_print_template） */
    private String templateNo;
    /** 业务单号（排程号 / 箱号 / 批号等） */
    private String bizNo;
    /** 打印数据（JSON） */
    private String dataJson;
    /** 打印份数 */
    private Integer copies;
}
