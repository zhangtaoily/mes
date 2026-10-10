package com.xinao.mes.produce.controller;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xinao.mes.common.auth.AuthContext;
import com.xinao.mes.common.auth.RequiresPerm;
import com.xinao.mes.common.exception.BizException;
import com.xinao.mes.common.log.OperLog;
import com.xinao.mes.common.result.R;
import com.xinao.mes.plan.domain.model.BaseEquipment;
import com.xinao.mes.plan.domain.model.BaseProcess;
import com.xinao.mes.plan.domain.model.BaseTeam;
import com.xinao.mes.plan.domain.model.BaseWorkshop;
import com.xinao.mes.plan.domain.model.ProdMachineCard;
import com.xinao.mes.plan.domain.model.ProdSchedule;
import com.xinao.mes.plan.domain.model.ProdScheduleCraft;
import com.xinao.mes.plan.domain.model.ProdTask;
import com.xinao.mes.plan.infrastructure.mapper.BaseEquipmentMapper;
import com.xinao.mes.plan.infrastructure.mapper.BaseProcessMapper;
import com.xinao.mes.plan.infrastructure.mapper.BaseTeamMapper;
import com.xinao.mes.plan.infrastructure.mapper.BaseWorkshopMapper;
import com.xinao.mes.plan.infrastructure.mapper.ProdMachineCardMapper;
import com.xinao.mes.plan.infrastructure.mapper.ProdScheduleCraftMapper;
import com.xinao.mes.plan.infrastructure.mapper.ProdScheduleMapper;
import com.xinao.mes.plan.infrastructure.mapper.ProdTaskMapper;
import com.xinao.mes.produce.application.BatchOnAppService;
import com.xinao.mes.produce.application.FeedAppService;
import com.xinao.mes.produce.application.OutputAppService;
import com.xinao.mes.produce.application.SiteReportAppService;
import com.xinao.mes.produce.domain.model.RepBatchOn;
import com.xinao.mes.produce.domain.model.RepFrontReport;
import com.xinao.mes.produce.domain.model.RepOutput;
import com.xinao.mes.produce.domain.model.RepStartStop;
import com.xinao.mes.produce.dto.BatchOnSaveDTO;
import com.xinao.mes.produce.dto.FeedSaveDTO;
import com.xinao.mes.produce.dto.PdaScanDTO;
import com.xinao.mes.produce.infrastructure.mapper.RepBatchOnMapper;
import com.xinao.mes.produce.vo.PdaScanVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * PDA 扫码 BFF（现场执行入口：扫码 → 上批/下批/产量汇报/投料）
 *
 * <p>与 PC 共用 application 层（同源校验链），仅做扫码聚合展示差异；
 * 扫码内容优先按制造卡二维码（qrCode=cardNo）匹配，否则按排程单号匹配。
 */
@RestController
@RequestMapping("/api/pda")
@RequiredArgsConstructor
public class PdaController {

    private final ProdMachineCardMapper cardMapper;
    private final ProdScheduleMapper scheduleMapper;
    private final ProdScheduleCraftMapper scheduleCraftMapper;
    private final ProdTaskMapper taskMapper;
    private final RepBatchOnMapper batchOnMapper;
    private final BaseProcessMapper processMapper;
    private final BaseEquipmentMapper equipmentMapper;
    private final BaseWorkshopMapper workshopMapper;
    private final BaseTeamMapper teamMapper;
    private final BatchOnAppService batchOnAppService;
    private final OutputAppService outputAppService;
    private final FeedAppService feedAppService;
    private final SiteReportAppService siteReportAppService;

    /** 扫码：制造卡二维码/排程单号 → 排程+任务+工艺项+当前活动上批（上批预览） */
    @PostMapping("/scan")
    @RequiresPerm("site:batchOn:list")
    public R<PdaScanVO> scan(@RequestBody PdaScanDTO dto) {
        String content = dto == null ? null : dto.getContent();
        if (StringUtils.isBlank(content)) {
            throw new BizException("扫码内容不能为空");
        }
        String code = content.trim();
        // 优先按制造卡二维码匹配（未撤回）
        ProdMachineCard card = cardMapper.selectOne(Wrappers.<ProdMachineCard>lambdaQuery()
                .eq(ProdMachineCard::getQrCode, code)
                .isNull(ProdMachineCard::getWithdrawTime)
                .last("LIMIT 1"));
        ProdSchedule schedule;
        if (card != null) {
            schedule = scheduleMapper.selectById(card.getScheduleId());
        } else {
            schedule = scheduleMapper.selectOne(Wrappers.<ProdSchedule>lambdaQuery()
                    .eq(ProdSchedule::getScheduleNo, code)
                    .last("LIMIT 1"));
        }
        if (schedule == null) {
            throw new BizException("二维码[" + code + "]无效：未找到对应的有效排程（可能已撤回）");
        }
        PdaScanVO vo = new PdaScanVO();
        vo.setSchedule(schedule);
        vo.setCard(card);
        vo.setTask(schedule.getTaskId() == null ? null : taskMapper.selectById(schedule.getTaskId()));
        vo.setCrafts(scheduleCraftMapper.selectList(Wrappers.<ProdScheduleCraft>lambdaQuery()
                .eq(ProdScheduleCraft::getScheduleId, schedule.getId())
                .orderByAsc(ProdScheduleCraft::getSortNo)));
        vo.setActiveBatchOn(batchOnMapper.selectOne(Wrappers.<RepBatchOn>lambdaQuery()
                .eq(RepBatchOn::getScheduleId, schedule.getId())
                .eq(RepBatchOn::getIsBatchOff, 0)
                .last("LIMIT 1")));
        vo.setProcessName(nameOfProcess(schedule.getProcessId()));
        vo.setMachineNo(noOfMachine(schedule.getEquipmentId()));
        return R.ok(vo);
    }

