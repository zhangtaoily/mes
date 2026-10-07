package com.xinao.mes.plan.controller;

import com.xinao.mes.common.auth.RequiresPerm;
import com.xinao.mes.common.log.OperLog;
import com.xinao.mes.common.result.PageResult;
import com.xinao.mes.common.result.R;
import com.xinao.mes.plan.application.CraftLibraryService;
import com.xinao.mes.plan.application.ProdTaskAppService;
import com.xinao.mes.plan.domain.model.PlanCraftCraft;
import com.xinao.mes.plan.domain.model.ProdTask;
import com.xinao.mes.plan.dto.BatchIdsDTO;
import com.xinao.mes.plan.dto.TaskCraftApplyDTO;
import com.xinao.mes.plan.dto.TaskQueryDTO;
import com.xinao.mes.plan.dto.TaskSaveDTO;
import com.xinao.mes.plan.vo.CraftSchemeVO;
import com.xinao.mes.plan.vo.TaskDetailVO;
import com.xinao.mes.plan.vo.TaskRemainingVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 生产任务（手工新增 + 工艺配置引用 + 状态流转） */
@RestController
@RequestMapping("/api/plan/tasks")
@RequiredArgsConstructor
public class ProdTaskController {

    private final ProdTaskAppService taskAppService;
    private final CraftLibraryService craftLibraryService;

    /** 分页查询 */
    @GetMapping
    @RequiresPerm("prod:task:list")
    public R<PageResult<ProdTask>> page(TaskQueryDTO query) {
        return R.ok(taskAppService.page(query));
    }

    /** 详情 */
    @GetMapping("/{id}")
    @RequiresPerm("prod:task:list")
    public R<TaskDetailVO> detail(@PathVariable Long id) {
        return R.ok(taskAppService.detail(id));
    }

    /** 保存 */
    @PostMapping
    @RequiresPerm("prod:task:list")
    @OperLog(title = "保存生产任务")
    public R<Long> save(@Valid @RequestBody TaskSaveDTO dto) {
        return R.ok(taskAppService.save(dto));
    }

    /** 工艺配置：引用技术/车间工艺库带出（allProcesses=true 批量） */
    @PostMapping("/{id}/apply-craft")
    @RequiresPerm("prod:task:list")
    @OperLog(title = "任务工艺配置引用")
    public R<Integer> applyCraft(@PathVariable Long id, @Valid @RequestBody TaskCraftApplyDTO dto) {
        dto.setTaskId(id);
        return R.ok(taskAppService.applyCraft(dto));
    }

    /** 工艺配置选择面板：按工序(+机型)列可用方案 */
    @GetMapping("/craft-schemes")
    @RequiresPerm("prod:task:list")
    public R<List<CraftSchemeVO>> craftSchemes(@RequestParam Integer refSource,
                                               @RequestParam Long processId,
                                               @RequestParam(required = false) String machineModel) {
        return R.ok(craftLibraryService.schemes(refSource, processId, machineModel));
    }

    /** 排程-任务明细新增面板：生效任务 + 实时剩余可排 */
    @GetMapping("/available")
    @RequiresPerm({ "prod:task:list", "prod:schedule:list" })
    public R<List<TaskRemainingVO>> availableTasks() {
        return R.ok(taskAppService.availableTasks());
    }

    /** 批量提交 */
    @PostMapping("/submit")
    @RequiresPerm("prod:task:list")
    @OperLog(title = "提交生产任务")
    public R<Void> submit(@Valid @RequestBody BatchIdsDTO dto) {
        taskAppService.submit(dto.getIds());
        return R.ok();
    }

    /** 批量撤回（有排程占用拦截） */
    @PostMapping("/withdraw")
    @RequiresPerm("prod:task:list")
    @OperLog(title = "撤回生产任务")
    public R<Void> withdraw(@Valid @RequestBody BatchIdsDTO dto) {
        taskAppService.withdraw(dto.getIds());
        return R.ok();
    }

    /** 批量了批 */
    @PostMapping("/finish")
    @RequiresPerm("prod:task:list")
    @OperLog(title = "了批生产任务")
    public R<Void> finish(@Valid @RequestBody BatchIdsDTO dto) {
        taskAppService.finish(dto.getIds());
        return R.ok();
    }

    /** 批量关闭（终态） */
    @PostMapping("/close")
    @RequiresPerm("prod:task:list")
    @OperLog(title = "关闭生产任务")
    public R<Void> close(@Valid @RequestBody BatchIdsDTO dto) {
        taskAppService.close(dto.getIds());
        return R.ok();
    }

    /** 批量作废 */
    @PostMapping("/void")
    @RequiresPerm("prod:task:list")
    @OperLog(title = "作废生产任务")
    public R<Void> voidTask(@Valid @RequestBody BatchIdsDTO dto) {
        taskAppService.voidTask(dto.getIds());
        return R.ok();
    }

    /** 删除（仅编制中） */
    @DeleteMapping("/{id}")
    @RequiresPerm("prod:task:list")
    @OperLog(title = "删除生产任务")
    public R<Void> delete(@PathVariable Long id) {
        taskAppService.delete(id);
        return R.ok();
    }
}
