package com.xinao.mes.plan.controller;

import com.xinao.mes.common.auth.RequiresPerm;
import com.xinao.mes.common.log.OperLog;
import com.xinao.mes.common.result.PageResult;
import com.xinao.mes.common.result.R;
import com.xinao.mes.plan.application.PlanCraftAppService;
import com.xinao.mes.plan.application.ScheduleAppService;
import com.xinao.mes.plan.domain.model.PlanCraft;
import com.xinao.mes.plan.dto.BatchIdsDTO;
import com.xinao.mes.plan.dto.CraftQueryDTO;
import com.xinao.mes.plan.dto.CraftSaveDTO;
import com.xinao.mes.plan.dto.ImportK3DTO;
import com.xinao.mes.plan.dto.PlanCraftApplyDTO;
import com.xinao.mes.plan.dto.RouteSaveDTO;
import com.xinao.mes.plan.vo.CraftDetailVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 计划工艺单（引入K3 → 编制 → 提交/撤回 → 生成/合并排程） */
@RestController
@RequestMapping("/api/plan/crafts")
@RequiredArgsConstructor
public class PlanCraftController {

    private final PlanCraftAppService craftAppService;
    private final ScheduleAppService scheduleAppService;

    /** 分页查询 */
    @GetMapping
    @RequiresPerm("craft:plan:list")
    public R<PageResult<PlanCraft>> page(CraftQueryDTO query) {
        return R.ok(craftAppService.page(query));
    }

    /** 详情（7Tab + 工序工艺项 + 可用按钮） */
    @GetMapping("/{id}")
    @RequiresPerm("craft:plan:list")
    public R<CraftDetailVO> detail(@PathVariable Long id) {
        return R.ok(craftAppService.detail(id));
    }

    /** 保存（新增/更新主信息与三个Tab） */
    @PostMapping
    @RequiresPerm("craft:plan:list")
    @OperLog(title = "保存计划工艺单")
    public R<Long> save(@Valid @RequestBody CraftSaveDTO dto) {
        return R.ok(craftAppService.save(dto));
    }

    /** 保存工序路线（整表替换，含编制工艺项） */
    @PutMapping("/{id}/routes")
    @RequiresPerm("craft:plan:list")
    @OperLog(title = "保存工序路线")
    public R<Void> saveRoute(@PathVariable Long id, @Valid @RequestBody RouteSaveDTO dto) {
        dto.setPlanCraftId(id);
        craftAppService.saveRoute(dto);
        return R.ok();
    }

    /** 编制工艺单：工艺项从技术/车间工艺库带出（routeId空=全部工序） */
    @PostMapping("/{id}/apply-craft")
    @RequiresPerm("craft:plan:list")
    @OperLog(title = "编制工艺单-工艺项引用")
    public R<Integer> applyCraft(@PathVariable Long id, @Valid @RequestBody PlanCraftApplyDTO dto) {
        dto.setPlanCraftId(id);
        return R.ok(craftAppService.applyCraftItems(dto));
    }

    /** 引入K3订单（单选带出 + 引入量回写） */
    @PostMapping("/import-k3")
    @RequiresPerm("craft:plan:list")
    @OperLog(title = "引入K3订单")
    public R<Long> importFromK3(@Valid @RequestBody ImportK3DTO dto) {
        return R.ok(craftAppService.importFromK3(dto));
    }

    /** 批量提交 */
    @PostMapping("/submit")
    @RequiresPerm("craft:plan:list")
    @OperLog(title = "提交计划工艺单")
    public R<Void> submit(@Valid @RequestBody BatchIdsDTO dto) {
        craftAppService.submit(dto.getIds());
        return R.ok();
    }

    /** 批量撤回 */
    @PostMapping("/withdraw")
    @RequiresPerm("craft:plan:list")
    @OperLog(title = "撤回计划工艺单")
    public R<Void> withdraw(@Valid @RequestBody BatchIdsDTO dto) {
        craftAppService.withdraw(dto.getIds());
        return R.ok();
    }

    /** 作废 */
    @PutMapping("/{id}/void")
    @RequiresPerm("craft:plan:list")
    @OperLog(title = "作废计划工艺单")
    public R<Void> voidCraft(@PathVariable Long id) {
        craftAppService.voidCraft(id);
        return R.ok();
    }

    /** 删除（仅编制中） */
    @DeleteMapping("/{id}")
    @RequiresPerm("craft:plan:list")
    @OperLog(title = "删除计划工艺单")
    public R<Void> delete(@PathVariable Long id) {
        craftAppService.delete(id);
        return R.ok();
    }

    /** 生成排程（按工序路线自动生成，返回工单号与排程id） */
    @PostMapping("/{id}/generate-schedule")
    @RequiresPerm("craft:plan:list")
    @OperLog(title = "生成排程")
    public R<Map<String, Object>> generateSchedule(@PathVariable Long id) {
        return R.ok(scheduleAppService.generateFromCraft(id));
    }

    /** 合并排程（勾选≥2张，按批号+车间分组） */
    @PostMapping("/merge-schedule")
    @RequiresPerm("craft:plan:list")
    @OperLog(title = "合并排程")
    public R<Map<String, Object>> mergeSchedule(@Valid @RequestBody BatchIdsDTO dto) {
        return R.ok(scheduleAppService.mergeFromCrafts(dto.getIds()));
    }
}
