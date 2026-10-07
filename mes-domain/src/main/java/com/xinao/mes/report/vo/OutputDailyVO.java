package com.xinao.mes.report.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 产量日报（细毛 1-5 / 前纺 1-6 / 后纺 7-10 工序组共用：按日+车间+工序） */
@Data
public class OutputDailyVO {

    private LocalDate reportDate;
    private Long workshopId;
    private String workshopName;
    private String processName;
    /** 汇报单数 */
    private Integer reportCount;
    /** 产量合计 */
    private BigDecimal outputQty;
    /** 返工数量 */
    private BigDecimal reworkQty;
    /** 回毛数量 */
    private BigDecimal returnWoolQty;
    /** 毛料数量 */
    private BigDecimal woolQty;
}
