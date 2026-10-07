package com.xinao.mes.produce.controller;

import com.xinao.mes.common.auth.RequiresPerm;
import com.xinao.mes.common.log.OperLog;
import com.xinao.mes.common.result.PageResult;
import com.xinao.mes.common.result.R;
import com.xinao.mes.produce.application.SiteReportAppService;
import com.xinao.mes.produce.domain.model.RepAutoWindingCut;
import com.xinao.mes.produce.domain.model.RepFrontReport;
import com.xinao.mes.produce.domain.model.RepRebatchDetail;
import com.xinao.mes.produce.domain.model.RepRebatchLog;
import com.xinao.mes.produce.domain.model.RepStartStop;
import com.xinao.mes.produce.domain.model.RepYarnSteam;
import com.xinao.mes.produce.dto.FrontReportQueryDTO;
import com.xinao.mes.produce.dto.RebatchGenDTO;
import com.xinao.mes.produce.dto.RebatchQueryDTO;
import com.xinao.mes.produce.dto.StopQueryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 现场汇报：启停/翻批日志/前纺生产情况/自络筒切疵/蒸纱信息
 * （分属现场管理与工艺管理菜单，方法级全路径）
 */
@RestController
@RequiredArgsConstructor
public class SiteReportController {

    private final SiteReportAppService siteReportAppService;

    // ---------- 启停汇报 ----------

    /** 分页查询 */
    @GetMapping("/api/site/stop")
    @RequiresPerm("site:stopReport:list")
    public R<PageResult<RepStartStop>> pageStops(StopQueryDTO query) {
        return R.ok(siteReportAppService.pageStops(query));
    }

    /** 保存（新增取号 ST，编辑直接更新） */
    @PostMapping("/api/site/stop")
    @RequiresPerm("site:stopReport:list")
    @OperLog(title = "启停汇报")
    public R<Long> saveStop(@RequestBody RepStartStop stop) {
        return R.ok(siteReportAppService.saveStop(stop));
    }

    /** 删除 */
    @DeleteMapping("/api/site/stop/{id}")
    @RequiresPerm("site:stopReport:list")
    @OperLog(title = "删除启停汇报")
    public R<Void> deleteStop(@PathVariable Long id) {
        siteReportAppService.deleteStop(id);
        return R.ok();
    }

    // ---------- 翻批日志 ----------

    /** 分页查询 */
    @GetMapping("/api/site/rebatch")
    @RequiresPerm("site:batchChangeLog:list")
    public R<PageResult<RepRebatchLog>> pageRebatchLogs(RebatchQueryDTO query) {
        return R.ok(siteReportAppService.pageRebatchLogs(query));
    }

    /** 翻批明细（上批快照） */
    @GetMapping("/api/site/rebatch/{id}/details")
    @RequiresPerm("site:batchChangeLog:list")
    public R<List<RepRebatchDetail>> rebatchDetails(@PathVariable Long id) {
        return R.ok(siteReportAppService.rebatchDetails(id));
    }

    /** 生成翻批记录：按车间+班组+时段汇总上批快照 */
    @PostMapping("/api/site/rebatch/generate")
    @RequiresPerm("site:batchChangeLog:list")
    @OperLog(title = "生成翻批记录")
    public R<Long> generateRebatch(@RequestBody RebatchGenDTO dto) {
        return R.ok(siteReportAppService.generateRebatch(dto));
    }

    // ---------- 前纺生产情况汇报（工艺管理菜单） ----------

    /** 分页查询 */
    @GetMapping("/api/craft/front-report")
    @RequiresPerm("craft:frontReport:list")
    public R<PageResult<RepFrontReport>> pageFront(FrontReportQueryDTO query) {
        return R.ok(siteReportAppService.pageFront(query));
    }

    /** 保存 */
    @PostMapping("/api/craft/front-report")
    @RequiresPerm("craft:frontReport:list")
    @OperLog(title = "前纺生产情况汇报")
    public R<Long> saveFront(@RequestBody RepFrontReport report) {
        return R.ok(siteReportAppService.saveFront(report));
    }

    /** 删除 */
    @DeleteMapping("/api/craft/front-report/{id}")
    @RequiresPerm("craft:frontReport:list")
    @OperLog(title = "删除前纺汇报")
    public R<Void> deleteFront(@PathVariable Long id) {
        siteReportAppService.deleteFront(id);
        return R.ok();
    }

    // ---------- 自络筒切疵记录 ----------

    /** 分页查询 */
    @GetMapping("/api/site/yarn-clearer")
    @RequiresPerm("site:yarnClearer:list")
    public R<PageResult<RepAutoWindingCut>> pageCuts(FrontReportQueryDTO query) {
        return R.ok(siteReportAppService.pageCuts(query));
    }

    /** 保存 */
    @PostMapping("/api/site/yarn-clearer")
    @RequiresPerm("site:yarnClearer:list")
    @OperLog(title = "自络筒切疵记录")
    public R<Long> saveCut(@RequestBody RepAutoWindingCut cut) {
        return R.ok(siteReportAppService.saveCut(cut));
    }

    /** 删除 */
    @DeleteMapping("/api/site/yarn-clearer/{id}")
    @RequiresPerm("site:yarnClearer:list")
    @OperLog(title = "删除切疵记录")
    public R<Void> deleteCut(@PathVariable Long id) {
        siteReportAppService.deleteCut(id);
        return R.ok();
    }

    // ---------- 蒸纱信息（打印/接收闭环） ----------

    /** 分页查询 */
    @GetMapping("/api/site/steam")
    @RequiresPerm("site:steam:list")
    public R<PageResult<RepYarnSteam>> pageSteams(FrontReportQueryDTO query) {
        return R.ok(siteReportAppService.pageSteams(query));
    }

    /** 保存 */
    @PostMapping("/api/site/steam")
    @RequiresPerm("site:steam:list")
    @OperLog(title = "保存蒸纱信息")
    public R<Long> saveSteam(@RequestBody RepYarnSteam steam) {
        return R.ok(siteReportAppService.saveSteam(steam));
    }

    /** 打印（回填打印人/时间） */
    @PostMapping("/api/site/steam/{id}/print")
    @RequiresPerm("site:steam:list")
    @OperLog(title = "蒸纱信息打印")
    public R<Void> printSteam(@PathVariable Long id) {
        siteReportAppService.printSteam(id);
        return R.ok();
    }

    /** 接收（打印/接收闭环，未打印不可接收） */
    @PostMapping("/api/site/steam/{id}/receive")
    @RequiresPerm("site:steam:list")
    @OperLog(title = "蒸纱信息接收")
    public R<Void> receiveSteam(@PathVariable Long id) {
        siteReportAppService.receiveSteam(id);
        return R.ok();
    }

    /** 删除 */
    @DeleteMapping("/api/site/steam/{id}")
    @RequiresPerm("site:steam:list")
    @OperLog(title = "删除蒸纱信息")
    public R<Void> deleteSteam(@PathVariable Long id) {
        siteReportAppService.deleteSteam(id);
        return R.ok();
    }
}
