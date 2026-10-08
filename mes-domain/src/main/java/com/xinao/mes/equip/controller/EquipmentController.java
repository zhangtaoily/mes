package com.xinao.mes.equip.controller;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinao.mes.common.auth.RequiresPerm;
import com.xinao.mes.common.log.OperLog;
import com.xinao.mes.common.result.PageResult;
import com.xinao.mes.common.result.R;
import com.xinao.mes.equip.domain.model.EqCheckRecord;
import com.xinao.mes.equip.domain.model.EqDutyMachine;
import com.xinao.mes.equip.domain.model.EqMaintainPlan;
import com.xinao.mes.equip.domain.model.EqMaintainRecord;
import com.xinao.mes.equip.domain.model.EqRepairRecord;
import com.xinao.mes.equip.infrastructure.mapper.EqCheckRecordMapper;
import com.xinao.mes.equip.infrastructure.mapper.EqDutyMachineMapper;
import com.xinao.mes.equip.infrastructure.mapper.EqMaintainPlanMapper;
import com.xinao.mes.equip.infrastructure.mapper.EqMaintainRecordMapper;
import com.xinao.mes.equip.infrastructure.mapper.EqRepairRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 设备管理：保养计划/保养记录/点检记录/维保记录/责任机台 */
@RestController
@RequestMapping("/api/equip")
@RequiredArgsConstructor
public class EquipmentController {

    private final EqMaintainPlanMapper planMapper;
    private final EqMaintainRecordMapper maintainMapper;
    private final EqCheckRecordMapper checkMapper;
    private final EqRepairRecordMapper repairMapper;
    private final EqDutyMachineMapper dutyMapper;

    // ==================== 保养计划（逻辑删） ====================

