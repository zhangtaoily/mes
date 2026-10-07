package com.xinao.mes.qc.vo;

import com.xinao.mes.qc.domain.model.QcPackageDefect;
import com.xinao.mes.qc.domain.model.QcPackageReport;
import lombok.Data;

import java.util.List;

/** 包装质检报告详情（主表 + 疵纱明细） */
@Data
public class QcPackageReportDetailVO {

    private QcPackageReport report;

    private List<QcPackageDefect> defects;
}
