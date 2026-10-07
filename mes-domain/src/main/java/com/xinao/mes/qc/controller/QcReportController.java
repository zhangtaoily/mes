package com.xinao.mes.qc.controller;

import com.xinao.mes.common.auth.RequiresPerm;
import com.xinao.mes.common.log.OperLog;
import com.xinao.mes.common.result.PageResult;
import com.xinao.mes.common.result.R;
import com.xinao.mes.qc.application.QcReportAppService;
import com.xinao.mes.qc.domain.model.QcReport;
import com.xinao.mes.qc.dto.QcReportQueryDTO;
import com.xinao.mes.qc.dto.QcReportSaveDTO;
import com.xinao.mes.qc.vo.QcReportDetailVO;
import com.xinao.mes.qc.vo.QcStatisticsVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 检验报告（质检管理：方案矩阵 → 公式计算 → 越限判定 → 提交/公示 → 作废恢复） */
@RestController
@RequestMapping("/api/qc/reports")
@RequiredArgsConstructor
public class QcReportController {

    private final QcReportAppService reportAppService;

    /** 分页查询（publicOnly=true 时仅公示中的报告） */
    @GetMapping
    @RequiresPerm("qc:report:list")
    public R<PageResult<QcReport>> page(QcReportQueryDTO query) {
        return R.ok(reportAppService.page(query));
    }

    /** 统计（作废报告不计入；不匀率均值为衍生指标参与统计） */
    @GetMapping("/statistics")
    @RequiresPerm("qc:report:list")
    public R<QcStatisticsVO> statistics(@RequestParam(required = false) Long workshopId) {
        return R.ok(reportAppService.statistics(workshopId));
    }

    /** 详情（主表 + 指标矩阵） */
    @GetMapping("/{id}")
    @RequiresPerm("qc:report:list")
    public R<QcReportDetailVO> detail(@PathVariable Long id) {
        return R.ok(reportAppService.detail(id));
    }

    /** 保存（新建按方案生成指标矩阵） */
    @PostMapping
    @RequiresPerm("qc:report:list")
    @OperLog(title = "保存检验报告")
    public R<Long> save(@Valid @RequestBody QcReportSaveDTO dto) {
        return R.ok(reportAppService.save(dto));
    }

    /** 公式计算（$BQ 取试样值集合算衍生指标 + 全量越限判定） */
    @PostMapping("/{id}/calc")
    @RequiresPerm("qc:report:list")
    @OperLog(title = "检验报告公式计算")
    public R<QcReportDetailVO> calc(@PathVariable Long id) {
        return R.ok(reportAppService.calc(id));
    }

    /** 提交 */
    @PostMapping("/{id}/submit")
    @RequiresPerm("qc:report:list")
    @OperLog(title = "提交检验报告")
    public R<Void> submit(@PathVariable Long id) {
        reportAppService.submit(id);
        return R.ok();
    }

    /** 作废（不计入统计；公示中同时取消公示） */
    @PostMapping("/{id}/void")
    @RequiresPerm("qc:report:list")
    @OperLog(title = "作废检验报告")
    public R<Void> voidReport(@PathVariable Long id) {
        reportAppService.voidReport(id);
        return R.ok();
    }

    /** 取消作废（恢复为已提交，重新计入统计） */
    @PostMapping("/{id}/unvoid")
    @RequiresPerm("qc:report:list")
    @OperLog(title = "取消作废检验报告")
    public R<Void> unvoid(@PathVariable Long id) {
        reportAppService.unvoid(id);
        return R.ok();
    }

    /** 公示（对外公示的检验数据） */
    @PostMapping("/{id}/public")
    @RequiresPerm("qc:report:list")
    @OperLog(title = "公示检验报告")
    public R<Void> publicize(@PathVariable Long id) {
        reportAppService.publicize(id);
        return R.ok();
    }
}