    @GetMapping("/maintain-plans")
    @RequiresPerm("equip:maintainPlan:list")
    public R<PageResult<EqMaintainPlan>> pagePlans(@RequestParam(required = false) String planCode,
                                                   @RequestParam(required = false) Long workshopId,
                                                   @RequestParam(defaultValue = "1") Integer pageNum,
                                                   @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<EqMaintainPlan> p = planMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<EqMaintainPlan>lambdaQuery()
                        .like(StringUtils.isNotBlank(planCode), EqMaintainPlan::getPlanCode, planCode)
                        .eq(workshopId != null, EqMaintainPlan::getWorkshopId, workshopId)
                        .orderByDesc(EqMaintainPlan::getId));
        return R.ok(PageResult.of(p.getTotal(), p.getRecords()));
    }

    @PostMapping("/maintain-plans")
    @RequiresPerm("equip:maintainPlan:list")
    @OperLog(title = "保存保养计划")
    public R<Long> savePlan(@RequestBody EqMaintainPlan plan) {
        if (StringUtils.isBlank(plan.getPlanCode())) {
            throw new com.xinao.mes.common.exception.BizException("计划编码不能为空");
        }
        if (plan.getId() == null) {
            planMapper.insert(plan);
        } else {
            planMapper.updateById(plan);
        }
        return R.ok(plan.getId());
    }

    @DeleteMapping("/maintain-plans/{id}")
    @RequiresPerm("equip:maintainPlan:list")
    @OperLog(title = "删除保养计划")
    public R<Void> deletePlan(@PathVariable Long id) {
        planMapper.deleteById(id);
        return R.ok();
    }

    // ==================== 保养记录（物理删） ====================

    @GetMapping("/maintain-records")
    @RequiresPerm("equip:maintainRecord:list")
    public R<PageResult<EqMaintainRecord>> pageMaintains(@RequestParam(required = false) String recordCode,
                                                         @RequestParam(required = false) Long workshopId,
                                                         @RequestParam(required = false) Long equipmentId,
                                                         @RequestParam(required = false) Integer isFinish,
                                                         @RequestParam(defaultValue = "1") Integer pageNum,
                                                         @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<EqMaintainRecord> p = maintainMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<EqMaintainRecord>lambdaQuery()
                        .like(StringUtils.isNotBlank(recordCode), EqMaintainRecord::getRecordCode, recordCode)
                        .eq(workshopId != null, EqMaintainRecord::getWorkshopId, workshopId)
                        .eq(equipmentId != null, EqMaintainRecord::getEquipmentId, equipmentId)
                        .eq(isFinish != null, EqMaintainRecord::getIsFinish, isFinish)
                        .orderByDesc(EqMaintainRecord::getId));
        return R.ok(PageResult.of(p.getTotal(), p.getRecords()));
    }

    @PostMapping("/maintain-records")
    @RequiresPerm("equip:maintainRecord:list")
    @OperLog(title = "保存保养记录")
    public R<Long> saveMaintain(@RequestBody EqMaintainRecord record) {
        if (record.getEquipmentId() == null) {
            throw new com.xinao.mes.common.exception.BizException("请选择机台");
        }
        if (record.getId() == null) {
            maintainMapper.insert(record);
        } else {
            maintainMapper.updateById(record);
        }
        return R.ok(record.getId());
    }

    @DeleteMapping("/maintain-records/{id}")
    @RequiresPerm("equip:maintainRecord:list")
    @OperLog(title = "删除保养记录")
    public R<Void> deleteMaintain(@PathVariable Long id) {
        maintainMapper.deleteById(id);
        return R.ok();
    }

    // ==================== 点检记录（物理删） ====================

    @GetMapping("/check-records")
    @RequiresPerm("equip:checkRecord:list")
    public R<PageResult<EqCheckRecord>> pageChecks(@RequestParam(required = false) String recordCode,
                                                   @RequestParam(required = false) Long workshopId,
                                                   @RequestParam(required = false) String checkUser,
                                                   @RequestParam(defaultValue = "1") Integer pageNum,
                                                   @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<EqCheckRecord> p = checkMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<EqCheckRecord>lambdaQuery()
                        .like(StringUtils.isNotBlank(recordCode), EqCheckRecord::getRecordCode, recordCode)
                        .eq(workshopId != null, EqCheckRecord::getWorkshopId, workshopId)
                        .like(StringUtils.isNotBlank(checkUser), EqCheckRecord::getCheckUser, checkUser)
                        .orderByDesc(EqCheckRecord::getId));
        return R.ok(PageResult.of(p.getTotal(), p.getRecords()));
    }

    @PostMapping("/check-records")
    @RequiresPerm("equip:checkRecord:list")
    @OperLog(title = "保存点检记录")
    public R<Long> saveCheck(@RequestBody EqCheckRecord record) {
        if (StringUtils.isBlank(record.getRecordCode())) {
            throw new com.xinao.mes.common.exception.BizException("记录编码不能为空");
        }
        if (record.getId() == null) {
            checkMapper.insert(record);
        } else {
            checkMapper.updateById(record);
        }
        return R.ok(record.getId());
    }

    @DeleteMapping("/check-records/{id}")
    @RequiresPerm("equip:checkRecord:list")
    @OperLog(title = "删除点检记录")
    public R<Void> deleteCheck(@PathVariable Long id) {
        checkMapper.deleteById(id);
        return R.ok();
    }

    // ==================== 维保记录（物理删） ====================

    @GetMapping("/repair-records")
    @RequiresPerm("equip:repairRecord:list")
    public R<PageResult<EqRepairRecord>> pageRepairs(@RequestParam(required = false) String repairCode,
                                                     @RequestParam(required = false) Long workshopId,
                                                     @RequestParam(required = false) Long equipmentId,
                                                     @RequestParam(defaultValue = "1") Integer pageNum,
                                                     @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<EqRepairRecord> p = repairMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<EqRepairRecord>lambdaQuery()
                        .like(StringUtils.isNotBlank(repairCode), EqRepairRecord::getRepairCode, repairCode)
                        .eq(workshopId != null, EqRepairRecord::getWorkshopId, workshopId)
                        .eq(equipmentId != null, EqRepairRecord::getEquipmentId, equipmentId)
                        .orderByDesc(EqRepairRecord::getId));
        return R.ok(PageResult.of(p.getTotal(), p.getRecords()));
    }

    @PostMapping("/repair-records")
    @RequiresPerm("equip:repairRecord:list")
    @OperLog(title = "保存维保记录")
    public R<Long> saveRepair(@RequestBody EqRepairRecord record) {
        if (record.getEquipmentId() == null) {
            throw new com.xinao.mes.common.exception.BizException("请选择机台");
        }
        if (StringUtils.isBlank(record.getRepairCode())) {
            throw new com.xinao.mes.common.exception.BizException("维修编码不能为空");
        }
        if (record.getId() == null) {
            repairMapper.insert(record);
        } else {
            repairMapper.updateById(record);
        }
        return R.ok(record.getId());
    }

    @DeleteMapping("/repair-records/{id}")
    @RequiresPerm("equip:repairRecord:list")
    @OperLog(title = "删除维保记录")
    public R<Void> deleteRepair(@PathVariable Long id) {
        repairMapper.deleteById(id);
        return R.ok();
    }

    // ==================== 责任机台（物理删） ====================

    @GetMapping("/duty-machines")
    @RequiresPerm("equip:dutyMachine:list")
    public R<PageResult<EqDutyMachine>> pageDuties(@RequestParam(required = false) Long workshopId,
                                                   @RequestParam(required = false) String checkUser,
                                                   @RequestParam(defaultValue = "1") Integer pageNum,
                                                   @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<EqDutyMachine> p = dutyMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<EqDutyMachine>lambdaQuery()
                        .eq(workshopId != null, EqDutyMachine::getWorkshopId, workshopId)
                        .like(StringUtils.isNotBlank(checkUser), EqDutyMachine::getCheckUser, checkUser)
                        .orderByDesc(EqDutyMachine::getId));
        return R.ok(PageResult.of(p.getTotal(), p.getRecords()));
    }

    @PostMapping("/duty-machines")
    @RequiresPerm("equip:dutyMachine:list")
    @OperLog(title = "保存责任机台")
    public R<Long> saveDuty(@RequestBody EqDutyMachine duty) {
        if (duty.getEquipmentId() == null) {
            throw new com.xinao.mes.common.exception.BizException("请选择机台");
        }
        if (StringUtils.isBlank(duty.getCheckUser())) {
            throw new com.xinao.mes.common.exception.BizException("请填写点检人员");
        }
        if (duty.getId() == null) {
            dutyMapper.insert(duty);
        } else {
            dutyMapper.updateById(duty);
        }
        return R.ok(duty.getId());
    }

    @DeleteMapping("/duty-machines/{id}")
    @RequiresPerm("equip:dutyMachine:list")
    @OperLog(title = "删除责任机台")
    public R<Void> deleteDuty(@PathVariable Long id) {
        dutyMapper.deleteById(id);
        return R.ok();
    }
}
