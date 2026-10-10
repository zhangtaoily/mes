package com.xinao.mes.equip.controller;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xinao.mes.common.auth.AuthContext;
import com.xinao.mes.common.auth.RequiresPerm;
import com.xinao.mes.common.exception.BizException;
import com.xinao.mes.common.log.OperLog;
import com.xinao.mes.common.result.R;
import com.xinao.mes.engine.code.CodeEngine;
import com.xinao.mes.equip.domain.model.EqCheckItem;
import com.xinao.mes.equip.domain.model.EqCheckRecord;
import com.xinao.mes.equip.domain.model.EqDutyMachine;
import com.xinao.mes.equip.domain.model.EqFaultType;
import com.xinao.mes.equip.domain.model.EqRepairRecord;
import com.xinao.mes.equip.infrastructure.mapper.EqCheckItemMapper;
import com.xinao.mes.equip.infrastructure.mapper.EqCheckRecordMapper;
import com.xinao.mes.equip.infrastructure.mapper.EqDutyMachineMapper;
import com.xinao.mes.equip.infrastructure.mapper.EqFaultTypeMapper;
import com.xinao.mes.equip.infrastructure.mapper.EqRepairRecordMapper;
import com.xinao.mes.equip.vo.PdaMachineVO;
import com.xinao.mes.plan.domain.model.BaseEquipment;
import com.xinao.mes.plan.domain.model.BaseWorkshop;
import com.xinao.mes.plan.infrastructure.mapper.BaseEquipmentMapper;
import com.xinao.mes.plan.infrastructure.mapper.BaseWorkshopMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * PDA 设备点检维护 BFF（M4：点检执行 / 报修登记 / 维修完成）
 *
 * <p>与 PC 共表同源（eq_check_record / eq_repair_record 台账语义一致）；
 * PDA 差异补齐：记录编码由 CodeEngine 自动取号（DJ/WX，操作工免手输）、
 * 点检人/维修人/日期/车间缺省自动补齐；扫机台聚合车间名与点检责任人。
 */
@RestController
@RequestMapping("/api/pda/equip")
@RequiredArgsConstructor
public class PdaEquipController {

    private final BaseEquipmentMapper equipmentMapper;
    private final BaseWorkshopMapper workshopMapper;
    private final EqCheckItemMapper checkItemMapper;
    private final EqCheckRecordMapper checkRecordMapper;
    private final EqFaultTypeMapper faultTypeMapper;
    private final EqRepairRecordMapper repairRecordMapper;
    private final EqDutyMachineMapper dutyMachineMapper;
    private final CodeEngine codeEngine;

    /** 扫机台：机台号精确匹配，带出车间名与点检责任人（点检/报修页共用） */
    @GetMapping("/scan-machine")
    public R<PdaMachineVO> scanMachine(@RequestParam String keyword) {
        String no = StringUtils.isBlank(keyword) ? null : keyword.trim();
        if (no == null) {
            throw new BizException("请扫描或输入机台号");
        }
        BaseEquipment eq = equipmentMapper.selectOne(Wrappers.<BaseEquipment>lambdaQuery()
                .eq(BaseEquipment::getMachineNo, no)
                .last("LIMIT 1"));
        if (eq == null) {
            throw new BizException("未找到机台[" + no + "]");
        }
        PdaMachineVO vo = new PdaMachineVO();
        vo.setEquipment(eq);
        if (eq.getWorkshopId() != null) {
            BaseWorkshop ws = workshopMapper.selectById(eq.getWorkshopId());
            vo.setWorkshopName(ws == null ? null : ws.getWorkshopName());
        }
        List<EqDutyMachine> duties = dutyMachineMapper.selectList(Wrappers.<EqDutyMachine>lambdaQuery()
                .eq(EqDutyMachine::getEquipmentId, eq.getId()));
        if (!duties.isEmpty()) {
            vo.setDutyUsers(duties.stream()
                    .map(EqDutyMachine::getCheckUser)
                    .filter(StringUtils::isNotBlank)
                    .distinct()
                    .collect(Collectors.joining("、")));
        }
        return R.ok(vo);
    }

    /** 点检项目字典（启用，点检页勾选用；登录即可，操作工无设备字典权限场景） */
    @GetMapping("/check-items")
    public R<List<EqCheckItem>> checkItems() {
        return R.ok(checkItemMapper.selectList(Wrappers.<EqCheckItem>lambdaQuery()
                .eq(EqCheckItem::getStatus, 1)
                .orderByAsc(EqCheckItem::getId)));
    }

