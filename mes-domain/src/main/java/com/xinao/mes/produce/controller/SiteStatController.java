package com.xinao.mes.produce.controller;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinao.mes.common.auth.RequiresPerm;
import com.xinao.mes.common.exception.BizException;
import com.xinao.mes.common.log.OperLog;
import com.xinao.mes.common.result.PageResult;
import com.xinao.mes.common.result.R;
import com.xinao.mes.produce.domain.model.RepLaborHour;
import com.xinao.mes.produce.domain.model.StAttendance;
import com.xinao.mes.produce.domain.model.StCost;
import com.xinao.mes.produce.domain.model.StEnergy;
import com.xinao.mes.produce.domain.model.StEnergyItem;
import com.xinao.mes.produce.domain.model.StIssue;
import com.xinao.mes.produce.domain.model.StShift;
import com.xinao.mes.produce.domain.model.StShiftPerson;
import com.xinao.mes.produce.infrastructure.mapper.RepLaborHourMapper;
import com.xinao.mes.produce.infrastructure.mapper.StAttendanceMapper;
import com.xinao.mes.produce.infrastructure.mapper.StCostMapper;
import com.xinao.mes.produce.infrastructure.mapper.StEnergyItemMapper;
import com.xinao.mes.produce.infrastructure.mapper.StEnergyMapper;
import com.xinao.mes.produce.infrastructure.mapper.StIssueMapper;
import com.xinao.mes.produce.infrastructure.mapper.StShiftMapper;
import com.xinao.mes.produce.infrastructure.mapper.StShiftPersonMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 现场统计：车间出勤/排班/人员工时/能耗/生产费用/问题反馈
 * （月度编制单据：编制中→已提交，支持撤回；单表维护直接走 mapper）
 */
@RestController
@RequestMapping("/api/site")
@RequiredArgsConstructor
public class SiteStatController {

    private final StAttendanceMapper attendanceMapper;
    private final StShiftMapper shiftMapper;
    private final StShiftPersonMapper shiftPersonMapper;
    private final RepLaborHourMapper laborHourMapper;
    private final StEnergyMapper energyMapper;
    private final StEnergyItemMapper energyItemMapper;
    private final StCostMapper costMapper;
    private final StIssueMapper issueMapper;

    // ==================== 车间出勤 ====================

