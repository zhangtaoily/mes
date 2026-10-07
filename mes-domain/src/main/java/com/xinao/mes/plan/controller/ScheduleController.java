package com.xinao.mes.plan.controller;

import com.xinao.mes.common.auth.RequiresPerm;
import com.xinao.mes.common.log.OperLog;
import com.xinao.mes.common.result.PageResult;
import com.xinao.mes.common.result.R;
import com.xinao.mes.plan.application.ScheduleAppService;
import com.xinao.mes.plan.dto.BatchIdsDTO;
import com.xinao.mes.plan.dto.ScheduleQueryDTO;
import com.xinao.mes.plan.dto.ScheduleSaveDTO;
import com.xinao.mes.plan.dto.ScheduleTaskAddDTO;
import com.xinao.mes.plan.vo.ScheduleDetailVO;
import com.xinao.mes.plan.vo.ScheduleListVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 生产排程（任务明细 PI 带出 + 剩余可排校验 + 刷新） */
@RestController
@RequestMapping("/api/plan/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleAppService scheduleAppService;

    /** 分页查询 */
    @GetMapping
    @RequiresPerm("prod:schedule:list")
    public R<PageResult<ScheduleListVO>> page(ScheduleQueryDTO query) {
        return R.ok(scheduleAppService.page(query));
    }

    /** 详情 */
    @GetMapping("/{id}")
    @RequiresPerm("prod:schedule:list")
    public R<ScheduleDetailVO> detail(@PathVariable Long id) {
        return R.ok(scheduleAppService.detail(id));
    }

    /** 保存（手工新增/编辑） */
    @PostMapping
    @RequiresPerm("prod:schedule:list")
    @OperLog(title = "保存生产排程")
    public R<Long> save(@Valid @RequestBody ScheduleSaveDTO dto) {
        return R.ok(scheduleAppService.save(dto));
    }

    /** 批量删除（有未撤回制造卡不可删） */
    @PostMapping("/delete")
    @RequiresPerm("prod:schedule:list")
    @OperLog(title = "删除生产排程")
    public R<Void> delete(@Valid @RequestBody BatchIdsDTO dto) {
        scheduleAppService.delete(dto.getIds());
        return R.ok();
    }

    /** 任务明细新增（从生产任务带出 + 剩余可排实时校验） */
    @PostMapping("/task-details")
    @RequiresPerm("prod:schedule:list")
    @OperLog(title = "排程任务明细新增")
    public R<Void> addTaskDetail(@Valid @RequestBody ScheduleTaskAddDTO dto) {
        scheduleAppService.addTaskDetail(dto);
        return R.ok();
    }

    /** 任务明细移除（释放占用） */
    @DeleteMapping("/task-details/{detailId}")
    @RequiresPerm("prod:schedule:list")
    @OperLog(title = "排程任务明细移除")
    public R<Void> removeTaskDetail(@PathVariable Long detailId) {
        scheduleAppService.removeTaskDetail(detailId);
        return R.ok();
    }

    /** 刷新排程：重算已排产量快照 */
    @PostMapping("/refresh")
    @RequiresPerm("prod:schedule:list")
    @OperLog(title = "刷新排程")
    public R<Integer> refresh() {
        return R.ok(scheduleAppService.refresh());
    }
}