    /** 故障分类字典（启用，报修登记选分类用；登录即可） */
    @GetMapping("/fault-types")
    public R<List<EqFaultType>> faultTypes() {
        return R.ok(faultTypeMapper.selectList(Wrappers.<EqFaultType>lambdaQuery()
                .eq(EqFaultType::getStatus, 1)
                .orderByAsc(EqFaultType::getId)));
    }

    /** PDA 提交点检记录（编码自动取号 DJ；点检人/日期/车间缺省补齐） */
    @PostMapping("/checks")
    @RequiresPerm("equip:checkRecord:list")
    @OperLog(title = "PDA点检提交")
    public R<Long> saveCheck(@RequestBody EqCheckRecord record) {
        if (record == null || StringUtils.isBlank(record.getDescribe())) {
            throw new BizException("点检内容不能为空");
        }
        record.setId(null);
        record.setRecordCode(codeEngine.next("点检记录", record.getDescribe()));
        if (StringUtils.isBlank(record.getCheckUser())) {
            var user = AuthContext.get();
            record.setCheckUser(user == null ? "PDA" : user.getUserName());
        }
        if (record.getCheckDate() == null) {
            record.setCheckDate(LocalDate.now());
        }
        // 车间由前端按扫机台带出（与 PC 台账一致，选填）
        checkRecordMapper.insert(record);
        return R.ok(record.getId());
    }

    /** PDA 报修单查询（编码模糊/机台过滤；未解决在前，最多50条） */
    @GetMapping("/repairs")
    @RequiresPerm("equip:repairRecord:list")
    public R<List<EqRepairRecord>> repairs(@RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) Long equipmentId,
                                           @RequestParam(required = false) Boolean unsolved) {
        String kw = StringUtils.isBlank(keyword) ? null : keyword.trim();
        boolean onlyUnsolved = Boolean.TRUE.equals(unsolved);
        return R.ok(repairRecordMapper.selectList(Wrappers.<EqRepairRecord>lambdaQuery()
                .and(kw != null, w -> w.like(EqRepairRecord::getRepairCode, kw))
                .eq(equipmentId != null, EqRepairRecord::getEquipmentId, equipmentId)
                .and(onlyUnsolved, w -> w.isNull(EqRepairRecord::getSolveTime))
                .last("ORDER BY (solve_time IS NULL) DESC, id DESC LIMIT 50")));
    }

    /** PDA 报修登记（编码自动取号 WX；故障时间缺省现在、车间按机台带出，解决时间留空待维修） */
    @PostMapping("/repairs")
    @RequiresPerm("equip:repairRecord:list")
    @OperLog(title = "PDA报修登记")
    public R<Long> saveRepair(@RequestBody EqRepairRecord record) {
        if (record == null || record.getEquipmentId() == null) {
            throw new BizException("请先扫描机台");
        }
        if (StringUtils.isBlank(record.getDetail())) {
            throw new BizException("请填写故障详情");
        }
        record.setId(null);
        record.setSolveTime(null);
        record.setRepairCode(codeEngine.next("维修记录", record.getDetail()));
        if (record.getFaultTime() == null) {
            record.setFaultTime(LocalDateTime.now());
        }
        if (record.getWorkshopId() == null) {
            BaseEquipment eq = equipmentMapper.selectById(record.getEquipmentId());
            if (eq != null) {
                record.setWorkshopId(eq.getWorkshopId());
            }
        }
        repairRecordMapper.insert(record);
        return R.ok(record.getId());
    }

    /** PDA 维修完成（回填解决时间与维修人；已修复的拦截） */
    @PostMapping("/repairs/{id}/solve")
    @RequiresPerm("equip:repairRecord:list")
    @OperLog(title = "PDA维修完成")
    public R<Void> solveRepair(@PathVariable Long id) {
        EqRepairRecord record = repairRecordMapper.selectById(id);
        if (record == null) {
            throw new BizException("报修单不存在");
        }
        if (record.getSolveTime() != null) {
            throw new BizException("该报修单已修复，不可重复操作");
        }
        EqRepairRecord patch = new EqRepairRecord();
        patch.setId(id);
        patch.setSolveTime(LocalDateTime.now());
        if (StringUtils.isBlank(record.getRepairUser())) {
            var user = AuthContext.get();
            patch.setRepairUser(user == null ? "PDA" : user.getUserName());
        }
        repairRecordMapper.updateById(patch);
        return R.ok();
    }
}
