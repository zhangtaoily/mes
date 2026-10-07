package com.xinao.mes.report.application;

import com.xinao.mes.common.exception.BizException;
import com.xinao.mes.report.dto.ReportQueryDTO;
import com.xinao.mes.report.infrastructure.mapper.ReportMapper;
import com.xinao.mes.report.vo.DefectStatsVO;
import com.xinao.mes.report.vo.IndicatorStatsVO;
import com.xinao.mes.report.vo.OutputDailyVO;
import com.xinao.mes.report.vo.PackInboundVO;
import com.xinao.mes.report.vo.ProgressTrackingVO;
import com.xinao.mes.report.vo.QcDailyVO;
import com.xinao.mes.report.vo.RawQcSummaryVO;
import com.xinao.mes.report.vo.ReportCatalogVO;
import com.xinao.mes.report.vo.TubeWeightOutputVO;
import com.xinao.mes.report.vo.WoolRateVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * M6 报表应用服务（P0 12 张，只读跨域聚合）。
 * 计算口径集中在 Service：完成率/越限率/用毛率/状态推导，SQL 只做聚合，便于单测。
 */
@Service
@RequiredArgsConstructor
public class ReportAppService {

    /** 细毛线工序（混条/头针~四针，毛条复精梳） */
    private static final List<Long> WOOL_TOP_PROCESS = List.of(1L, 2L, 3L, 4L, 5L);
    /** 前纺工序（含粗纱） */
    private static final List<Long> FORE_SPIN_PROCESS = List.of(1L, 2L, 3L, 4L, 5L, 6L);
    /** 后纺工序（细纱/并线/倍捻/摇绞） */
    private static final List<Long> AFTER_SPIN_PROCESS = List.of(7L, 8L, 9L, 10L);

    private final ReportMapper reportMapper;

    // ==================== 1. 车间产品质量检验日报表 ====================

    public List<QcDailyVO> qcDaily(ReportQueryDTO query) {
        String begin = query.safeDateBegin(LocalDate.now().minusDays(30).toString());
        String end = query.safeDateEnd(LocalDate.now().toString());
        List<QcDailyVO> rows = reportMapper.qcDaily(begin, end, query.getWorkshopId(), query.getBatchNo());
        List<IndicatorStatsVO> indicators = reportMapper.qcDailyIndicators(begin, end, query.getWorkshopId());
        indicators.forEach(this::fillOverRate);
        rows.forEach(row -> {
            row.setIndicators(indicators);
            row.setOverRate(percent(row.getOverCount(), row.getItemCount()));
        });
        return rows;
    }

    // ==================== 2. 疵纱统计表 ====================

    public DefectStatsVO.DefectSummary defectStats(ReportQueryDTO query) {
        List<DefectStatsVO.DefectRow> rows = reportMapper.defectRows(query.getDateBegin(), query.getDateEnd(), query.getWorkshopId());
        Map<Long, DefectStatsVO> byReport = new LinkedHashMap<>();
        for (DefectStatsVO.DefectRow row : rows) {
            DefectStatsVO vo = byReport.computeIfAbsent(row.getId(), id -> {
                DefectStatsVO v = new DefectStatsVO();
                v.setId(row.getId());
                v.setWarehouseBatchNo(row.getWarehouseBatchNo());
                v.setBatchNo(row.getBatchNo());
                v.setWorkshopName(row.getWorkshopName());
                v.setQcWorkshopName(row.getQcWorkshopName());
                v.setDocDate(row.getDocDate());
                v.setFirstGradeQty(row.getFirstGradeQty());
                v.setPassRate(row.getPassRate());
                v.setDefectTotal(BigDecimal.ZERO);
                return v;
            });
            if (row.getDefectType() != null) {
                vo.getDefects().add(row);
                vo.setDefectTotal(vo.getDefectTotal().add(nullToZero(row.getDefectValue())));
            }
        }
        DefectStatsVO.DefectSummary summary = new DefectStatsVO.DefectSummary();
        summary.setReports(new ArrayList<>(byReport.values()));
        summary.setTypeSummary(reportMapper.defectTypeSummary(query.getDateBegin(), query.getDateEnd(), query.getWorkshopId()));
        return summary;
    }

    // ==================== 3. 筒子称重产量表 ====================

    public List<TubeWeightOutputVO> tubeWeightOutput(ReportQueryDTO query) {
        return reportMapper.tubeWeightOutput(query.getDateBegin(), query.getDateEnd(), query.getWarehouseBatchNo());
    }

    // ==================== 4. 包装批次入库汇总 ====================

    public List<PackInboundVO> packInbound(ReportQueryDTO query) {
        return reportMapper.packInbound(query.getDateBegin(), query.getDateEnd(), query.getWarehouseBatchNo());
    }

    // ==================== 5. 纺纱车间生产进度跟踪 ====================

