package com.xinao.mes.qc.domain.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xinao.mes.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 包装质检报告（聚合根：qc_package_defect 疵纱明细，15 类疵纱分类统计） */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("qc_package_report")
public class QcPackageReport extends BaseEntity {

    /** 入库批次 2607933-4 */
    private String warehouseBatchNo;
    /** 生产批次 */
    private String batchNo;
    /** 生产车间 */
    private Long workshopId;
    /** 质检车间（质检五组） */
    private Long qcWorkshopId;
    /** 单据日期 */
    private LocalDate docDate;
    /** 一等品数 */
    private Integer firstGradeQty;
    /** 合格率% */
    private BigDecimal passRate;
    /** 备注 */
    private String remark;
}
