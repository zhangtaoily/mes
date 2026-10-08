package com.xinao.mes.plan.controller;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinao.mes.common.auth.RequiresPerm;
import com.xinao.mes.common.exception.BizException;
import com.xinao.mes.common.log.OperLog;
import com.xinao.mes.common.result.PageResult;
import com.xinao.mes.common.result.R;
import com.xinao.mes.plan.domain.model.BaseCraft;
import com.xinao.mes.plan.domain.model.BaseCraftParam;
import com.xinao.mes.plan.domain.model.BaseCraftWorkshop;
import com.xinao.mes.plan.infrastructure.mapper.BaseCraftMapper;
import com.xinao.mes.plan.infrastructure.mapper.BaseCraftParamMapper;
import com.xinao.mes.plan.infrastructure.mapper.BaseCraftWorkshopMapper;
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
 * 工艺库维护：技术工艺库(base_craft) / 车间工艺库(base_craft_workshop)。
 * 两库共用 base_craft_param（craft_id 指向各自主表，技术库机型列恒为 NULL、车间库非空作区分标记）。
 */
@RestController
@RequestMapping("/api/craft")
@RequiredArgsConstructor
public class CraftLibraryController {

    private final BaseCraftMapper craftMapper;
    private final BaseCraftWorkshopMapper wsCraftMapper;
    private final BaseCraftParamMapper paramMapper;

    // ==================== 技术工艺库 ====================

