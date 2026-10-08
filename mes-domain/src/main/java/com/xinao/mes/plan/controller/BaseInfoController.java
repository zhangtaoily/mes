package com.xinao.mes.plan.controller;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinao.mes.common.auth.RequiresPerm;
import com.xinao.mes.common.log.OperLog;
import com.xinao.mes.common.result.PageResult;
import com.xinao.mes.common.result.R;
import com.xinao.mes.plan.domain.model.BaseBatchFlag;
import com.xinao.mes.plan.domain.model.BasePackMaterial;
import com.xinao.mes.plan.domain.model.BasePrintTemplate;
import com.xinao.mes.plan.domain.model.BaseTeam;
import com.xinao.mes.plan.domain.model.BaseWorkshop;
import com.xinao.mes.plan.domain.model.BaseEquipment;
import com.xinao.mes.plan.infrastructure.mapper.BaseBatchFlagMapper;
import com.xinao.mes.plan.infrastructure.mapper.BasePackMaterialMapper;
import com.xinao.mes.plan.infrastructure.mapper.BasePrintTemplateMapper;
import com.xinao.mes.plan.infrastructure.mapper.BaseTeamMapper;
import com.xinao.mes.plan.infrastructure.mapper.BaseWorkshopMapper;
import com.xinao.mes.plan.infrastructure.mapper.BaseEquipmentMapper;
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

import java.util.List;

/**
 * 基础信息管理：车间/班组/设备/标识牌/包装材质/打印模板
 * （单表维护，无状态机，直接走 mapper）
 */
@RestController
@RequestMapping("/api/base")
@RequiredArgsConstructor
public class BaseInfoController {

    private final BaseWorkshopMapper workshopMapper;
    private final BaseTeamMapper teamMapper;
    private final BaseEquipmentMapper equipmentMapper;
    private final BaseBatchFlagMapper batchFlagMapper;
    private final BasePackMaterialMapper packMaterialMapper;
    private final BasePrintTemplateMapper printTemplateMapper;

    // ==================== 车间与班组 ====================

