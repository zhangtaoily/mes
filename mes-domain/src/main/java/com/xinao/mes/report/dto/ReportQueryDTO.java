package com.xinao.mes.report.dto;

import lombok.Data;

/** 报表通用查询参数（各报表按需取用，日期为 yyyy-MM-dd 字符串） */
@Data
public class ReportQueryDTO {

    /** 开始日期 */
    private String dateBegin;
    /** 结束日期 */
    private String dateEnd;
    /** 车间 */
    private Long workshopId;
    /** 生产批号（模糊） */
    private String batchNo;
    /** 入库批号（模糊） */
    private String warehouseBatchNo;
    /** 年月（yyyy-MM，用毛率用） */
    private String periodMonth;
    /** 指标（条干CV/物理指标统计用，空=条干CV值） */
    private Long indicatorId;

    /** 默认近 30 天（报表中心无日期参数时的兜底口径） */
    public String safeDateBegin(String fallback) {
        return dateBegin != null && !dateBegin.isBlank() ? dateBegin : fallback;
    }

    public String safeDateEnd(String fallback) {
        return dateEnd != null && !dateEnd.isBlank() ? dateEnd : fallback;
    }
}
