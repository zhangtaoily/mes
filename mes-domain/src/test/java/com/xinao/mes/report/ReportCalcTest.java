package com.xinao.mes.report;

import com.xinao.mes.report.application.ReportAppService;
import com.xinao.mes.report.dto.ReportQueryDTO;
import com.xinao.mes.report.infrastructure.mapper.ReportMapper;
import com.xinao.mes.report.vo.DefectStatsVO;
import com.xinao.mes.report.vo.IndicatorStatsVO;
import com.xinao.mes.report.vo.ProgressTrackingVO;
import com.xinao.mes.report.vo.ReportCatalogVO;
import com.xinao.mes.report.vo.WoolRateVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * M6 报表口径单测：完成率/状态推导/用毛率合并/疵纱组装/目录完整性。
 * SQL 聚合正确性由集成验收脚本与手工台账核对（M6 验收清单）。
 */
class ReportCalcTest {

    private ReportMapper mapper;
    private ReportAppService service;

    @BeforeEach
    void setUp() {
        mapper = mock(ReportMapper.class);
        service = new ReportAppService(mapper);
        lenient().when(mapper.workshopNames()).thenReturn(List.of(row(1L, "纺十车间")));
    }

    private static WoolRateVO.StandardRateRow row(Long id, String name) {
        WoolRateVO.StandardRateRow r = new WoolRateVO.StandardRateRow();
        r.setWorkshopId(id);
        r.setWorkshopName(name);
        return r;
    }

    // ==================== 完成率 / 越限率 ====================

    @Test
    void percentNullDenominatorReturnsZero() {
        assertEquals(new BigDecimal("0.00"), service.percent(null, null));
        assertEquals(new BigDecimal("0.00"), service.percent(3, 0));
    }

    @Test
    void percentComputesRate() {
        // 3 越限 / 30 项 = 10.00%
        assertEquals(new BigDecimal("10.00"), service.percent(3, 30));
        // 1 / 3 = 33.33%
        assertEquals(new BigDecimal("33.33"), service.percent(1, 3));
    }

    @Test
    void percentDecimalComputesCompleteRate() {
        // 1400 / 3000 = 46.67%
        assertEquals(new BigDecimal("46.67"),
                service.percentDecimal(new BigDecimal("1400"), new BigDecimal("3000")));
        assertEquals(new BigDecimal("0.00"), service.percentDecimal(new BigDecimal("100"), null));
    }

    // ==================== 生产进度：完成率 + 状态推导 ====================

    private static ProgressTrackingVO task(Long id, Integer isFinished, LocalDate delivery, BigDecimal plan, BigDecimal output) {
        ProgressTrackingVO vo = new ProgressTrackingVO();
        vo.setId(id);
        vo.setTaskNo("PT" + id);
        vo.setBatchNo("26091340");
        vo.setIsFinished(isFinished);
        vo.setDeliveryDate(delivery);
        vo.setPlanQty(plan);
        vo.setOutputQty(output);
        return vo;
    }

    @Test
    void progressTrackingStatusDerivation() {
        LocalDate today = LocalDate.now();
        when(mapper.progressTracking(any(), any())).thenReturn(List.of(
                task(1L, 1, today.plusDays(5), new BigDecimal("3000"), new BigDecimal("3000")),   // 已了批 → 已完成
                task(2L, 0, today.minusDays(3), new BigDecimal("1000"), new BigDecimal("500")),   // 交期已过 → 逾期
                task(3L, 0, today.plusDays(10), new BigDecimal("3000"), new BigDecimal("1400")),  // 正常 → 进行中
                task(4L, 0, today.plusDays(10), new BigDecimal("800"), null)                      // 无产出 → 0.00%
        ));
        List<ProgressTrackingVO> rows = service.progressTracking(new ReportQueryDTO());
        assertEquals("已完成", rows.get(0).getStatus());
        assertEquals("逾期", rows.get(1).getStatus());
        assertEquals("进行中", rows.get(2).getStatus());
        // 1400/3000 = 46.67%
        assertEquals(new BigDecimal("46.67"), rows.get(2).getCompleteRate());
        // 500/1000 = 50.00%
        assertEquals(new BigDecimal("50.00"), rows.get(1).getCompleteRate());
        // 无产出 → 0.00%
        assertEquals(new BigDecimal("0.00"), rows.get(3).getCompleteRate());
    }

    // ==================== 用毛率：四查询合并 + 实际 vs 标准 ====================

    private static WoolRateVO.WoolRateRow wr(String month, Long ws, String field, BigDecimal val) {
        WoolRateVO.WoolRateRow r = new WoolRateVO.WoolRateRow();
        r.setPeriodMonth(month);
        r.setWorkshopId(ws);
        switch (field) {
            case "feed" -> r.setFeedQty(val);
            case "output" -> r.setOutputQty(val);
            case "return" -> r.setReturnWoolQty(val);
        }
        return r;
    }

    @Test
    void woolRateMergesAndComputesActualVsStandard() {
        when(mapper.feedQtyByMonth()).thenReturn(List.of(wr("2026-09", 1L, "feed", new BigDecimal("1500"))));
        when(mapper.outputQtyByMonth()).thenReturn(List.of(wr("2026-09", 1L, "output", new BigDecimal("1400"))));
        when(mapper.returnWoolByMonth()).thenReturn(List.of(wr("2026-09", 1L, "return", new BigDecimal("30"))));
        WoolRateVO.StandardRateRow std = row(1L, null);
        std.setPeriodMonth("2026-09");
        std.setStandardRate(new BigDecimal("105.000"));
        when(mapper.standardWoolRate()).thenReturn(List.of(std));

        List<WoolRateVO> rows = service.woolRate(new ReportQueryDTO());
        assertEquals(1, rows.size());
        WoolRateVO vo = rows.get(0);
        assertEquals("纺十车间", vo.getWorkshopName());
        // 实际用毛率 = 1500/1400 = 107.14%
        assertEquals(new BigDecimal("107.14"), vo.getActualRate());
        // 差额 = 107.14 - 105.000 = 2.14
        assertEquals(new BigDecimal("2.14"), vo.getRateDiff().setScale(2, java.math.RoundingMode.HALF_UP));
    }