    /** 车间分页 */
    @GetMapping("/workshops")
    @RequiresPerm("base:workshop:list")
    public R<PageResult<BaseWorkshop>> pageWorkshops(@RequestParam(required = false) String workshopName,
                                                     @RequestParam(required = false) Integer workshopType,
                                                     @RequestParam(required = false) Integer status,
                                                     @RequestParam(defaultValue = "1") Integer pageNum,
                                                     @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<BaseWorkshop> p = workshopMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<BaseWorkshop>lambdaQuery()
                        .like(StringUtils.isNotBlank(workshopName), BaseWorkshop::getWorkshopName, workshopName)
                        .eq(workshopType != null, BaseWorkshop::getWorkshopType, workshopType)
                        .eq(status != null, BaseWorkshop::getStatus, status)
                        .orderByAsc(BaseWorkshop::getId));
        return R.ok(PageResult.of(p.getTotal(), p.getRecords()));
    }

    /** 新增车间 */
    @PostMapping("/workshops")
    @RequiresPerm("base:workshop:list")
    @OperLog(title = "新增车间")
    public R<Void> saveWorkshop(@RequestBody BaseWorkshop workshop) {
        workshopMapper.insert(workshop);
        return R.ok();
    }

    /** 修改车间 */
    @PutMapping("/workshops/{id}")
    @RequiresPerm("base:workshop:list")
    @OperLog(title = "修改车间")
    public R<Void> updateWorkshop(@PathVariable Long id, @RequestBody BaseWorkshop workshop) {
        workshop.setId(id);
        workshopMapper.updateById(workshop);
        return R.ok();
    }

    /** 删除车间 */
    @DeleteMapping("/workshops/{id}")
    @RequiresPerm("base:workshop:list")
    @OperLog(title = "删除车间")
    public R<Void> deleteWorkshop(@PathVariable Long id) {
        workshopMapper.deleteById(id);
        return R.ok();
    }

    /** 班组分页 */
    @GetMapping("/teams")
    @RequiresPerm("base:workshop:list")
    public R<PageResult<BaseTeam>> pageTeams(@RequestParam(required = false) Long workshopId,
                                             @RequestParam(required = false) String teamName,
                                             @RequestParam(required = false) Integer status,
                                             @RequestParam(defaultValue = "1") Integer pageNum,
                                             @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<BaseTeam> p = teamMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<BaseTeam>lambdaQuery()
                        .eq(workshopId != null, BaseTeam::getWorkshopId, workshopId)
                        .like(StringUtils.isNotBlank(teamName), BaseTeam::getTeamName, teamName)
                        .eq(status != null, BaseTeam::getStatus, status)
                        .orderByAsc(BaseTeam::getId));
        return R.ok(PageResult.of(p.getTotal(), p.getRecords()));
    }

    /** 新增班组 */
    @PostMapping("/teams")
    @RequiresPerm("base:workshop:list")
    @OperLog(title = "新增班组")
    public R<Void> saveTeam(@RequestBody BaseTeam team) {
        teamMapper.insert(team);
        return R.ok();
    }

    /** 修改班组 */
    @PutMapping("/teams/{id}")
    @RequiresPerm("base:workshop:list")
    @OperLog(title = "修改班组")
    public R<Void> updateTeam(@PathVariable Long id, @RequestBody BaseTeam team) {
        team.setId(id);
        teamMapper.updateById(team);
        return R.ok();
    }

    /** 删除班组 */
    @DeleteMapping("/teams/{id}")
    @RequiresPerm("base:workshop:list")
    @OperLog(title = "删除班组")
    public R<Void> deleteTeam(@PathVariable Long id) {
        teamMapper.deleteById(id);
        return R.ok();
    }

    // ==================== 设备信息 ====================

    /** 设备分页 */
    @GetMapping("/machines")
    @RequiresPerm("base:machine:list")
    public R<PageResult<BaseEquipment>> pageMachines(@RequestParam(required = false) Long workshopId,
                                                     @RequestParam(required = false) String machineNo,
                                                     @RequestParam(required = false) String machineModel,
                                                     @RequestParam(required = false) Integer status,
                                                     @RequestParam(defaultValue = "1") Integer pageNum,
                                                     @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<BaseEquipment> p = equipmentMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<BaseEquipment>lambdaQuery()
                        .eq(workshopId != null, BaseEquipment::getWorkshopId, workshopId)
                        .like(StringUtils.isNotBlank(machineNo), BaseEquipment::getMachineNo, machineNo)
                        .like(StringUtils.isNotBlank(machineModel), BaseEquipment::getMachineModel, machineModel)
                        .eq(status != null, BaseEquipment::getStatus, status)
                        .orderByAsc(BaseEquipment::getId));
        return R.ok(PageResult.of(p.getTotal(), p.getRecords()));
    }

    /** 新增设备 */
    @PostMapping("/machines")
    @RequiresPerm("base:machine:list")
    @OperLog(title = "新增设备")
    public R<Void> saveMachine(@RequestBody BaseEquipment equipment) {
        equipmentMapper.insert(equipment);
        return R.ok();
    }

    /** 修改设备 */
    @PutMapping("/machines/{id}")
    @RequiresPerm("base:machine:list")
    @OperLog(title = "修改设备")
    public R<Void> updateMachine(@PathVariable Long id, @RequestBody BaseEquipment equipment) {
        equipment.setId(id);
        equipmentMapper.updateById(equipment);
        return R.ok();
    }

    /** 删除设备 */
    @DeleteMapping("/machines/{id}")
    @RequiresPerm("base:machine:list")
    @OperLog(title = "删除设备")
    public R<Void> deleteMachine(@PathVariable Long id) {
        equipmentMapper.deleteById(id);
        return R.ok();
    }

    // ==================== 标识牌 ====================

    /** 标识牌分页 */
    @GetMapping("/batch-flags")
    @RequiresPerm("base:tagBoard:list")
    public R<PageResult<BaseBatchFlag>> pageBatchFlags(@RequestParam(required = false) String flagName,
                                                       @RequestParam(required = false) Long processId,
                                                       @RequestParam(required = false) Integer status,
                                                       @RequestParam(defaultValue = "1") Integer pageNum,
                                                       @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<BaseBatchFlag> p = batchFlagMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<BaseBatchFlag>lambdaQuery()
                        .like(StringUtils.isNotBlank(flagName), BaseBatchFlag::getFlagName, flagName)
                        .eq(processId != null, BaseBatchFlag::getProcessId, processId)
                        .eq(status != null, BaseBatchFlag::getStatus, status)
                        .orderByAsc(BaseBatchFlag::getId));
        return R.ok(PageResult.of(p.getTotal(), p.getRecords()));
    }

    /** 新增标识牌 */
    @PostMapping("/batch-flags")
    @RequiresPerm("base:tagBoard:list")
    @OperLog(title = "新增标识牌")
    public R<Void> saveBatchFlag(@RequestBody BaseBatchFlag flag) {
        batchFlagMapper.insert(flag);
        return R.ok();
    }

    /** 修改标识牌 */
    @PutMapping("/batch-flags/{id}")
    @RequiresPerm("base:tagBoard:list")
    @OperLog(title = "修改标识牌")
    public R<Void> updateBatchFlag(@PathVariable Long id, @RequestBody BaseBatchFlag flag) {
        flag.setId(id);
        batchFlagMapper.updateById(flag);
        return R.ok();
    }

    /** 删除标识牌 */
    @DeleteMapping("/batch-flags/{id}")
    @RequiresPerm("base:tagBoard:list")
    @OperLog(title = "删除标识牌")
    public R<Void> deleteBatchFlag(@PathVariable Long id) {
        batchFlagMapper.deleteById(id);
        return R.ok();
    }

    // ==================== 包装材质 ====================

    /** 包装材质分页 */
    @GetMapping("/pack-materials")
    @RequiresPerm("base:packMaterial:list")
    public R<PageResult<BasePackMaterial>> pagePackMaterials(@RequestParam(required = false) String materialName,
                                                             @RequestParam(required = false) Integer status,
                                                             @RequestParam(defaultValue = "1") Integer pageNum,
                                                             @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<BasePackMaterial> p = packMaterialMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<BasePackMaterial>lambdaQuery()
                        .like(StringUtils.isNotBlank(materialName), BasePackMaterial::getMaterialName, materialName)
                        .eq(status != null, BasePackMaterial::getStatus, status)
                        .orderByAsc(BasePackMaterial::getId));
        return R.ok(PageResult.of(p.getTotal(), p.getRecords()));
    }

    /** 新增包装材质 */
    @PostMapping("/pack-materials")
    @RequiresPerm("base:packMaterial:list")
    @OperLog(title = "新增包装材质")
    public R<Void> savePackMaterial(@RequestBody BasePackMaterial material) {
        packMaterialMapper.insert(material);
        return R.ok();
    }

    /** 修改包装材质 */
    @PutMapping("/pack-materials/{id}")
    @RequiresPerm("base:packMaterial:list")
    @OperLog(title = "修改包装材质")
    public R<Void> updatePackMaterial(@PathVariable Long id, @RequestBody BasePackMaterial material) {
        material.setId(id);
        packMaterialMapper.updateById(material);
        return R.ok();
    }

    /** 删除包装材质 */
    @DeleteMapping("/pack-materials/{id}")
    @RequiresPerm("base:packMaterial:list")
    @OperLog(title = "删除包装材质")
    public R<Void> deletePackMaterial(@PathVariable Long id) {
        packMaterialMapper.deleteById(id);
        return R.ok();
    }

    // ==================== 打印模板 ====================

    /** 打印模板分页 */
    @GetMapping("/print-templates")
    @RequiresPerm("base:packMaterial:list")
    public R<PageResult<BasePrintTemplate>> pagePrintTemplates(@RequestParam(required = false) String templateName,
                                                               @RequestParam(required = false) Integer templateType,
                                                               @RequestParam(required = false) Integer status,
                                                               @RequestParam(defaultValue = "1") Integer pageNum,
                                                               @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<BasePrintTemplate> p = printTemplateMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<BasePrintTemplate>lambdaQuery()
                        .like(StringUtils.isNotBlank(templateName), BasePrintTemplate::getTemplateName, templateName)
                        .eq(templateType != null, BasePrintTemplate::getTemplateType, templateType)
                        .eq(status != null, BasePrintTemplate::getStatus, status)
                        .orderByAsc(BasePrintTemplate::getId));
        return R.ok(PageResult.of(p.getTotal(), p.getRecords()));
    }

    /** 新增打印模板 */
    @PostMapping("/print-templates")
    @RequiresPerm("base:packMaterial:list")
    @OperLog(title = "新增打印模板")
    public R<Void> savePrintTemplate(@RequestBody BasePrintTemplate template) {
        printTemplateMapper.insert(template);
        return R.ok();
    }

    /** 修改打印模板 */
    @PutMapping("/print-templates/{id}")
    @RequiresPerm("base:packMaterial:list")
    @OperLog(title = "修改打印模板")
    public R<Void> updatePrintTemplate(@PathVariable Long id, @RequestBody BasePrintTemplate template) {
        template.setId(id);
        printTemplateMapper.updateById(template);
        return R.ok();
    }

    /** 删除打印模板 */
    @DeleteMapping("/print-templates/{id}")
    @RequiresPerm("base:packMaterial:list")
    @OperLog(title = "删除打印模板")
    public R<Void> deletePrintTemplate(@PathVariable Long id) {
        printTemplateMapper.deleteById(id);
        return R.ok();
    }

    /** 车间全量下拉（基础信息页内复用） */
    @GetMapping("/workshop-options")
    public R<List<BaseWorkshop>> workshopOptions() {
        return R.ok(workshopMapper.selectList(Wrappers.<BaseWorkshop>lambdaQuery()
                .eq(BaseWorkshop::getStatus, 1).orderByAsc(BaseWorkshop::getId)));
    }
}