    /** PDA 上批（source=PDA，与 PC 同源校验链） */
    @PostMapping("/batch-on")
    @RequiresPerm("site:batchOn:list")
    @OperLog(title = "PDA上批")
    public R<Long> onBatch(@Valid @RequestBody BatchOnSaveDTO dto) {
        if (dto.getBatchOn() != null) {
            dto.getBatchOn().setSource(RepBatchOn.SOURCE_PDA);
        }
        return R.ok(batchOnAppService.onBatch(dto));
    }

    /** PDA 下批 */
    @PostMapping("/batch-off/{id}")
    @RequiresPerm("site:batchOn:list")
    @OperLog(title = "PDA下批")
    public R<Void> offBatch(@PathVariable Long id) {
        batchOnAppService.offBatch(id);
        return R.ok();
    }

    /** PDA 产量汇报（发布事件累计任务工序/订单产出） */
    @PostMapping("/output")
    @RequiresPerm("site:outputReport:list")
    @OperLog(title = "PDA产量汇报")
    public R<Long> saveOutput(@RequestBody RepOutput output) {
        return R.ok(outputAppService.save(output));
    }

    /** PDA 投料（与 PC 同源校验链） */
    @PostMapping("/feed")
    @RequiresPerm("site:feedRecord:list")
    @OperLog(title = "PDA投料")
    public R<Long> feed(@Valid @RequestBody FeedSaveDTO dto) {
        return R.ok(feedAppService.save(dto));
    }

    /** 投料可选任务（生效且未投料完毕封账） */
    @GetMapping("/tasks")
    @RequiresPerm("site:feedRecord:list")
    public R<List<ProdTask>> tasks() {
        return R.ok(feedAppService.availableTasks());
    }

    // ---------- PDA 启停/前纺汇报（M1） ----------

    /** PDA 启停汇报（与 PC 同源，汇报人/日期/机型缺省自动补齐） */
    @PostMapping("/stop")
    @RequiresPerm("site:stopReport:list")
    @OperLog(title = "PDA启停汇报")
    public R<Long> saveStop(@RequestBody RepStartStop stop) {
        if (stop == null) {
            throw new BizException("停机汇报不能为空");
        }
        if (StringUtils.isBlank(stop.getReportUser())) {
            var user = AuthContext.get();
            stop.setReportUser(user == null ? "PDA" : user.getUserName());
        }
        if (stop.getReportDate() == null) {
            stop.setReportDate(LocalDate.now());
        }
        if (StringUtils.isBlank(stop.getMachineModel()) && stop.getEquipmentId() != null) {
            BaseEquipment eq = equipmentMapper.selectById(stop.getEquipmentId());
            if (eq != null) {
                stop.setMachineModel(eq.getMachineModel());
            }
        }
        return R.ok(siteReportAppService.saveStop(stop));
    }

    /** PDA 前纺生产情况汇报（与 PC 同源，日期缺省今天） */
    @PostMapping("/front-report")
    @RequiresPerm("craft:frontReport:list")
    @OperLog(title = "PDA前纺汇报")
    public R<Long> saveFront(@RequestBody RepFrontReport report) {
        if (report == null) {
            throw new BizException("前纺汇报不能为空");
        }
        if (report.getReportDate() == null) {
            report.setReportDate(LocalDate.now());
        }
        return R.ok(siteReportAppService.saveFront(report));
    }

    // ---------- PDA 常用下拉（登录即可，避免依赖 PC 基础信息权限） ----------

    /** 车间（启用） */
    @GetMapping("/workshops")
    public R<List<BaseWorkshop>> workshops() {
        return R.ok(workshopMapper.selectList(Wrappers.<BaseWorkshop>lambdaQuery()
                .eq(BaseWorkshop::getStatus, 1).orderByAsc(BaseWorkshop::getId)));
    }

    /** 班组（启用） */
    @GetMapping("/teams")
    public R<List<BaseTeam>> teams() {
        return R.ok(teamMapper.selectList(Wrappers.<BaseTeam>lambdaQuery()
                .eq(BaseTeam::getStatus, 1).orderByAsc(BaseTeam::getId)));
    }

    /** 机台（可按车间过滤） */
    @GetMapping("/machines")
    public R<List<BaseEquipment>> machines(@RequestParam(required = false) Long workshopId) {
        return R.ok(equipmentMapper.selectList(Wrappers.<BaseEquipment>lambdaQuery()
                .eq(workshopId != null, BaseEquipment::getWorkshopId, workshopId)
                .orderByAsc(BaseEquipment::getId)));
    }

    // ---------- 内部 ----------

    private String nameOfProcess(Long processId) {
        if (processId == null) {
            return null;
        }
        BaseProcess p = processMapper.selectById(processId);
        return p == null ? null : p.getProcessName();
    }

    private String noOfMachine(Long equipmentId) {
        if (equipmentId == null) {
            return null;
        }
        BaseEquipment e = equipmentMapper.selectById(equipmentId);
        return e == null ? null : e.getMachineNo();
    }
}