    public List<ProgressTrackingVO> progressTracking(ReportQueryDTO query) {
        List<ProgressTrackingVO> rows = reportMapper.progressTracking(query.getWorkshopId(), query.getBatchNo());
        LocalDate today = LocalDate.now();
        for (ProgressTrackingVO row : rows) {
            row.setCompleteRate(percentDecimal(nullToZero(row.getOutputQty()), row.getPlanQty()));
            if (row.getIsFinished() != null && row.getIsFinished() == 1) {
                row.setStatus("已完成");
            } else if (row.getDeliveryDate() != null && today.isAfter(row.getDeliveryDate())) {
                row.setStatus("逾期");
            } else {
                row.setStatus("进行中");
            }
        }
        return rows;
    }

    // ==================== 6/7/8. 产量日报（细毛/前纺/纺纱） ====================

    public List<OutputDailyVO> woolTopDaily(ReportQueryDTO query) {
        return outputDaily(query, WOOL_TOP_PROCESS);
    }

    public List<OutputDailyVO> foreSpinStats(ReportQueryDTO query) {
        return outputDaily(query, FORE_SPIN_PROCESS);
    }

    public List<OutputDailyVO> spinningDaily(ReportQueryDTO query) {
        return outputDaily(query, AFTER_SPIN_PROCESS);
    }

    private List<OutputDailyVO> outputDaily(ReportQueryDTO query, List<Long> processIds) {
        String begin = query.safeDateBegin(LocalDate.now().minusDays(30).toString());
        String end = query.safeDateEnd(LocalDate.now().toString());
        return reportMapper.outputDaily(begin, end, query.getWorkshopId(), processIds);
    }

    // ==================== 9. 车间用毛率统计 ====================

    public List<WoolRateVO> woolRate(ReportQueryDTO query) {
        Map<String, WoolRateVO> merged = new LinkedHashMap<>();
        for (WoolRateVO.WoolRateRow row : reportMapper.feedQtyByMonth()) {
            WoolRateVO vo = merged.computeIfAbsent(key(row.getPeriodMonth(), row.getWorkshopId()), k -> newVO(row));
            vo.setFeedQty(nullToZero(row.getFeedQty()));
        }
        for (WoolRateVO.WoolRateRow row : reportMapper.outputQtyByMonth()) {
            WoolRateVO vo = merged.computeIfAbsent(key(row.getPeriodMonth(), row.getWorkshopId()), k -> newVO(row));
            vo.setOutputQty(nullToZero(row.getOutputQty()));
        }
        for (WoolRateVO.WoolRateRow row : reportMapper.returnWoolByMonth()) {
            WoolRateVO vo = merged.computeIfAbsent(key(row.getPeriodMonth(), row.getWorkshopId()), k -> newVO(row));
            vo.setReturnWoolQty(nullToZero(row.getReturnWoolQty()));
        }
        for (WoolRateVO.StandardRateRow row : reportMapper.standardWoolRate()) {
            WoolRateVO vo = merged.get(key(row.getPeriodMonth(), row.getWorkshopId()));
            if (vo != null) {
                vo.setStandardRate(row.getStandardRate());
            }
        }
        for (WoolRateVO vo : merged.values()) {
            vo.setActualRate(percentDecimal(nullToZero(vo.getFeedQty()), vo.getOutputQty()));
            if (vo.getStandardRate() != null && vo.getActualRate() != null) {
                vo.setRateDiff(vo.getActualRate().subtract(vo.getStandardRate()));
            }
        }
        // 车间名补齐（取产出侧任务的车间口径；仅投料的月份无产出行时留空）
        Map<Long, String> workshopNames = workshopNames();
        for (WoolRateVO vo : merged.values()) {
            vo.setWorkshopName(workshopNames.get(vo.getWorkshopId()));
        }
        List<WoolRateVO> result = new ArrayList<>(merged.values());
        if (query.getPeriodMonth() != null && !query.getPeriodMonth().isBlank()) {
            result = result.stream().filter(v -> query.getPeriodMonth().equals(v.getPeriodMonth())).collect(Collectors.toList());
        }
        result.sort((a, b) -> Objects.compare(b.getPeriodMonth(), a.getPeriodMonth(), String::compareTo));
        return result;
    }

    // ==================== 10. 纱线物理指标数据统计 ====================

    public List<IndicatorStatsVO> yarnPhysicalStats(ReportQueryDTO query) {
        String begin = query.safeDateBegin(LocalDate.now().minusDays(30).toString());
        String end = query.safeDateEnd(LocalDate.now().toString());
        List<IndicatorStatsVO> rows = reportMapper.indicatorStats(begin, end, query.getWorkshopId(), query.getIndicatorId());
        rows.forEach(this::fillOverRate);
        return rows;
    }

    // ==================== 11. 条干CV统计 ====================