    @Test
    void woolRateFeedOnlyMonthYieldsZeroRate() {
        // 只有投料无产出（分母 0）→ 实际用毛率 0，标准差额为空
        when(mapper.feedQtyByMonth()).thenReturn(List.of(wr("2026-08", 1L, "feed", new BigDecimal("900"))));
        when(mapper.outputQtyByMonth()).thenReturn(List.of());
        when(mapper.returnWoolByMonth()).thenReturn(List.of());
        when(mapper.standardWoolRate()).thenReturn(List.of());

        List<WoolRateVO> rows = service.woolRate(new ReportQueryDTO());
        assertEquals(1, rows.size());
        assertEquals(new BigDecimal("0.00"), rows.get(0).getActualRate());
        assertNull(rows.get(0).getStandardRate());
    }

    // ==================== 疵纱统计：批次分组组装 ====================

    private static DefectStatsVO.DefectRow dRow(Long id, String whBatch, String type, String value) {
        DefectStatsVO.DefectRow r = new DefectStatsVO.DefectRow();
        r.setId(id);
        r.setWarehouseBatchNo(whBatch);
        r.setDefectType(type);
        r.setDefectValue(new BigDecimal(value));
        return r;
    }

    @Test
    void defectStatsGroupsByReportAndSumsTotal() {
        when(mapper.defectRows(any(), any(), any())).thenReturn(List.of(
                dRow(1L, "RK26100701", "正纱", "2"),
                dRow(1L, "RK26100701", "断纱", "3"),
                dRow(2L, "RK26100702", "油纱", "1")
        ));
        when(mapper.defectTypeSummary(any(), any(), any())).thenReturn(List.of(dRow(null, null, "断纱", "3")));

        DefectStatsVO.DefectSummary summary = service.defectStats(new ReportQueryDTO());
        assertEquals(2, summary.getReports().size());
        DefectStatsVO first = summary.getReports().get(0);
        assertEquals(2, first.getDefects().size());
        // RK26100701 疵纱合计 = 2 + 3 = 5
        assertEquals(new BigDecimal("5"), first.getDefectTotal());
        assertEquals("断纱", summary.getTypeSummary().get(0).getDefectType());
    }

    // ==================== 条干CV：指标缺失拦截 ====================

    @Test
    void cvStatsRejectsWhenIndicatorMissing() {
        when(mapper.findIndicatorIdByName("条干CV值")).thenReturn(null);
        Exception ex = org.junit.jupiter.api.Assertions.assertThrows(
                com.xinao.mes.common.exception.BizException.class,
                () -> service.cvStats(new ReportQueryDTO()));
        assertTrue(ex.getMessage().contains("条干CV值"));
    }

    @Test
    void cvStatsQueriesWithIndicatorId() {
        when(mapper.findIndicatorIdByName("条干CV值")).thenReturn(7L);
        when(mapper.cvStatsByBatch(anyString(), anyString(), any(), anyLong()))
                .thenReturn(List.of(new IndicatorStatsVO()));
        List<IndicatorStatsVO> rows = service.cvStats(new ReportQueryDTO());
        assertEquals(1, rows.size());
    }

    // ==================== 产量日报：工序组过滤 ====================

    @Test
    void spinningDailyUsesAfterSpinProcesses() {
        when(mapper.outputDaily(anyString(), anyString(), any(), anyList())).thenReturn(List.of());
        service.spinningDaily(new ReportQueryDTO());
        org.mockito.Mockito.verify(mapper).outputDaily(anyString(), anyString(), any(),
                org.mockito.ArgumentMatchers.argThat(ids -> ids.size() == 4 && ids.contains(7L) && ids.contains(10L)));
    }

    @Test
    void foreSpinStatsIncludesRoving() {
        when(mapper.outputDaily(anyString(), anyString(), any(), anyList())).thenReturn(List.of());
        service.foreSpinStats(new ReportQueryDTO());
        org.mockito.Mockito.verify(mapper).outputDaily(anyString(), anyString(), any(),
                org.mockito.ArgumentMatchers.argThat(ids -> ids.size() == 6 && ids.contains(6L)));
    }

    // ==================== 报表目录：8 分类 12 张 P0 ====================

    @Test
    void catalogHasEightCategoriesAndTwelveP0Reports() {
        List<ReportCatalogVO> catalog = service.catalog();
        assertEquals(8, catalog.size());
        List<ReportCatalogVO.ReportItem> implemented = catalog.stream()
                .flatMap(c -> c.getReports().stream())
                .filter(ReportCatalogVO.ReportItem::isImplemented)
                .toList();
        assertEquals(12, implemented.size());
        implemented.forEach(item -> {
            assertEquals("P0", item.getPriority());
            assertFalse(item.getCode() == null || item.getCode().isBlank());
        });
        // 后置报表不提供编码
        catalog.stream().flatMap(c -> c.getReports().stream())
                .filter(r -> !r.isImplemented())
                .forEach(r -> assertTrue(r.getCode() == null, "后置报表不应有编码: " + r.getName()));
    }
}
