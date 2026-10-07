package com.xinao.mes.qc.application;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinao.mes.common.exception.BizException;
import com.xinao.mes.common.result.PageResult;
import com.xinao.mes.qc.domain.model.QcPackageDefect;
import com.xinao.mes.qc.domain.model.QcPackageReport;
import com.xinao.mes.qc.dto.QcPackageReportSaveDTO;
import com.xinao.mes.qc.infrastructure.mapper.QcPackageDefectMapper;
import com.xinao.mes.qc.infrastructure.mapper.QcPackageReportMapper;
import com.xinao.mes.qc.vo.QcPackageReportDetailVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * 包装质检报告应用服务（按入库批次录入 15 类疵纱分类统计；
 * 一等品数缺省取"正纱"数量，合格率 = 一等品/明细合计×100 服务端计算）
 */
@Service
@RequiredArgsConstructor
public class QcPackageReportAppService {

    private final QcPackageReportMapper reportMapper;
    private final QcPackageDefectMapper defectMapper;

    public PageResult<QcPackageReport> page(String warehouseBatchNo, String batchNo, int pageNum, int pageSize) {
        Page<QcPackageReport> p = Page.of(pageNum, pageSize);
        Page<QcPackageReport> result = reportMapper.selectPage(p, Wrappers.<QcPackageReport>lambdaQuery()
                .like(StringUtils.isNotBlank(warehouseBatchNo), QcPackageReport::getWarehouseBatchNo, warehouseBatchNo)
                .like(StringUtils.isNotBlank(batchNo), QcPackageReport::getBatchNo, batchNo)
                .orderByDesc(QcPackageReport::getId));
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    public QcPackageReportDetailVO detail(Long id) {
        QcPackageReport report = reportMapper.selectById(id);
        if (report == null) {
            throw new BizException("包装质检报告不存在: " + id);
        }
        QcPackageReportDetailVO vo = new QcPackageReportDetailVO();
        vo.setReport(report);
        vo.setDefects(defectMapper.selectList(Wrappers.<QcPackageDefect>lambdaQuery()
                .eq(QcPackageDefect::getPackageQcId, id)));
        return vo;
    }

    @Transactional
    public Long save(QcPackageReportSaveDTO dto) {
        if (dto.getWarehouseBatchNo() == null || dto.getWarehouseBatchNo().isBlank()) {
            throw new BizException("入库批次不能为空");
        }
        QcPackageReport report;
        if (dto.getId() == null) {
            report = new QcPackageReport();
            report.setWarehouseBatchNo(dto.getWarehouseBatchNo());
            report.setBatchNo(dto.getBatchNo());
            report.setWorkshopId(dto.getWorkshopId());
            report.setQcWorkshopId(dto.getQcWorkshopId());
            report.setDocDate(dto.getDocDate() == null ? LocalDate.now() : dto.getDocDate());
            report.setRemark(dto.getRemark());
            reportMapper.insert(report);
        } else {
            report = reportMapper.selectById(dto.getId());
            if (report == null) {
                throw new BizException("包装质检报告不存在: " + dto.getId());
            }
            report.setBatchNo(dto.getBatchNo());
            report.setWorkshopId(dto.getWorkshopId());
            report.setQcWorkshopId(dto.getQcWorkshopId());
            report.setDocDate(dto.getDocDate());
            report.setRemark(dto.getRemark());
            reportMapper.updateById(report);
            defectMapper.delete(Wrappers.<QcPackageDefect>lambdaQuery()
                    .eq(QcPackageDefect::getPackageQcId, report.getId()));
        }
        // 疵纱明细整表保存 + 合格率计算
        List<QcPackageReportSaveDTO.DefectVal> defects = dto.getDefects();
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal firstGrade = BigDecimal.ZERO;
        if (defects != null) {
            for (QcPackageReportSaveDTO.DefectVal d : defects) {
                QcPackageDefect defect = new QcPackageDefect();
                defect.setPackageQcId(report.getId());
                defect.setDefectType(d.getDefectType());
                defect.setValue(d.getValue());
                defect.setRemark(d.getRemark());
                defectMapper.insert(defect);
                if (d.getValue() != null) {
                    total = total.add(d.getValue());
                    if ("正纱".equals(d.getDefectType())) {
                        firstGrade = d.getValue();
                    }
                }
            }
        }
        Integer firstGradeQty = dto.getFirstGradeQty() != null ? dto.getFirstGradeQty() : firstGrade.intValue();
        report.setFirstGradeQty(firstGradeQty);
        if (total.compareTo(BigDecimal.ZERO) > 0) {
            report.setPassRate(BigDecimal.valueOf(firstGradeQty)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(total, 3, RoundingMode.HALF_UP));
        }
        reportMapper.updateById(report);
        return report.getId();
    }
}