    /** 出勤分页 */
    @GetMapping("/attendance")
    @RequiresPerm("site:attendance:list")
    public R<PageResult<StAttendance>> pageAttendance(@RequestParam(required = false) String periodMonth,
                                                      @RequestParam(required = false) Long workshopId,
                                                      @RequestParam(required = false) String docStatus,
                                                      @RequestParam(defaultValue = "1") Integer pageNum,
                                                      @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<StAttendance> p = attendanceMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<StAttendance>lambdaQuery()
                        .eq(StringUtils.isNotBlank(periodMonth), StAttendance::getPeriodMonth, periodMonth)
                        .eq(workshopId != null, StAttendance::getWorkshopId, workshopId)
                        .eq(StringUtils.isNotBlank(docStatus), StAttendance::getDocStatus, docStatus)
                        .orderByDesc(StAttendance::getPeriodMonth).orderByAsc(StAttendance::getWorkshopId));
        return R.ok(PageResult.of(p.getTotal(), p.getRecords()));
    }

    /** 保存出勤（期间+车间唯一） */
    @PostMapping("/attendance")
    @RequiresPerm("site:attendance:list")
    @OperLog(title = "保存车间出勤")
    public R<Long> saveAttendance(@RequestBody StAttendance att) {
        checkPeriodUnique(attendanceMapper.selectCount(Wrappers.<StAttendance>lambdaQuery()
                        .eq(StAttendance::getPeriodMonth, att.getPeriodMonth())
                        .eq(StAttendance::getWorkshopId, att.getWorkshopId())
                        .ne(att.getId() != null, StAttendance::getId, att.getId())),
                "出勤", att.getPeriodMonth());
        if (att.getId() == null) {
            att.setDocStatus("编制中");
            attendanceMapper.insert(att);
        } else {
            attendanceMapper.updateById(att);
        }
        return R.ok(att.getId());
    }

    /** 提交出勤 */
    @PostMapping("/attendance/{id}/submit")
    @RequiresPerm("site:attendance:list")
    @OperLog(title = "提交车间出勤")
    public R<Void> submitAttendance(@PathVariable Long id) {
        StAttendance att = attendanceMapper.selectById(id);
        if (att == null) {
            throw new BizException("出勤单据不存在");
        }
        if (!"编制中".equals(att.getDocStatus())) {
            throw new BizException("出勤单当前状态[" + att.getDocStatus() + "]不可提交");
        }
        att.setDocStatus("已提交");
        attendanceMapper.updateById(att);
        return R.ok();
    }

    /** 撤回出勤 */
    @PostMapping("/attendance/{id}/withdraw")
    @RequiresPerm("site:attendance:list")
    @OperLog(title = "撤回车间出勤")
    public R<Void> withdrawAttendance(@PathVariable Long id) {
        StAttendance att = attendanceMapper.selectById(id);
        if (att != null && "已提交".equals(att.getDocStatus())) {
            att.setDocStatus("编制中");
            attendanceMapper.updateById(att);
        }
        return R.ok();
    }

    /** 删除出勤 */
    @DeleteMapping("/attendance/{id}")
    @RequiresPerm("site:attendance:list")
    @OperLog(title = "删除车间出勤")
    public R<Void> deleteAttendance(@PathVariable Long id) {
        attendanceMapper.deleteById(id);
        return R.ok();
    }

    // ==================== 车间排班 ====================

    /** 排班分页 */
    @GetMapping("/schedule")
    @RequiresPerm("site:schedulePlan:list")
    public R<PageResult<StShift>> pageShifts(@RequestParam(required = false) Long workshopId,
                                             @RequestParam(required = false) Long teamId,
                                             @RequestParam(required = false) String shiftName,
                                             @RequestParam(defaultValue = "1") Integer pageNum,
                                             @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<StShift> p = shiftMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<StShift>lambdaQuery()
                        .eq(workshopId != null, StShift::getWorkshopId, workshopId)
                        .eq(teamId != null, StShift::getTeamId, teamId)
                        .like(StringUtils.isNotBlank(shiftName), StShift::getShiftName, shiftName)
                        .orderByDesc(StShift::getStartDate).orderByDesc(StShift::getId));
        return R.ok(PageResult.of(p.getTotal(), p.getRecords()));
    }

    /** 保存排班 */
    @PostMapping("/schedule")
    @RequiresPerm("site:schedulePlan:list")
    @OperLog(title = "保存车间排班")
    public R<Long> saveShift(@RequestBody StShift shift) {
        if (shift.getId() == null) {
            shiftMapper.insert(shift);
        } else {
            shiftMapper.updateById(shift);
        }
        return R.ok(shift.getId());
    }

    /** 删除排班（连带人员） */
    @DeleteMapping("/schedule/{id}")
    @RequiresPerm("site:schedulePlan:list")
    @OperLog(title = "删除车间排班")
    @Transactional
    public R<Void> deleteShift(@PathVariable Long id) {
        shiftMapper.deleteById(id);
        shiftPersonMapper.delete(Wrappers.<StShiftPerson>lambdaQuery().eq(StShiftPerson::getShiftId, id));
        return R.ok();
    }

    /** 排班人员列表 */
    @GetMapping("/schedule/{id}/persons")
    @RequiresPerm("site:schedulePlan:list")
    public R<List<StShiftPerson>> listPersons(@PathVariable Long id) {
        return R.ok(shiftPersonMapper.selectList(Wrappers.<StShiftPerson>lambdaQuery()
                .eq(StShiftPerson::getShiftId, id).orderByAsc(StShiftPerson::getId)));
    }

    /** 批量追加排班人员 */
    @PostMapping("/schedule/{id}/persons")
    @RequiresPerm("site:schedulePlan:list")
    @OperLog(title = "追加排班人员")
    public R<Void> addPersons(@PathVariable Long id, @RequestBody List<StShiftPerson> persons) {
        for (StShiftPerson p : persons) {
            p.setId(null);
            p.setShiftId(id);
            shiftPersonMapper.insert(p);
        }
        return R.ok();
    }

    /** 移除排班人员 */
    @DeleteMapping("/schedule/persons/{personId}")
    @RequiresPerm("site:schedulePlan:list")
    @OperLog(title = "移除排班人员")
    public R<Void> deletePerson(@PathVariable Long personId) {
        shiftPersonMapper.deleteById(personId);
        return R.ok();
    }

    // ==================== 人员工时 ====================

    /** 工时分页 */
    @GetMapping("/work-hours")
    @RequiresPerm("site:workHours:list")
    public R<PageResult<RepLaborHour>> pageLaborHours(@RequestParam(required = false) String periodMonth,
                                                      @RequestParam(required = false) String prodLine,
                                                      @RequestParam(required = false) Long userId,
                                                      @RequestParam(defaultValue = "1") Integer pageNum,
                                                      @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<RepLaborHour> p = laborHourMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<RepLaborHour>lambdaQuery()
                        .eq(StringUtils.isNotBlank(periodMonth), RepLaborHour::getPeriodMonth, periodMonth)
                        .like(StringUtils.isNotBlank(prodLine), RepLaborHour::getProdLine, prodLine)
                        .eq(userId != null, RepLaborHour::getUserId, userId)
                        .orderByDesc(RepLaborHour::getPeriodMonth).orderByAsc(RepLaborHour::getId));
        return R.ok(PageResult.of(p.getTotal(), p.getRecords()));
    }

    /** 保存工时 */
    @PostMapping("/work-hours")
    @RequiresPerm("site:workHours:list")
    @OperLog(title = "保存人员工时")
    public R<Long> saveLaborHour(@RequestBody RepLaborHour hour) {
        if (hour.getId() == null) {
            laborHourMapper.insert(hour);
        } else {
            laborHourMapper.updateById(hour);
        }
        return R.ok(hour.getId());
    }

    /** 删除工时 */
    @DeleteMapping("/work-hours/{id}")
    @RequiresPerm("site:workHours:list")
    @OperLog(title = "删除人员工时")
    public R<Void> deleteLaborHour(@PathVariable Long id) {
        laborHourMapper.deleteById(id);
        return R.ok();
    }

    // ==================== 车间能耗 ====================

    /** 能耗分页 */
    @GetMapping("/energy")
    @RequiresPerm("site:energy:list")
    public R<PageResult<StEnergy>> pageEnergy(@RequestParam(required = false) String periodMonth,
                                              @RequestParam(required = false) Long workshopId,
                                              @RequestParam(required = false) String docStatus,
                                              @RequestParam(defaultValue = "1") Integer pageNum,
                                              @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<StEnergy> p = energyMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<StEnergy>lambdaQuery()
                        .eq(StringUtils.isNotBlank(periodMonth), StEnergy::getPeriodMonth, periodMonth)
                        .eq(workshopId != null, StEnergy::getWorkshopId, workshopId)
                        .eq(StringUtils.isNotBlank(docStatus), StEnergy::getDocStatus, docStatus)
                        .orderByDesc(StEnergy::getPeriodMonth).orderByAsc(StEnergy::getWorkshopId));
        return R.ok(PageResult.of(p.getTotal(), p.getRecords()));
    }

    /** 保存能耗（期间+车间唯一） */
    @PostMapping("/energy")
    @RequiresPerm("site:energy:list")
    @OperLog(title = "保存车间能耗")
    public R<Long> saveEnergy(@RequestBody StEnergy energy) {
        checkPeriodUnique(energyMapper.selectCount(Wrappers.<StEnergy>lambdaQuery()
                        .eq(StEnergy::getPeriodMonth, energy.getPeriodMonth())
                        .eq(StEnergy::getWorkshopId, energy.getWorkshopId())
                        .ne(energy.getId() != null, StEnergy::getId, energy.getId())),
                "能耗", energy.getPeriodMonth());
        if (energy.getId() == null) {
            energy.setDocStatus("编制中");
            energyMapper.insert(energy);
        } else {
            energyMapper.updateById(energy);
        }
        return R.ok(energy.getId());
    }

    /** 提交能耗 */
    @PostMapping("/energy/{id}/submit")
    @RequiresPerm("site:energy:list")
    @OperLog(title = "提交车间能耗")
    public R<Void> submitEnergy(@PathVariable Long id) {
        StEnergy energy = energyMapper.selectById(id);
        if (energy == null) {
            throw new BizException("能耗单据不存在");
        }
        if (!"编制中".equals(energy.getDocStatus())) {
            throw new BizException("能耗单当前状态[" + energy.getDocStatus() + "]不可提交");
        }
        energy.setDocStatus("已提交");
        energyMapper.updateById(energy);
        return R.ok();
    }

    /** 撤回能耗 */
    @PostMapping("/energy/{id}/withdraw")
    @RequiresPerm("site:energy:list")
    @OperLog(title = "撤回车间能耗")
    public R<Void> withdrawEnergy(@PathVariable Long id) {
        StEnergy energy = energyMapper.selectById(id);
        if (energy != null && "已提交".equals(energy.getDocStatus())) {
            energy.setDocStatus("编制中");
            energyMapper.updateById(energy);
        }
        return R.ok();
    }

    /** 删除能耗（连带明细） */
    @DeleteMapping("/energy/{id}")
    @RequiresPerm("site:energy:list")
    @OperLog(title = "删除车间能耗")
    @Transactional
    public R<Void> deleteEnergy(@PathVariable Long id) {
        energyMapper.deleteById(id);
        energyItemMapper.delete(Wrappers.<StEnergyItem>lambdaQuery().eq(StEnergyItem::getEnergyId, id));
        return R.ok();
    }

    /** 能耗机台明细 */
    @GetMapping("/energy/{id}/items")
    @RequiresPerm("site:energy:list")
    public R<List<StEnergyItem>> listEnergyItems(@PathVariable Long id) {
        return R.ok(energyItemMapper.selectList(Wrappers.<StEnergyItem>lambdaQuery()
                .eq(StEnergyItem::getEnergyId, id).orderByAsc(StEnergyItem::getId)));
    }

    /** 追加能耗机台明细（批量） */
    @PostMapping("/energy/{id}/items")
    @RequiresPerm("site:energy:list")
    @OperLog(title = "追加能耗明细")
    public R<Void> addEnergyItems(@PathVariable Long id, @RequestBody List<StEnergyItem> items) {
        for (StEnergyItem it : items) {
            it.setId(null);
            it.setEnergyId(id);
            energyItemMapper.insert(it);
        }
        return R.ok();
    }

    /** 删除能耗明细 */
    @DeleteMapping("/energy/items/{itemId}")
    @RequiresPerm("site:energy:list")
    @OperLog(title = "删除能耗明细")
    public R<Void> deleteEnergyItem(@PathVariable Long itemId) {
        energyItemMapper.deleteById(itemId);
        return R.ok();
    }

    // ==================== 车间生产费用 ====================

    /** 费用分页 */
    @GetMapping("/cost")
    @RequiresPerm("site:cost:list")
    public R<PageResult<StCost>> pageCost(@RequestParam(required = false) String periodMonth,
                                          @RequestParam(required = false) Long workshopId,
                                          @RequestParam(required = false) String docStatus,
                                          @RequestParam(defaultValue = "1") Integer pageNum,
                                          @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<StCost> p = costMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<StCost>lambdaQuery()
                        .eq(StringUtils.isNotBlank(periodMonth), StCost::getPeriodMonth, periodMonth)
                        .eq(workshopId != null, StCost::getWorkshopId, workshopId)
                        .eq(StringUtils.isNotBlank(docStatus), StCost::getDocStatus, docStatus)
                        .orderByDesc(StCost::getPeriodMonth).orderByAsc(StCost::getWorkshopId));
        return R.ok(PageResult.of(p.getTotal(), p.getRecords()));
    }

    /** 保存费用（期间+车间唯一） */
    @PostMapping("/cost")
    @RequiresPerm("site:cost:list")
    @OperLog(title = "保存车间生产费用")
    public R<Long> saveCost(@RequestBody StCost cost) {
        checkPeriodUnique(costMapper.selectCount(Wrappers.<StCost>lambdaQuery()
                        .eq(StCost::getPeriodMonth, cost.getPeriodMonth())
                        .eq(StCost::getWorkshopId, cost.getWorkshopId())
                        .ne(cost.getId() != null, StCost::getId, cost.getId())),
                "生产费用", cost.getPeriodMonth());
        if (cost.getId() == null) {
            cost.setDocStatus("编制中");
            costMapper.insert(cost);
        } else {
            costMapper.updateById(cost);
        }
        return R.ok(cost.getId());
    }

    /** 提交费用 */
    @PostMapping("/cost/{id}/submit")
    @RequiresPerm("site:cost:list")
    @OperLog(title = "提交车间生产费用")
    public R<Void> submitCost(@PathVariable Long id) {
        StCost cost = costMapper.selectById(id);
        if (cost == null) {
            throw new BizException("生产费用单据不存在");
        }
        if (!"编制中".equals(cost.getDocStatus())) {
            throw new BizException("生产费用单当前状态[" + cost.getDocStatus() + "]不可提交");
        }
        cost.setDocStatus("已提交");
        costMapper.updateById(cost);
        return R.ok();
    }

    /** 撤回费用 */
    @PostMapping("/cost/{id}/withdraw")
    @RequiresPerm("site:cost:list")
    @OperLog(title = "撤回车间生产费用")
    public R<Void> withdrawCost(@PathVariable Long id) {
        StCost cost = costMapper.selectById(id);
        if (cost != null && "已提交".equals(cost.getDocStatus())) {
            cost.setDocStatus("编制中");
            costMapper.updateById(cost);
        }
        return R.ok();
    }

    /** 删除费用 */
    @DeleteMapping("/cost/{id}")
    @RequiresPerm("site:cost:list")
    @OperLog(title = "删除车间生产费用")
    public R<Void> deleteCost(@PathVariable Long id) {
        costMapper.deleteById(id);
        return R.ok();
    }

    // ==================== 现场问题反馈 ====================

    /** 反馈分页 */
    @GetMapping("/issue")
    @RequiresPerm("site:issueFeedback:list")
    public R<PageResult<StIssue>> pageIssues(@RequestParam(required = false) String checkUser,
                                             @RequestParam(required = false) String rectifyStatus,
                                             @RequestParam(required = false) String issueLocation,
                                             @RequestParam(defaultValue = "1") Integer pageNum,
                                             @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<StIssue> p = issueMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<StIssue>lambdaQuery()
                        .like(StringUtils.isNotBlank(checkUser), StIssue::getCheckUser, checkUser)
                        .eq(StringUtils.isNotBlank(rectifyStatus), StIssue::getRectifyStatus, rectifyStatus)
                        .like(StringUtils.isNotBlank(issueLocation), StIssue::getIssueLocation, issueLocation)
                        .orderByDesc(StIssue::getCheckDate).orderByDesc(StIssue::getId));
        return R.ok(PageResult.of(p.getTotal(), p.getRecords()));
    }

    /** 保存反馈 */
    @PostMapping("/issue")
    @RequiresPerm("site:issueFeedback:list")
    @OperLog(title = "保存现场问题反馈")
    public R<Long> saveIssue(@RequestBody StIssue issue) {
        if (issue.getId() == null) {
            issueMapper.insert(issue);
        } else {
            issueMapper.updateById(issue);
        }
        return R.ok(issue.getId());
    }

    /** 删除反馈 */
    @DeleteMapping("/issue/{id}")
    @RequiresPerm("site:issueFeedback:list")
    @OperLog(title = "删除现场问题反馈")
    public R<Void> deleteIssue(@PathVariable Long id) {
        issueMapper.deleteById(id);
        return R.ok();
    }

    // ---------- 内部 ----------

    /** 期间+车间唯一性校验 */
    private void checkPeriodUnique(Long count, String biz, String period) {
        if (count != null && count > 0) {
            throw new BizException(biz + "[" + period + "]该车间该期间已存在，请直接编辑原单");
        }
    }
}