    public List<IndicatorStatsVO> cvStats(ReportQueryDTO query) {
        Long indicatorId = query.getIndicatorId();
        if (indicatorId == null) {
            indicatorId = reportMapper.findIndicatorIdByName("条干CV值");
            if (indicatorId == null) {
                throw new BizException("未找到【条干CV值】指标，请先在质检指标中维护");
            }
        }
        String begin = query.safeDateBegin(LocalDate.now().minusDays(30).toString());
        String end = query.safeDateEnd(LocalDate.now().toString());
        List<IndicatorStatsVO> rows = reportMapper.cvStatsByBatch(begin, end, query.getWorkshopId(), indicatorId);
        rows.forEach(this::fillOverRate);
        return rows;
    }

    // ==================== 12. 原料检验汇总 ====================

    public List<RawQcSummaryVO> rawQcSummary(ReportQueryDTO query) {
        return reportMapper.rawQcSummary(query.getBatchNo());
    }

    // ==================== 报表目录 ====================

    public List<ReportCatalogVO> catalog() {
        List<ReportCatalogVO> catalog = new ArrayList<>();
        catalog.add(category("包装报表", List.of(
                item("qc-daily", "车间产品质量检验日报表"),
                item("tube-weight-output", "筒子称重产量表"),
                item("pack-inbound", "包装批次入库汇总"),
                item("defect-stats", "疵纱统计表"),
                pending("原棉产量月汇总表"))));
        catalog.add(category("细毛报表", List.of(
                item("wool-top-daily", "细毛产量日报"),
                item("fore-spin-stats", "前纺产量统计"),
                pending("细毛质量月报"))));
        catalog.add(category("纺纱报表", List.of(
                item("progress-tracking", "纺纱车间生产进度跟踪"),
                item("spinning-daily", "纺纱生产日报"),
                pending("盘存报表"),
                pending("配棉分析"),
                pending("电耗报表"))));
        catalog.add(category("纺纱考核类", List.of(
                item("wool-rate", "车间用毛率统计"),
                pending("纺纱细纱考核统计"),
                pending("粗纱库存表"),
                pending("设备利用率报表"),
                pending("纺纱生产周期（天）"))));
        catalog.add(category("试验报表", List.of(
                item("yarn-physical-stats", "纱线物理指标数据统计"),
                item("cv-stats", "条干CV统计"),
                pending("成品强力报表"),
                pending("成品毛羽报表"))));
        catalog.add(category("中心实验室", List.of(
                item("raw-qc-summary", "原料检验汇总"),
                pending("实验室检验数据统计"))));
        catalog.add(category("设备管理", List.of(
                pending("设备保养完成率统计"),
                pending("设备故障停机分析"))));
        catalog.add(category("生产成本分析-纺纱", List.of(
                pending("生产成本分析-纺纱"),
                pending("车间费用汇总"))));
        return catalog;
    }

    // ==================== 私有计算 ====================

    private void fillOverRate(IndicatorStatsVO vo) {
        vo.setOverRate(percent(vo.getOverCount(), vo.getValueCount()));
    }

    /** 整数占比 → xx.xx%（分母 0 或 null 返回 0） */
    public BigDecimal percent(Integer numerator, Integer denominator) {
        if (numerator == null || denominator == null || denominator == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(numerator).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    /** 产量/计划 → xx.xx% 完成率（分母空返回 0） */
    public BigDecimal percentDecimal(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || denominator == null || denominator.signum() == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return numerator.multiply(BigDecimal.valueOf(100)).divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal nullToZero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static String key(String month, Long workshopId) {
        return month + "#" + workshopId;
    }

    private static WoolRateVO newVO(WoolRateVO.WoolRateRow row) {
        WoolRateVO vo = new WoolRateVO();
        vo.setPeriodMonth(row.getPeriodMonth());
        vo.setWorkshopId(row.getWorkshopId());
        return vo;
    }

    /** 车间名映射（用毛率等报表补齐车间名，直查 base_workshop） */
    private Map<Long, String> workshopNames() {
        return reportMapper.workshopNames().stream()
                .filter(r -> r.getWorkshopId() != null && r.getWorkshopName() != null)
                .collect(Collectors.toMap(WoolRateVO.StandardRateRow::getWorkshopId,
                        WoolRateVO.StandardRateRow::getWorkshopName, (a, b) -> a));
    }

    private static ReportCatalogVO category(String name, List<ReportCatalogVO.ReportItem> reports) {
        ReportCatalogVO vo = new ReportCatalogVO();
        vo.setCategory(name);
        vo.setReports(reports);
        return vo;
    }

    private static ReportCatalogVO.ReportItem item(String code, String name) {
        ReportCatalogVO.ReportItem it = new ReportCatalogVO.ReportItem();
        it.setCode(code);
        it.setName(name);
        it.setPriority("P0");
        it.setImplemented(true);
        return it;
    }

    private static ReportCatalogVO.ReportItem pending(String name) {
        ReportCatalogVO.ReportItem it = new ReportCatalogVO.ReportItem();
        it.setName(name);
        it.setPriority("P1");
        it.setImplemented(false);
        return it;
    }
}
