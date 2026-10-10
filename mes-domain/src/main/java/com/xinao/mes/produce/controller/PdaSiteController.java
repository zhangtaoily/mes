package com.xinao.mes.produce.controller;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xinao.mes.common.auth.RequiresPerm;
import com.xinao.mes.common.exception.BizException;
import com.xinao.mes.common.log.OperLog;
import com.xinao.mes.common.result.R;
import com.xinao.mes.plan.domain.model.BaseWorkshop;
import com.xinao.mes.plan.infrastructure.mapper.BaseWorkshopMapper;
import com.xinao.mes.produce.application.PickAppService;
import com.xinao.mes.produce.application.SiteReportAppService;
import com.xinao.mes.produce.application.TruckAppService;
import com.xinao.mes.produce.domain.model.MtPick;
import com.xinao.mes.produce.domain.model.RepTruck;
import com.xinao.mes.produce.domain.model.RepYarnSteam;
import com.xinao.mes.produce.dto.TruckMoveDTO;
import com.xinao.mes.produce.dto.TruckSaveDTO;
import com.xinao.mes.produce.infrastructure.mapper.MtPickMapper;
import com.xinao.mes.produce.infrastructure.mapper.RepTruckMapper;
import com.xinao.mes.produce.infrastructure.mapper.RepYarnSteamMapper;
import com.xinao.mes.produce.vo.TruckDetailVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * PDA 现场物流 BFF（M3：领退料确认 / 毛球周转车 / 蒸纱信息）
 *
 * <p>与 PC 共用 application 层（同源校验链）：领退料确认走 flow 迁移 + CAS；
 * 周转车移动/新增记全程轨迹；蒸纱打印/接收闭环（未打印不可接收）。
 * PDA 差异补齐：查询关键词模糊 + LIMIT 50、待办单据在前；
 * 移动操作 scanWorkshop 缺省按车辆所属车间带出。
 */
@RestController
@RequestMapping("/api/pda/site")
@RequiredArgsConstructor
public class PdaSiteController {

    private final PickAppService pickAppService;
    private final TruckAppService truckAppService;
    private final SiteReportAppService siteReportAppService;
    private final MtPickMapper pickMapper;
    private final RepTruckMapper truckMapper;
    private final RepYarnSteamMapper steamMapper;
    private final BaseWorkshopMapper workshopMapper;

    // ---------- 领退料确认 ----------

    /** 领退料单查询（单号/订单号/批号模糊；docStatus 可选过滤，待确认在前，最多50条） */
    @GetMapping("/picks")
    @RequiresPerm("prod:feed:list")
    public R<List<MtPick>> picks(@RequestParam(required = false) String keyword,
                                 @RequestParam(required = false) String docStatus) {
        String kw = StringUtils.isBlank(keyword) ? null : keyword.trim();
        String status = StringUtils.isBlank(docStatus) ? null : docStatus.trim();
        return R.ok(pickMapper.selectList(Wrappers.<MtPick>lambdaQuery()
                .and(kw != null, w -> w.like(MtPick::getPickNo, kw)
                        .or().like(MtPick::getOrderNo, kw)
                        .or().like(MtPick::getBatchNo, kw))
                .eq(status != null, MtPick::getDocStatus, status)
                .last("ORDER BY (doc_status = '待确认') DESC, id DESC LIMIT 50")));
    }

