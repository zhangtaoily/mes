package com.xinao.mes.report.controller;

import com.xinao.mes.common.auth.RequiresPerm;
import com.xinao.mes.common.result.R;
import com.xinao.mes.report.application.ReportAppService;
import com.xinao.mes.report.dto.ReportQueryDTO;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * M6 报表中心（P0 12 张，只读跨域聚合，原型 reports.html 分类树）。
 * 全部 GET 查询，权限统一 report:list（admin 通配可访问）。
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportAppService reportAppService;

    /** 报表目录（分类树：P0 已实现，P1/P2 后置灰显） */
    @GetMapping("/catalog")
    @RequiresPerm("report:list")
    public R<List<ReportCatalogVO>> catalog() {
        return R.ok(reportAppService.catalog());
    }

    /** 1. 车间产品质量检验日报表 */
    @GetMapping("/qc-daily")
    @RequiresPerm("report:list")
    public R<List<QcDailyVO>> qcDaily(ReportQueryDTO query) {
        return R.ok(reportAppService.qcDaily(query));
    }

    /** 2. 疵纱统计表（15 类疵纱：批次台账 + 类型合计） */
    @GetMapping("/defect-stats")
    @RequiresPerm("report:list")
    public R<DefectStatsVO.DefectSummary> defectStats(ReportQueryDTO query) {
        return R.ok(reportAppService.defectStats(query));
    }

    /** 3. 筒子称重产量表 */
    @GetMapping("/tube-weight-output")
    @RequiresPerm("report:list")
    public R<List<TubeWeightOutputVO>> tubeWeightOutput(ReportQueryDTO query) {
        return R.ok(reportAppService.tubeWeightOutput(query));
    }

    /** 4. 包装批次入库汇总 */
    @GetMapping("/pack-inbound")
    @RequiresPerm("report:list")
    public R<List<PackInboundVO>> packInbound(ReportQueryDTO query) {
        return R.ok(reportAppService.packInbound(query));
    }

    /** 5. 纺纱车间生产进度跟踪 */
    @GetMapping("/progress-tracking")
    @RequiresPerm("report:list")
    public R<List<ProgressTrackingVO>> progressTracking(ReportQueryDTO query) {
        return R.ok(reportAppService.progressTracking(query));
    }

    /** 6. 纺纱生产日报（后纺 7-10 工序） */
    @GetMapping("/spinning-daily")
    @RequiresPerm("report:list")
    public R<List<OutputDailyVO>> spinningDaily(ReportQueryDTO query) {
        return R.ok(reportAppService.spinningDaily(query));
    }

    /** 7. 细毛产量日报（毛条线 1-5 工序） */
    @GetMapping("/wool-top-daily")
    @RequiresPerm("report:list")
    public R<List<OutputDailyVO>> woolTopDaily(ReportQueryDTO query) {
        return R.ok(reportAppService.woolTopDaily(query));
    }

    /** 8. 前纺产量统计（前纺 1-6 工序） */
    @GetMapping("/fore-spin-stats")
    @RequiresPerm("report:list")
    public R<List<OutputDailyVO>> foreSpinStats(ReportQueryDTO query) {
        return R.ok(reportAppService.foreSpinStats(query));
    }

    /** 9. 车间用毛率统计（实际 vs 标准） */
    @GetMapping("/wool-rate")
    @RequiresPerm("report:list")
    public R<List<WoolRateVO>> woolRate(ReportQueryDTO query) {
        return R.ok(reportAppService.woolRate(query));
    }

    /** 10. 纱线物理指标数据统计 */
    @GetMapping("/yarn-physical-stats")
    @RequiresPerm("report:list")
    public R<List<IndicatorStatsVO>> yarnPhysicalStats(ReportQueryDTO query) {
        return R.ok(reportAppService.yarnPhysicalStats(query));
    }

    /** 11. 条干CV统计 */
    @GetMapping("/cv-stats")
    @RequiresPerm("report:list")
    public R<List<IndicatorStatsVO>> cvStats(ReportQueryDTO query) {
        return R.ok(reportAppService.cvStats(query));
    }

    /** 12. 原料检验汇总（中心实验室） */
    @GetMapping("/raw-qc-summary")
    @RequiresPerm("report:list")
    public R<List<RawQcSummaryVO>> rawQcSummary(ReportQueryDTO query) {
        return R.ok(reportAppService.rawQcSummary(query));
    }
}