    /** 分页查询 */
    @GetMapping("/tech-crafts")
    @RequiresPerm("craft:tech:list")
    public R<PageResult<BaseCraft>> pageTech(@RequestParam(required = false) String craftCode,
                                             @RequestParam(required = false) String craftName,
                                             @RequestParam(required = false) Integer enabled,
                                             @RequestParam(defaultValue = "1") Integer pageNum,
                                             @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<BaseCraft> p = craftMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<BaseCraft>lambdaQuery()
                        .like(StringUtils.isNotBlank(craftCode), BaseCraft::getCraftCode, craftCode)
                        .like(StringUtils.isNotBlank(craftName), BaseCraft::getCraftName, craftName)
                        .eq(enabled != null, BaseCraft::getEnabled, enabled)
                        .orderByDesc(BaseCraft::getId));
        return R.ok(PageResult.of(p.getTotal(), p.getRecords()));
    }

    /** 新增 */
    @PostMapping("/tech-crafts")
    @RequiresPerm("craft:tech:list")
    @OperLog(title = "新增技术工艺")
    public R<Long> saveTech(@RequestBody BaseCraft craft) {
        checkTech(craft);
        craftMapper.insert(craft);
        return R.ok(craft.getId());
    }

    /** 修改 */
    @PutMapping("/tech-crafts/{id}")
    @RequiresPerm("craft:tech:list")
    @OperLog(title = "修改技术工艺")
    public R<Void> updateTech(@PathVariable Long id, @RequestBody BaseCraft craft) {
        checkTech(craft);
        craft.setId(id);
        craftMapper.updateById(craft);
        return R.ok();
    }

    /** 删除（逻辑删） */
    @DeleteMapping("/tech-crafts/{id}")
    @RequiresPerm("craft:tech:list")
    @OperLog(title = "删除技术工艺")
    public R<Void> deleteTech(@PathVariable Long id) {
        craftMapper.deleteById(id);
        return R.ok();
    }

    /** 工艺参数列表（技术库：机型为空） */
    @GetMapping("/tech-crafts/{id}/params")
    @RequiresPerm("craft:tech:list")
    public R<List<BaseCraftParam>> techParams(@PathVariable Long id) {
        return R.ok(paramList(id, false));
    }

    /** 追加工艺参数（技术库机型恒空） */
    @PostMapping("/tech-crafts/{id}/params")
    @RequiresPerm("craft:tech:list")
    @OperLog(title = "新增技术工艺参数")
    public R<Void> addTechParam(@PathVariable Long id, @RequestBody BaseCraftParam param) {
        param.setId(null);
        param.setCraftId(id);
        param.setMachineModel(null);
        paramMapper.insert(param);
        return R.ok();
    }

    /** 删除工艺参数（物理删，参数表无逻辑删除列） */
    @DeleteMapping("/tech-crafts/{id}/params/{paramId}")
    @RequiresPerm("craft:tech:list")
    @OperLog(title = "删除技术工艺参数")
    public R<Void> deleteTechParam(@PathVariable Long id, @PathVariable Long paramId) {
        paramMapper.deleteById(paramId);
        return R.ok();
    }

    // ==================== 车间工艺库 ====================

    /** 分页查询 */
    @GetMapping("/ws-crafts")
    @RequiresPerm("craft:ws:list")
    public R<PageResult<BaseCraftWorkshop>> pageWs(@RequestParam(required = false) String craftName,
                                                   @RequestParam(required = false) Long workshopId,
                                                   @RequestParam(required = false) Integer enabled,
                                                   @RequestParam(defaultValue = "1") Integer pageNum,
                                                   @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<BaseCraftWorkshop> p = wsCraftMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<BaseCraftWorkshop>lambdaQuery()
                        .like(StringUtils.isNotBlank(craftName), BaseCraftWorkshop::getCraftName, craftName)
                        .eq(workshopId != null, BaseCraftWorkshop::getWorkshopId, workshopId)
                        .eq(enabled != null, BaseCraftWorkshop::getEnabled, enabled)
                        .orderByDesc(BaseCraftWorkshop::getId));
        return R.ok(PageResult.of(p.getTotal(), p.getRecords()));
    }

    /** 新增 */
    @PostMapping("/ws-crafts")
    @RequiresPerm("craft:ws:list")
    @OperLog(title = "新增车间工艺")
    public R<Long> saveWs(@RequestBody BaseCraftWorkshop craft) {
        checkWs(craft);
        wsCraftMapper.insert(craft);
        return R.ok(craft.getId());
    }

    /** 修改 */
    @PutMapping("/ws-crafts/{id}")
    @RequiresPerm("craft:ws:list")
    @OperLog(title = "修改车间工艺")
    public R<Void> updateWs(@PathVariable Long id, @RequestBody BaseCraftWorkshop craft) {
        checkWs(craft);
        craft.setId(id);
        wsCraftMapper.updateById(craft);
        return R.ok();
    }

    /** 删除（逻辑删） */
    @DeleteMapping("/ws-crafts/{id}")
    @RequiresPerm("craft:ws:list")
    @OperLog(title = "删除车间工艺")
    public R<Void> deleteWs(@PathVariable Long id) {
        wsCraftMapper.deleteById(id);
        return R.ok();
    }

    /** 工艺参数列表（车间库：机型非空） */
    @GetMapping("/ws-crafts/{id}/params")
    @RequiresPerm("craft:ws:list")
    public R<List<BaseCraftParam>> wsParams(@PathVariable Long id) {
        return R.ok(paramList(id, true));
    }

    /** 追加工艺参数（车间库按 工序+机型 带出，机型必填） */
    @PostMapping("/ws-crafts/{id}/params")
    @RequiresPerm("craft:ws:list")
    @OperLog(title = "新增车间工艺参数")
    public R<Void> addWsParam(@PathVariable Long id, @RequestBody BaseCraftParam param) {
        if (StringUtils.isBlank(param.getMachineModel())) {
            throw new BizException("车间工艺参数必须指定机型");
        }
        param.setId(null);
        param.setCraftId(id);
        paramMapper.insert(param);
        return R.ok();
    }

    /** 删除工艺参数 */
    @DeleteMapping("/ws-crafts/{id}/params/{paramId}")
    @RequiresPerm("craft:ws:list")
    @OperLog(title = "删除车间工艺参数")
    public R<Void> deleteWsParam(@PathVariable Long id, @PathVariable Long paramId) {
        paramMapper.deleteById(paramId);
        return R.ok();
    }

    // ==================== 内部方法 ====================

    private void checkTech(BaseCraft craft) {
        if (craft == null || StringUtils.isBlank(craft.getCraftCode())) {
            throw new BizException("工艺编号不能为空");
        }
        if (StringUtils.isBlank(craft.getCraftName())) {
            throw new BizException("工艺名称不能为空");
        }
    }

    private void checkWs(BaseCraftWorkshop craft) {
        if (craft == null || StringUtils.isBlank(craft.getCraftName())) {
            throw new BizException("车间工艺名称不能为空");
        }
        if (craft.getWorkshopId() == null) {
            throw new BizException("请选择所属车间");
        }
    }

    /** 参数查询：byMachine=true 车间库（机型非空），false 技术库（机型为空） */
    private List<BaseCraftParam> paramList(Long craftId, boolean byMachine) {
        return paramMapper.selectList(Wrappers.<BaseCraftParam>lambdaQuery()
                .eq(BaseCraftParam::getCraftId, craftId)
                .isNotNull(byMachine, BaseCraftParam::getMachineModel)
                .isNull(!byMachine, BaseCraftParam::getMachineModel)
                .orderByAsc(BaseCraftParam::getSortNo)
                .orderByAsc(BaseCraftParam::getId));
    }
}