    /** PDA 领退料批量确认（flow 迁移 + CAS，确认人/时间回填当前用户） */
    @PostMapping("/picks/confirm")
    @RequiresPerm("prod:feed:list")
    @OperLog(title = "PDA领退料确认")
    public R<Void> confirm(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BizException("请选择要确认的单据");
        }
        pickAppService.confirm(ids);
        return R.ok();
    }

    // ---------- 毛球周转车 ----------

    /** 周转车查询（车号/批号模糊，最多50条） */
    @GetMapping("/trucks")
    @RequiresPerm("site:truck:list")
    public R<List<RepTruck>> trucks(@RequestParam(required = false) String keyword) {
        String kw = StringUtils.isBlank(keyword) ? null : keyword.trim();
        return R.ok(truckMapper.selectList(Wrappers.<RepTruck>lambdaQuery()
                .and(kw != null, w -> w.like(RepTruck::getTruckNo, kw)
                        .or().like(RepTruck::getBatchNo, kw))
                .orderByDesc(RepTruck::getId)
                .last("LIMIT 50")));
    }

    /** 周转车详情（主表 + 移动轨迹） */
    @GetMapping("/trucks/{id}")
    @RequiresPerm("site:truck:list")
    public R<TruckDetailVO> truckDetail(@PathVariable Long id) {
        return R.ok(truckAppService.detail(id));
    }

    /** PDA 新增周转车（空车登记，车号唯一校验同 PC） */
    @PostMapping("/trucks")
    @RequiresPerm("site:truck:list")
    @OperLog(title = "PDA新增周转车")
    public R<Long> saveTruck(@Valid @RequestBody TruckSaveDTO dto) {
        return R.ok(truckAppService.save(dto));
    }

    /** PDA 周转车移动（绑定/称重/入库/移库/出库，记移动轨迹；scanWorkshop 缺省按车辆车间带出） */
    @PostMapping("/trucks/move")
    @RequiresPerm("site:truck:list")
    @OperLog(title = "PDA周转车移动")
    public R<Void> move(@RequestBody TruckMoveDTO dto) {
        if (dto.getTruckId() == null) {
            throw new BizException("请先扫描周转车");
        }
        if (StringUtils.isBlank(dto.getScanWorkshop())) {
            RepTruck truck = truckMapper.selectById(dto.getTruckId());
            if (truck != null && truck.getWorkshopId() != null) {
                BaseWorkshop ws = workshopMapper.selectById(truck.getWorkshopId());
                if (ws != null) {
                    dto.setScanWorkshop(ws.getWorkshopName());
                }
            }
        }
        truckAppService.move(dto);
        return R.ok();
    }

    // ---------- 蒸纱信息 ----------

    /** 蒸纱信息查询（批号/车号模糊；未打印在前、未接收在前，最多50条） */
    @GetMapping("/steams")
    @RequiresPerm("site:steam:list")
    public R<List<RepYarnSteam>> steams(@RequestParam(required = false) String keyword) {
        String kw = StringUtils.isBlank(keyword) ? null : keyword.trim();
        return R.ok(steamMapper.selectList(Wrappers.<RepYarnSteam>lambdaQuery()
                .and(kw != null, w -> w.like(RepYarnSteam::getBatchNo, kw)
                        .or().like(RepYarnSteam::getTruckNo, kw))
                .last("ORDER BY (print_time IS NULL) DESC, (receive_time IS NULL) DESC, id DESC LIMIT 50")));
    }

    /** PDA 新增蒸纱信息（车号必填校验同 PC） */
    @PostMapping("/steams")
    @RequiresPerm("site:steam:list")
    @OperLog(title = "PDA新增蒸纱信息")
    public R<Long> saveSteam(@RequestBody RepYarnSteam steam) {
        return R.ok(siteReportAppService.saveSteam(steam));
    }

    /** PDA 蒸纱打印（回填打印人/时间；已打印拦截同 PC） */
    @PostMapping("/steams/{id}/print")
    @RequiresPerm("site:steam:list")
    @OperLog(title = "PDA蒸纱打印")
    public R<Void> printSteam(@PathVariable Long id) {
        siteReportAppService.printSteam(id);
        return R.ok();
    }

    /** PDA 蒸纱接收（未打印不可接收，闭环校验同 PC） */
    @PostMapping("/steams/{id}/receive")
    @RequiresPerm("site:steam:list")
    @OperLog(title = "PDA蒸纱接收")
    public R<Void> receiveSteam(@PathVariable Long id) {
        siteReportAppService.receiveSteam(id);
        return R.ok();
    }
}
